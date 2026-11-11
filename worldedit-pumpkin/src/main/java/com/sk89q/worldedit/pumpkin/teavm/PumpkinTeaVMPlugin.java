/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.pumpkin.teavm;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PumpkinTeaVMPlugin implements TeaVMPlugin, ClassHolderTransformer {
    private static final String TARGET = "com.sk89q.worldedit.pumpkin.Redirects";
    private static final char MARKER = (char) 0xFFFF;
    private static final MethodReference DECODE = MethodReference.parse(TARGET + ".decodeString(Ljava/lang/String;)Ljava/lang/String;");
    private final Map<String, MethodReference> redirects = new HashMap<>();

    public PumpkinTeaVMPlugin() {
        instance("java.io.File", "toPath", "()Ljava/nio/file/Path;", "fileToPath");
        instance("java.util.Calendar", "toInstant", "()Ljava/time/Instant;", "calendarToInstant");
        instance("java.lang.Runtime", "availableProcessors", "()I", "availableProcessors");
        statics("java.util.TimeZone", "getTimeZone", "(Ljava/time/ZoneId;)Ljava/util/TimeZone;", "timeZoneOf");
        statics("java.util.Collections", "unmodifiableSortedSet", "(Ljava/util/SortedSet;)Ljava/util/SortedSet;", "unmodifiableSortedSet");
        statics("java.util.Locale", "forLanguageTag", "(Ljava/lang/String;)Ljava/util/Locale;", "localeForTag");
        statics("java.util.Arrays", "spliterator", "([Ljava/lang/Object;)Ljava/util/Spliterator;", "arraySpliterator");
        statics("com.sk89q.worldedit.internal.util.DeprecationUtil", "checkDelegatingOverride", "(Ljava/lang/Class;)V", "ignore");
        instance("java.util.Spliterator$OfInt", "trySplit", "()Ljava/util/Spliterator$OfInt;", "splitInt");
        statics("java.nio.file.spi.FileSystemProvider", "installedProviders", "()Ljava/util/List;", "installedProviders");
        statics("java.nio.file.Paths", "get", "(Ljava/net/URI;)Ljava/nio/file/Path;", "pathOf");
        instance("java.util.Properties", "store", "(Ljava/io/OutputStream;Ljava/lang/String;)V", "storeProperties");
    }

    private void instance(String owner, String name, String descriptor, String target) {
        String redirected = "(L" + owner.replace('.', '/') + ";" + descriptor.substring(1);
        redirects.put(owner + "." + name + descriptor, MethodReference.parse(TARGET + "." + target + redirected));
    }

    private void statics(String owner, String name, String descriptor, String target) {
        redirects.put(owner + "." + name + descriptor, MethodReference.parse(TARGET + "." + target + descriptor));
    }

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(TARGET)) {
            return;
        }

        for (MethodHolder method : cls.getMethods()) {
            method.getModifiers().remove(ElementModifier.SYNCHRONIZED);

            Program program = method.getProgram();
            if (program == null) {
                continue;
            }

            for (int i = 0; i < program.basicBlockCount(); i++) {
                BasicBlock block = program.basicBlockAt(i);
                Instruction instruction = block.getFirstInstruction();

                while (instruction != null) {
                    Instruction next = instruction.getNext();

                    if (instruction instanceof InvokeInstruction invoke) {
                        if (isNotification(invoke)) {
                            instruction.delete();
                        } else {
                            redirect(invoke);
                        }
                    } else if (instruction instanceof StringConstantInstruction constant) {
                        encode(program, constant);
                    } else if (instruction instanceof MonitorEnterInstruction || instruction instanceof MonitorExitInstruction) {
                        instruction.delete();
                    }

                    instruction = next;
                }
            }
        }
    }

    private static boolean isNotification(InvokeInstruction invoke) {
        String method = invoke.getMethod().toString();
        return invoke.getInstance() != null && (method.endsWith(".notify()V") || method.endsWith(".notifyAll()V"));
    }

    private void encode(Program program, StringConstantInstruction constant) {
        String value = constant.getConstant();
        if (value == null || !needsEncoding(value)) {
            return;
        }

        StringBuilder encoded = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c >= 0x7F && c <= 0x9F) {
                encoded.append(MARKER).append((char) (c + 0x100));
            } else if (c == MARKER) {
                encoded.append(MARKER).append(MARKER);
            } else {
                encoded.append(c);
            }
        }

        Variable receiver = constant.getReceiver();
        Variable temporary = program.createVariable();
        constant.setConstant(encoded.toString());
        constant.setReceiver(temporary);

        InvokeInstruction decode = new InvokeInstruction();
        decode.setType(InvocationType.SPECIAL);
        decode.setMethod(DECODE);
        decode.setArguments(temporary);
        decode.setReceiver(receiver);
        decode.setLocation(constant.getLocation());
        constant.insertNext(decode);
    }

    private static boolean needsEncoding(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= 0x7F && c <= 0x9F) {
                return true;
            }
        }

        return false;
    }

    private void redirect(InvokeInstruction invoke) {
        MethodReference target = redirects.get(invoke.getMethod().toString());
        if (target == null) {
            return;
        }

        Variable instance = invoke.getInstance();
        if (instance != null) {
            List<? extends Variable> arguments = invoke.getArguments();
            Variable[] shifted = new Variable[arguments.size() + 1];
            shifted[0] = instance;

            for (int i = 0; i < arguments.size(); i++) {
                shifted[i + 1] = arguments.get(i);
            }

            invoke.setArguments(shifted);
            invoke.setInstance(null);
        }

        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(target);
    }
}

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

import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.internal.command.exception.ExceptionMatch;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

public final class PumpkinReflectionPolicy extends SimpleReflectionPolicy {
    private static final String COMMAND_PACKAGE = "com.sk89q.worldedit.command.";

    @Override
    protected void setup() {
        allClasses().reflectableMethods(withAnnotation(Subscribe.class));
        allClasses().reflectableMethods(withAnnotation(ExceptionMatch.class));
        allClasses().reflectableFields(named("REGISTRY"));

        selectClasses(PumpkinReflectionPolicy::isCommandContainer).reflectableMethods(method -> !method.isConstructor());
        selectClasses(PumpkinReflectionPolicy::isRegistrationHelper).reflectableMethods(method -> !method.isConstructor());

        selectClasses(extending(RegionSelector.class)).reflectableMethods(method -> method.isConstructor() && method.parameters().isEmpty());
        selectClasses(extending(LocalConfiguration.class)).reflectableFields(field -> true);
    }

    private static boolean isCommandContainer(IntrospectClass<?> cls) {
        String name = cls.name();
        return name.startsWith(COMMAND_PACKAGE) && name.endsWith("Commands") && name.indexOf('.', COMMAND_PACKAGE.length()) < 0;
    }

    private static boolean isRegistrationHelper(IntrospectClass<?> cls) {
        String name = cls.name();
        return name.startsWith(COMMAND_PACKAGE) && name.contains("Registration$");
    }
}

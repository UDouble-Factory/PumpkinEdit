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

package com.sk89q.worldedit.pumpkin;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import pumpkin.logging.Level;
import pumpkin.logging.Logging;

public final class PumpkinLogger implements Logger {
    private static final String PREFIX = "[WorldEdit] ";

    private static String format(String message, Object... parameters) {
        if (message == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder(message.length() + 16);
        int index = 0;
        int start = 0;

        while (index < parameters.length) {
            int marker = message.indexOf("{}", start);
            if (marker < 0) {
                break;
            }

            builder.append(message, start, marker).append(parameters[index]);
            start = marker + 2;
            index++;
        }

        builder.append(message, start, message.length());
        return builder.toString();
    }

    private static void write(Level level, String message, Throwable throwable) {
        if (throwable == null) {
            Logging.log(level, PREFIX + message);
            return;
        }

        StringBuilder builder = new StringBuilder(PREFIX).append(message);
        Throwable current = throwable;
        int depth = 0;

        while (current != null && depth < 8) {
            builder.append(depth == 0 ? ": " : " <- ").append(current.getClass().getName());
            if (current.getMessage() != null) {
                builder.append(": ").append(current.getMessage());
            }
            current = current.getCause();
            depth++;
        }

        Logging.log(level, builder.toString());

        if (level != Level.ERROR) {
            return;
        }

        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        StackTraceElement[] frames = root.getStackTrace();
        for (int i = 0; i < frames.length && i < 30; i++) {
            Logging.log(level, PREFIX + "    at " + frames[i]);
        }
    }

    private static void write(Level level, String message, Object[] parameters) {
        Throwable throwable = null;
        if (parameters.length > 0 && parameters[parameters.length - 1] instanceof Throwable last) {
            throwable = last;
        }

        write(level, format(message, parameters), throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(String message) {
        write(Level.DEBUG, message, (Throwable) null);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        write(Level.DEBUG, message, throwable);
    }

    @Override
    public void debug(String message, Object p0) {
        write(Level.DEBUG, message, new Object[] { p0 });
    }

    @Override
    public void debug(String message, Object p0, Object p1) {
        write(Level.DEBUG, message, new Object[] { p0, p1 });
    }

    @Override
    public void debug(String message, Object... parameters) {
        write(Level.DEBUG, message, parameters);
    }

    @Override
    public void info(String message) {
        write(Level.INFO, message, (Throwable) null);
    }

    @Override
    public void info(String message, Throwable throwable) {
        write(Level.INFO, message, throwable);
    }

    @Override
    public void info(String message, Object p0) {
        write(Level.INFO, message, new Object[] { p0 });
    }

    @Override
    public void info(String message, Object p0, Object p1) {
        write(Level.INFO, message, new Object[] { p0, p1 });
    }

    @Override
    public void info(String message, Object p0, Object p1, Object p2) {
        write(Level.INFO, message, new Object[] { p0, p1, p2 });
    }

    @Override
    public void info(Supplier<?> message, Throwable throwable) {
        write(Level.INFO, String.valueOf(message.get()), throwable);
    }

    @Override
    public void warn(String message) {
        write(Level.WARN, message, (Throwable) null);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        write(Level.WARN, message, throwable);
    }

    @Override
    public void warn(String message, Object p0) {
        write(Level.WARN, message, new Object[] { p0 });
    }

    @Override
    public void warn(String message, Object p0, Object p1) {
        write(Level.WARN, message, new Object[] { p0, p1 });
    }

    @Override
    public void error(String message) {
        write(Level.ERROR, message, (Throwable) null);
    }

    @Override
    public void error(Object message) {
        write(Level.ERROR, String.valueOf(message), (Throwable) null);
    }

    @Override
    public void error(String message, Throwable throwable) {
        write(Level.ERROR, message, throwable);
    }

    @Override
    public void error(String message, Object p0) {
        write(Level.ERROR, message, new Object[] { p0 });
    }

    @Override
    public void error(String message, Object p0, Object p1) {
        write(Level.ERROR, message, new Object[] { p0, p1 });
    }
}

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

package org.apache.logging.log4j;

import org.apache.logging.log4j.util.Supplier;

public interface Logger {
    boolean isDebugEnabled();

    void debug(String message);

    void debug(String message, Throwable throwable);

    void debug(String message, Object p0);

    void debug(String message, Object p0, Object p1);

    void debug(String message, Object... parameters);

    void info(String message);

    void info(String message, Throwable throwable);

    void info(String message, Object p0);

    void info(String message, Object p0, Object p1);

    void info(String message, Object p0, Object p1, Object p2);

    void info(Supplier<?> message, Throwable throwable);

    void warn(String message);

    void warn(String message, Throwable throwable);

    void warn(String message, Object p0);

    void warn(String message, Object p0, Object p1);

    void error(String message);

    void error(Object message);

    void error(String message, Throwable throwable);

    void error(String message, Object p0);

    void error(String message, Object p0, Object p1);
}

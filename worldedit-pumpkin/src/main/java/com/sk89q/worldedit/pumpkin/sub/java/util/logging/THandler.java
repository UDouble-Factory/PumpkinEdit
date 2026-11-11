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

package com.sk89q.worldedit.pumpkin.sub.java.util.logging;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public abstract class THandler {
    private Formatter formatter;
    private Level level = Level.ALL;

    protected THandler() {
    }

    public abstract void publish(LogRecord record);

    public abstract void flush();

    public abstract void close();

    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
    }

    public Formatter getFormatter() {
        return formatter;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Level getLevel() {
        return level;
    }

    public boolean isLoggable(LogRecord record) {
        return record != null && record.getLevel().intValue() >= level.intValue() && level.intValue() != Level.OFF.intValue();
    }
}

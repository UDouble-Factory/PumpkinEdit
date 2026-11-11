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

import pumpkin.logging.Logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class TLogger {
    public static final String GLOBAL_LOGGER_NAME = "global";
    private static final Map<String, TLogger> LOGGERS = new HashMap<>();

    private final String name;
    private final List<THandler> handlers = new ArrayList<>();
    private Level level = Level.INFO;
    private boolean useParentHandlers = true;

    protected TLogger(String name, String resourceBundleName) {
        this.name = name;
    }

    public static TLogger getLogger(String name) {
        return LOGGERS.computeIfAbsent(name, key -> new TLogger(key, null));
    }

    public static TLogger getGlobal() {
        return getLogger(GLOBAL_LOGGER_NAME);
    }

    public static TLogger getAnonymousLogger() {
        return new TLogger("", null);
    }

    public String getName() {
        return name;
    }

    public TLogger getParent() {
        return null;
    }

    public void setLevel(Level level) {
        this.level = level == null ? Level.INFO : level;
    }

    public Level getLevel() {
        return level;
    }

    public void setUseParentHandlers(boolean useParentHandlers) {
        this.useParentHandlers = useParentHandlers;
    }

    public boolean getUseParentHandlers() {
        return useParentHandlers;
    }

    public void addHandler(THandler handler) {
        handlers.add(handler);
    }

    public void removeHandler(THandler handler) {
        handlers.remove(handler);
    }

    public THandler[] getHandlers() {
        return handlers.toArray(new THandler[0]);
    }

    public boolean isLoggable(Level target) {
        return level.intValue() != Level.OFF.intValue() && target.intValue() >= level.intValue();
    }

    public void log(LogRecord record) {
        if (!isLoggable(record.getLevel())) {
            return;
        }

        if (handlers.isEmpty()) {
            Logging.log(convert(record.getLevel()), String.valueOf(record.getMessage()));
            return;
        }

        for (THandler handler : handlers) {
            handler.publish(record);
        }
    }

    private static pumpkin.logging.Level convert(Level level) {
        int value = level.intValue();
        if (value >= Level.SEVERE.intValue()) {
            return pumpkin.logging.Level.ERROR;
        }
        if (value >= Level.WARNING.intValue()) {
            return pumpkin.logging.Level.WARN;
        }
        if (value >= Level.INFO.intValue()) {
            return pumpkin.logging.Level.INFO;
        }
        return pumpkin.logging.Level.DEBUG;
    }

    public void log(Level target, String message) {
        if (!isLoggable(target)) {
            return;
        }

        LogRecord record = new LogRecord(target, message);
        record.setLoggerName(name);
        log(record);
    }

    public void log(Level target, String message, Object parameter) {
        log(target, message, new Object[] { parameter });
    }

    public void log(Level target, String message, Object[] parameters) {
        if (!isLoggable(target)) {
            return;
        }

        LogRecord record = new LogRecord(target, message);
        record.setLoggerName(name);
        record.setParameters(parameters);
        log(record);
    }

    public void log(Level target, String message, Throwable thrown) {
        if (!isLoggable(target)) {
            return;
        }

        LogRecord record = new LogRecord(target, message);
        record.setLoggerName(name);
        record.setThrown(thrown);
        log(record);
    }

    public void severe(String message) {
        log(Level.SEVERE, message);
    }

    public void warning(String message) {
        log(Level.WARNING, message);
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void config(String message) {
        log(Level.CONFIG, message);
    }

    public void fine(String message) {
        log(Level.FINE, message);
    }

    public void finer(String message) {
        log(Level.FINER, message);
    }

    public void finest(String message) {
        log(Level.FINEST, message);
    }
}

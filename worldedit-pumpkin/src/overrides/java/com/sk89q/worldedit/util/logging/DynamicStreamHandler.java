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

package com.sk89q.worldedit.util.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import javax.annotation.Nullable;

public class DynamicStreamHandler extends StreamHandler {

    private @Nullable StreamHandler handler;
    private @Nullable Formatter formatter;

    public @Nullable StreamHandler getHandler() {
        return handler;
    }

    public void setHandler(@Nullable StreamHandler handler) {
        if (this.handler != null) {
            this.handler.close();
        }

        this.handler = handler;

        if (handler != null && formatter != null) {
            handler.setFormatter(formatter);
        }
    }

    @Override
    public void publish(LogRecord record) {
        if (handler != null) {
            handler.publish(record);
        }
    }

    @Override
    public void close() {
        if (handler != null) {
            handler.close();
        }
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return handler != null && handler.isLoggable(record);
    }

    @Override
    public void flush() {
        if (handler != null) {
            handler.flush();
        }
    }

    @Override
    public void setFormatter(Formatter newFormatter) {
        this.formatter = newFormatter;

        if (handler != null) {
            handler.setFormatter(newFormatter);
        }
    }

    @Override
    public Formatter getFormatter() {
        return formatter;
    }
}

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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class TStreamHandler extends THandler {
    private OutputStream output;

    public TStreamHandler() {
    }

    public TStreamHandler(OutputStream output, Formatter formatter) {
        this.output = output;
        setFormatter(formatter);
    }

    protected void setOutputStream(OutputStream output) {
        close();
        this.output = output;
    }

    public void setEncoding(String encoding) {
    }

    @Override
    public void publish(LogRecord record) {
        Formatter formatter = getFormatter();
        if (output == null || formatter == null || !isLoggable(record)) {
            return;
        }

        try {
            output.write(formatter.format(record).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            output = null;
        }
    }

    @Override
    public void flush() {
        if (output == null) {
            return;
        }

        try {
            output.flush();
        } catch (IOException ignored) {
            output = null;
        }
    }

    @Override
    public void close() {
        if (output == null) {
            return;
        }

        try {
            output.close();
        } catch (IOException ignored) {
            output = null;
        }

        output = null;
    }
}

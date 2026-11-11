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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormat extends Formatter {
    public static final String DEFAULT_FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s]: %5$s%6$s%n";

    public LogFormat() {
    }

    public LogFormat(String format) {
    }

    private static void pad(StringBuilder builder, int value) {
        if (value < 10) {
            builder.append('0');
        }

        builder.append(value);
    }

    @Override
    public String format(LogRecord record) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(record.getMillis()), ZoneOffset.UTC);
        StringBuilder builder = new StringBuilder(64).append('[').append(time.getYear()).append('-');

        pad(builder, time.getMonthValue());
        builder.append('-');
        pad(builder, time.getDayOfMonth());
        builder.append(' ');
        pad(builder, time.getHour());
        builder.append(':');
        pad(builder, time.getMinute());
        builder.append(':');
        pad(builder, time.getSecond());
        builder.append(' ').append(record.getLevel().getName()).append("]: ").append(record.getMessage());

        if (record.getThrown() != null) {
            builder.append(' ').append(record.getThrown());
        }

        return builder.append('\n').toString();
    }
}

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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TimeZone;
import java.util.TreeMap;

public final class Redirects {
    private static final char MARKER = (char) 0xFFFF;
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private Redirects() {
    }

    public static Path fileToPath(File file) {
        return Paths.get(file.getPath());
    }

    public static Instant calendarToInstant(Calendar calendar) {
        return Instant.ofEpochMilli(calendar.getTimeInMillis());
    }

    public static int availableProcessors(Runtime runtime) {
        return 1;
    }

    public static TimeZone timeZoneOf(ZoneId zone) {
        return TimeZone.getTimeZone(zone.getId());
    }

    public static <T> SortedSet<T> unmodifiableSortedSet(SortedSet<T> set) {
        return set;
    }

    public static Locale localeForTag(String tag) {
        String[] parts = tag.replace('_', '-').split("-");
        if (parts.length >= 2) {
            return Locale.of(parts[0], parts[1].toUpperCase(Locale.ROOT));
        }

        return Locale.of(parts[0]);
    }

    public static <T> Spliterator<T> arraySpliterator(Object[] array) {
        return Spliterators.spliterator(array, Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    public static Spliterator.OfInt splitInt(Spliterator.OfInt spliterator) {
        Spliterator<Integer> base = spliterator;
        return (Spliterator.OfInt) base.trySplit();
    }

    public static void ignore(Class<?> type) {
    }

    public static Path pathOf(URI uri) {
        return Paths.get(uri.getPath());
    }

    public static List<FileSystemProvider> installedProviders() {
        return List.of(FileSystems.getDefault().provider());
    }

    public static void storeProperties(Properties properties, OutputStream output, String comments) throws IOException {
        Map<String, String> sorted = new TreeMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            sorted.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        StringBuilder text = new StringBuilder();
        if (comments != null) {
            text.append('#').append(comments.replace("\n", "\n#")).append('\n');
        }

        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            appendProperty(text, entry.getKey(), true);
            text.append('=');
            appendProperty(text, entry.getValue(), false);
            text.append('\n');
        }

        output.write(text.toString().getBytes(StandardCharsets.ISO_8859_1));
        output.flush();
    }

    private static void appendProperty(StringBuilder text, String value, boolean key) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case ' ' -> text.append(key || i == 0 ? "\\ " : " ");
                case '\t' -> text.append("\\t");
                case '\n' -> text.append("\\n");
                case '\r' -> text.append("\\r");
                case '\f' -> text.append("\\f");
                case '\\' -> text.append("\\\\");
                case '=', ':', '#', '!' -> text.append(key || i == 0 ? "\\" : "").append(c);
                default -> {
                    if (c < 0x20 || c > 0x7E) {
                        text.append("\\u").append(HEX[c >> 12 & 0xF]).append(HEX[c >> 8 & 0xF]).append(HEX[c >> 4 & 0xF]).append(HEX[c & 0xF]);
                    } else {
                        text.append(c);
                    }
                }
            }
        }
    }

    public static String decodeString(String encoded) {
        StringBuilder builder = new StringBuilder(encoded.length());

        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);

            if (c == MARKER && i + 1 < encoded.length()) {
                char next = encoded.charAt(++i);
                builder.append(next == MARKER ? MARKER : (char) (next - 0x100));
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }
}

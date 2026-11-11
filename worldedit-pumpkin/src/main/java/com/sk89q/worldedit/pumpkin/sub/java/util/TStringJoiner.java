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

package com.sk89q.worldedit.pumpkin.sub.java.util;

import java.util.Objects;

public final class TStringJoiner {
    private final String prefix;
    private final String delimiter;
    private final String suffix;
    private StringBuilder value;
    private String emptyValue;

    public TStringJoiner(CharSequence delimiter) {
        this(delimiter, "", "");
    }

    public TStringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        this.prefix = Objects.requireNonNull(prefix).toString();
        this.delimiter = Objects.requireNonNull(delimiter).toString();
        this.suffix = Objects.requireNonNull(suffix).toString();
    }

    public TStringJoiner setEmptyValue(CharSequence emptyValue) {
        this.emptyValue = Objects.requireNonNull(emptyValue).toString();
        return this;
    }

    public TStringJoiner add(CharSequence element) {
        if (value == null) {
            value = new StringBuilder();
        } else {
            value.append(delimiter);
        }

        value.append(String.valueOf(element));
        return this;
    }

    public TStringJoiner merge(TStringJoiner other) {
        Objects.requireNonNull(other);

        if (other.value != null) {
            add(other.value.toString());
        }

        return this;
    }

    public int length() {
        if (value == null) {
            return emptyValue != null ? emptyValue.length() : prefix.length() + suffix.length();
        }

        return prefix.length() + value.length() + suffix.length();
    }

    @Override
    public String toString() {
        if (value == null) {
            return emptyValue != null ? emptyValue : prefix + suffix;
        }

        return prefix + value + suffix;
    }
}

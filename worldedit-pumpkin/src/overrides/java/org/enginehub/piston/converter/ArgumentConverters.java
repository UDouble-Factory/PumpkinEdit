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

package org.enginehub.piston.converter;

import com.google.common.reflect.TypeToken;

import java.util.function.Function;

public class ArgumentConverters {

    private static final ArgumentConverter<String> STRING_ARGUMENT_CONVERTER = SimpleArgumentConverter.from((s, c) -> SuccessfulConversion.fromSingle(s), "any text");

    public static ArgumentConverter<String> forString() {
        return STRING_ARGUMENT_CONVERTER;
    }

    private static <T> ArgumentConverter<T> parsing(Function<String, T> parser, String description) {
        return SimpleArgumentConverter.from((s, c) -> {
            try {
                return SuccessfulConversion.fromSingle(parser.apply(s));
            } catch (Throwable t) {
                return FailedConversion.from(t);
            }
        }, description);
    }

    @SuppressWarnings("unchecked")
    public static <T> ArgumentConverter<T> get(TypeToken<T> type) {
        Class<?> raw = type.wrap().getRawType();
        ArgumentConverter<?> result;

        if (raw == String.class) {
            result = forString();
        } else if (raw == Integer.class) {
            result = parsing(Integer::valueOf, "any integer");
        } else if (raw == Double.class) {
            result = parsing(Double::valueOf, "any double");
        } else if (raw == Long.class) {
            result = parsing(Long::valueOf, "any long");
        } else if (raw == Float.class) {
            result = parsing(Float::valueOf, "any float");
        } else if (raw == Short.class) {
            result = parsing(Short::valueOf, "any short");
        } else if (raw == Byte.class) {
            result = parsing(Byte::valueOf, "any byte");
        } else if (raw == Boolean.class) {
            result = parsing(Boolean::valueOf, "any boolean");
        } else if (raw == Character.class) {
            result = parsing(s -> s.charAt(0), "any character");
        } else {
            throw new IllegalArgumentException("No built-in converters for " + type);
        }

        return (ArgumentConverter<T>) result;
    }

    private ArgumentConverters() {
    }
}

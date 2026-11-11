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

package com.google.common.reflect;

import com.google.common.primitives.Primitives;
import com.sk89q.worldedit.registry.Registry;

import java.lang.reflect.Type;

public abstract class TypeToken<T> {
    private final Class<?> rawType;
    private final String arguments;

    protected TypeToken() {
        String name = getClass().getName();

        if (name.equals("com.sk89q.worldedit.command.argument.RegistryConverter$1") || name.equals("com.sk89q.worldedit.command.GeneralCommandsRegistration$1")) {
            this.rawType = Registry.class;
            this.arguments = "<?>";
        } else {
            throw new IllegalStateException("Unknown generic type: " + name);
        }
    }

    private TypeToken(Class<?> rawType) {
        this.rawType = rawType;
        this.arguments = "";
    }

    public static <T> TypeToken<T> of(Class<T> type) {
        return new Simple<>(type);
    }

    @SuppressWarnings("unchecked")
    public final Class<? super T> getRawType() {
        return (Class<? super T>) rawType;
    }

    public final Type getType() {
        return rawType;
    }

    public final TypeToken<T> wrap() {
        if (rawType.isPrimitive()) {
            return new Simple<>(Primitives.wrap(rawType));
        }

        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TypeToken<?> that)) {
            return false;
        }

        return rawType == that.rawType && arguments.equals(that.arguments);
    }

    @Override
    public int hashCode() {
        return rawType.hashCode() * 31 + arguments.hashCode();
    }

    @Override
    public String toString() {
        return rawType.getName() + arguments;
    }

    private static final class Simple<T> extends TypeToken<T> {
        private Simple(Class<?> rawType) {
            super(rawType);
        }
    }
}

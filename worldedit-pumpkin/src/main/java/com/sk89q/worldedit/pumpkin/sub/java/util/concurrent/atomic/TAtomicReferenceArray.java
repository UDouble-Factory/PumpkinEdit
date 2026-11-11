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

package com.sk89q.worldedit.pumpkin.sub.java.util.concurrent.atomic;

import java.util.Arrays;

public class TAtomicReferenceArray<E> {
    private final Object[] values;

    public TAtomicReferenceArray(int length) {
        values = new Object[length];
    }

    public TAtomicReferenceArray(E[] source) {
        values = Arrays.copyOf(source, source.length, Object[].class);
    }

    public final int length() {
        return values.length;
    }

    @SuppressWarnings("unchecked")
    public final E get(int index) {
        return (E) values[index];
    }

    public final void set(int index, E value) {
        values[index] = value;
    }

    public final void lazySet(int index, E value) {
        values[index] = value;
    }

    @SuppressWarnings("unchecked")
    public final E getAndSet(int index, E value) {
        E previous = (E) values[index];
        values[index] = value;
        return previous;
    }

    public final boolean compareAndSet(int index, E expected, E value) {
        if (values[index] != expected) {
            return false;
        }

        values[index] = value;
        return true;
    }

    public final boolean weakCompareAndSet(int index, E expected, E value) {
        return compareAndSet(index, expected, value);
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}

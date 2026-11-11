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

public class TAtomicIntegerArray {
    private final int[] values;

    public TAtomicIntegerArray(int length) {
        values = new int[length];
    }

    public TAtomicIntegerArray(int[] source) {
        values = source.clone();
    }

    public final int length() {
        return values.length;
    }

    public final int get(int index) {
        return values[index];
    }

    public final void set(int index, int value) {
        values[index] = value;
    }

    public final void lazySet(int index, int value) {
        values[index] = value;
    }

    public final int getAndSet(int index, int value) {
        int previous = values[index];
        values[index] = value;
        return previous;
    }

    public final boolean compareAndSet(int index, int expected, int value) {
        if (values[index] != expected) {
            return false;
        }

        values[index] = value;
        return true;
    }

    public final int getAndAdd(int index, int delta) {
        int previous = values[index];
        values[index] = previous + delta;
        return previous;
    }

    public final int addAndGet(int index, int delta) {
        values[index] += delta;
        return values[index];
    }

    public final int getAndIncrement(int index) {
        return getAndAdd(index, 1);
    }

    public final int incrementAndGet(int index) {
        return addAndGet(index, 1);
    }

    public final int getAndDecrement(int index) {
        return getAndAdd(index, -1);
    }

    public final int decrementAndGet(int index) {
        return addAndGet(index, -1);
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}

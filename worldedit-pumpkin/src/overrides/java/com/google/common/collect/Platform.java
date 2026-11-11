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

package com.google.common.collect;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class Platform {

    static <K, V> Map<K, V> newHashMapWithExpectedSize(int expectedSize) {
        return Maps.newHashMapWithExpectedSize(expectedSize);
    }

    static <K, V> Map<K, V> newLinkedHashMapWithExpectedSize(int expectedSize) {
        return Maps.newLinkedHashMapWithExpectedSize(expectedSize);
    }

    static <E> Set<E> newHashSetWithExpectedSize(int expectedSize) {
        return Sets.newHashSetWithExpectedSize(expectedSize);
    }

    static <E> Set<E> newConcurrentHashSet() {
        return Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    static <E> Set<E> newLinkedHashSetWithExpectedSize(int expectedSize) {
        return Sets.newLinkedHashSetWithExpectedSize(expectedSize);
    }

    static <K, V> Map<K, V> preservesInsertionOrderOnPutsMap() {
        return new LinkedHashMap<>();
    }

    static <K, V> Map<K, V> preservesInsertionOrderOnPutsMapWithExpectedSize(int expectedSize) {
        return Maps.newLinkedHashMapWithExpectedSize(expectedSize);
    }

    static <E> Set<E> preservesInsertionOrderOnAddsSet() {
        return new LinkedHashSet<>();
    }

    @SuppressWarnings("unchecked")
    static <T> T[] copy(Object[] source, int from, int to, T[] arrayOfType) {
        return Arrays.copyOfRange(source, from, to, (Class<? extends T[]>) arrayOfType.getClass());
    }

    static MapMaker tryWeakKeys(MapMaker mapMaker) {
        return mapMaker.weakKeys();
    }

    static <E extends Enum<E>> Class<E> getDeclaringClassOrObjectForJ2cl(E e) {
        return e.getDeclaringClass();
    }

    static int reduceIterationsIfGwt(int iterations) {
        return iterations;
    }

    static int reduceExponentIfGwt(int exponent) {
        return exponent;
    }

    private Platform() {
    }
}

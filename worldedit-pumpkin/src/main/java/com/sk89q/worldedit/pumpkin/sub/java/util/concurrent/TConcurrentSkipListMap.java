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

package com.sk89q.worldedit.pumpkin.sub.java.util.concurrent;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

public class TConcurrentSkipListMap<K, V> extends TreeMap<K, V> implements ConcurrentMap<K, V> {
    public TConcurrentSkipListMap() {
    }

    public TConcurrentSkipListMap(Comparator<? super K> comparator) {
        super(comparator);
    }

    public TConcurrentSkipListMap(Map<? extends K, ? extends V> source) {
        super(source);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        V existing = get(key);
        if (existing == null) {
            put(key, value);
        }

        return existing;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (containsKey(key) && Objects.equals(get(key), value)) {
            remove(key);
            return true;
        }

        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (containsKey(key) && Objects.equals(get(key), oldValue)) {
            put(key, newValue);
            return true;
        }

        return false;
    }

    @Override
    public V replace(K key, V value) {
        if (containsKey(key)) {
            return put(key, value);
        }

        return null;
    }
}

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

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class TConcurrentLinkedQueue<E> extends AbstractQueue<E> {
    private final ArrayDeque<E> elements = new ArrayDeque<>();

    public TConcurrentLinkedQueue() {
    }

    public TConcurrentLinkedQueue(Collection<? extends E> source) {
        elements.addAll(source);
    }

    @Override
    public boolean offer(E element) {
        return elements.offer(element);
    }

    @Override
    public E poll() {
        return elements.poll();
    }

    @Override
    public E peek() {
        return elements.peek();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public boolean contains(Object element) {
        return elements.contains(element);
    }

    @Override
    public boolean remove(Object element) {
        return elements.remove(element);
    }

    @Override
    public void clear() {
        elements.clear();
    }

    @Override
    public Iterator<E> iterator() {
        Iterator<E> snapshot = new ArrayList<>(elements).iterator();

        return new Iterator<>() {
            private E last;

            @Override
            public boolean hasNext() {
                return snapshot.hasNext();
            }

            @Override
            public E next() {
                last = snapshot.next();
                return last;
            }

            @Override
            public void remove() {
                elements.remove(last);
            }
        };
    }
}

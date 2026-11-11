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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public final class TSpliterators {
    private TSpliterators() {
    }

    public static <T> Spliterator<T> emptySpliterator() {
        return new OfIterator<>(Collections.<T>emptyIterator(), 0, Spliterator.SIZED);
    }

    @SuppressWarnings("unchecked")
    public static <T> Spliterator<T> spliterator(Object[] array, int characteristics) {
        return new OfIterator<>((Iterator<T>) Arrays.asList(array).iterator(), array.length, characteristics | Spliterator.SIZED);
    }

    @SuppressWarnings("unchecked")
    public static <T> Spliterator<T> spliterator(Object[] array, int from, int to, int characteristics) {
        return new OfIterator<>((Iterator<T>) Arrays.asList(array).subList(from, to).iterator(), to - from, characteristics | Spliterator.SIZED);
    }

    public static <T> Spliterator<T> spliterator(Collection<? extends T> collection, int characteristics) {
        return new OfIterator<>(collection.iterator(), collection.size(), characteristics | Spliterator.SIZED);
    }

    public static <T> Spliterator<T> spliterator(Iterator<? extends T> iterator, long size, int characteristics) {
        return new OfIterator<>(iterator, size, characteristics | Spliterator.SIZED);
    }

    public static <T> Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> iterator, int characteristics) {
        return new OfIterator<>(iterator, Long.MAX_VALUE, characteristics & ~Spliterator.SIZED);
    }

    public static Spliterator.OfInt spliterator(int[] array, int characteristics) {
        return spliterator(array, 0, array.length, characteristics);
    }

    public static Spliterator.OfInt spliterator(int[] array, int from, int to, int characteristics) {
        PrimitiveIterator.OfInt iterator = new PrimitiveIterator.OfInt() {
            private int index = from;

            @Override
            public boolean hasNext() {
                return index < to;
            }

            @Override
            public int nextInt() {
                return array[index++];
            }
        };

        return new OfIntIterator(iterator, to - from, characteristics | Spliterator.SIZED);
    }

    public static Spliterator.OfInt spliterator(PrimitiveIterator.OfInt iterator, long size, int characteristics) {
        return new OfIntIterator(iterator, size, characteristics | Spliterator.SIZED);
    }

    public static Spliterator.OfInt spliteratorUnknownSize(PrimitiveIterator.OfInt iterator, int characteristics) {
        return new OfIntIterator(iterator, Long.MAX_VALUE, characteristics & ~Spliterator.SIZED);
    }

    public static Spliterator.OfLong spliterator(PrimitiveIterator.OfLong iterator, long size, int characteristics) {
        return new OfLongIterator(iterator, size, characteristics | Spliterator.SIZED);
    }

    public static Spliterator.OfLong spliteratorUnknownSize(PrimitiveIterator.OfLong iterator, int characteristics) {
        return new OfLongIterator(iterator, Long.MAX_VALUE, characteristics & ~Spliterator.SIZED);
    }

    public static Spliterator.OfDouble spliterator(PrimitiveIterator.OfDouble iterator, long size, int characteristics) {
        return new OfDoubleIterator(iterator, size, characteristics | Spliterator.SIZED);
    }

    public static Spliterator.OfDouble spliteratorUnknownSize(PrimitiveIterator.OfDouble iterator, int characteristics) {
        return new OfDoubleIterator(iterator, Long.MAX_VALUE, characteristics & ~Spliterator.SIZED);
    }

    private static final class OfIterator<T> implements Spliterator<T> {
        private final Iterator<? extends T> iterator;
        private final int characteristics;
        private long remaining;

        private OfIterator(Iterator<? extends T> iterator, long size, int characteristics) {
            this.iterator = iterator;
            this.remaining = size;
            this.characteristics = characteristics;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (!iterator.hasNext()) {
                return false;
            }

            action.accept(iterator.next());
            if (remaining != Long.MAX_VALUE) {
                remaining--;
            }
            return true;
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return remaining;
        }

        @Override
        public int characteristics() {
            return characteristics;
        }
    }

    private static final class OfIntIterator implements Spliterator.OfInt {
        private final PrimitiveIterator.OfInt iterator;
        private final int characteristics;
        private long remaining;

        private OfIntIterator(PrimitiveIterator.OfInt iterator, long size, int characteristics) {
            this.iterator = iterator;
            this.remaining = size;
            this.characteristics = characteristics;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (!iterator.hasNext()) {
                return false;
            }

            action.accept(iterator.nextInt());
            if (remaining != Long.MAX_VALUE) {
                remaining--;
            }
            return true;
        }

        @Override
        public Spliterator.OfInt trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return remaining;
        }

        @Override
        public int characteristics() {
            return characteristics;
        }
    }

    private static final class OfLongIterator implements Spliterator.OfLong {
        private final PrimitiveIterator.OfLong iterator;
        private final int characteristics;
        private long remaining;

        private OfLongIterator(PrimitiveIterator.OfLong iterator, long size, int characteristics) {
            this.iterator = iterator;
            this.remaining = size;
            this.characteristics = characteristics;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            if (!iterator.hasNext()) {
                return false;
            }

            action.accept(iterator.nextLong());
            if (remaining != Long.MAX_VALUE) {
                remaining--;
            }
            return true;
        }

        @Override
        public Spliterator.OfLong trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return remaining;
        }

        @Override
        public int characteristics() {
            return characteristics;
        }
    }

    private static final class OfDoubleIterator implements Spliterator.OfDouble {
        private final PrimitiveIterator.OfDouble iterator;
        private final int characteristics;
        private long remaining;

        private OfDoubleIterator(PrimitiveIterator.OfDouble iterator, long size, int characteristics) {
            this.iterator = iterator;
            this.remaining = size;
            this.characteristics = characteristics;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if (!iterator.hasNext()) {
                return false;
            }

            action.accept(iterator.nextDouble());
            if (remaining != Long.MAX_VALUE) {
                remaining--;
            }
            return true;
        }

        @Override
        public Spliterator.OfDouble trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return remaining;
        }

        @Override
        public int characteristics() {
            return characteristics;
        }
    }
}

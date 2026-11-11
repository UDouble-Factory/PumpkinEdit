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

import java.io.Serializable;
import java.util.Random;

public final class TUUID implements Serializable, Comparable<TUUID> {
    private static final Random RANDOM = new Random(System.nanoTime() ^ System.currentTimeMillis());
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final long mostSigBits;
    private final long leastSigBits;

    public TUUID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    public static TUUID randomUUID() {
        long most = RANDOM.nextLong();
        long least = RANDOM.nextLong();
        most = (most & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000004000L;
        least = (least & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new TUUID(most, least);
    }

    public static TUUID nameUUIDFromBytes(byte[] name) {
        long most = 0xcbf29ce484222325L;
        long least = 0x84222325cbf29ce4L;

        for (byte value : name) {
            most = (most ^ (value & 0xFF)) * 0x100000001b3L;
            least = (least ^ (value & 0xFF)) * 0x100000001b3L + most;
        }

        most = (most & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000003000L;
        least = (least & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new TUUID(most, least);
    }

    public static TUUID fromString(String name) {
        String[] parts = name.split("-");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid UUID string: " + name);
        }

        long most = Long.parseLong(parts[0], 16) & 0xFFFFFFFFL;
        most = (most << 16) | (Long.parseLong(parts[1], 16) & 0xFFFFL);
        most = (most << 16) | (Long.parseLong(parts[2], 16) & 0xFFFFL);

        long least = Long.parseLong(parts[3], 16) & 0xFFFFL;
        least = (least << 48) | (Long.parseLong(parts[4], 16) & 0xFFFFFFFFFFFFL);

        return new TUUID(most, least);
    }

    public long getMostSignificantBits() {
        return mostSigBits;
    }

    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    public int version() {
        return (int) ((mostSigBits >> 12) & 0x0F);
    }

    private static void digits(StringBuilder builder, long value, int count) {
        for (int shift = (count - 1) * 4; shift >= 0; shift -= 4) {
            builder.append(HEX[(int) ((value >>> shift) & 0x0F)]);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(36);
        digits(builder, mostSigBits >>> 32, 8);
        builder.append('-');
        digits(builder, mostSigBits >>> 16, 4);
        builder.append('-');
        digits(builder, mostSigBits, 4);
        builder.append('-');
        digits(builder, leastSigBits >>> 48, 4);
        builder.append('-');
        digits(builder, leastSigBits, 12);
        return builder.toString();
    }

    @Override
    public int hashCode() {
        long mixed = mostSigBits ^ leastSigBits;
        return ((int) (mixed >> 32)) ^ (int) mixed;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TUUID that && mostSigBits == that.mostSigBits && leastSigBits == that.leastSigBits;
    }

    @Override
    public int compareTo(TUUID other) {
        int most = Long.compare(mostSigBits, other.mostSigBits);
        return most != 0 ? most : Long.compare(leastSigBits, other.leastSigBits);
    }
}

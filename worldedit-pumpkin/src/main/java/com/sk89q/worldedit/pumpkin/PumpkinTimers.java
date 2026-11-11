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

package com.sk89q.worldedit.pumpkin;

import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

public final class PumpkinTimers {
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static final class Entry {
        private final Object owner;
        private final BooleanSupplier cancelled;
        private final LongConsumer task;
        private final long period;
        private long next;

        private Entry(Object owner, BooleanSupplier cancelled, LongConsumer task, long next, long period) {
            this.owner = owner;
            this.cancelled = cancelled;
            this.task = task;
            this.next = next;
            this.period = period;
        }
    }

    private PumpkinTimers() {
    }

    public static void add(Object owner, BooleanSupplier cancelled, LongConsumer task, long delay, long period) {
        ENTRIES.add(new Entry(owner, cancelled, task, System.currentTimeMillis() + Math.max(0, delay), period));
    }

    public static void cancel(Object owner) {
        ENTRIES.removeIf(entry -> entry.owner == owner);
    }

    public static int purge(Object owner) {
        int before = ENTRIES.size();
        ENTRIES.removeIf(entry -> entry.owner == owner && entry.cancelled.getAsBoolean());
        return before - ENTRIES.size();
    }

    static void runDue() {
        long now = System.currentTimeMillis();

        for (Entry entry : new ArrayList<>(ENTRIES)) {
            if (entry.cancelled.getAsBoolean()) {
                ENTRIES.remove(entry);
                continue;
            }
            if (entry.next > now) {
                continue;
            }

            long scheduled = entry.next;
            if (entry.period > 0) {
                entry.next = now + entry.period;
            } else {
                ENTRIES.remove(entry);
            }

            try {
                entry.task.accept(scheduled);
            } catch (Throwable failure) {
                ENTRIES.remove(entry);
                LogManager.getLogger().error("A scheduled task failed", failure);
            }
        }
    }
}

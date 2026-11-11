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

import com.sk89q.worldedit.pumpkin.PumpkinTimers;

public class TTimer {
    private boolean cancelled;

    public TTimer() {
    }

    public TTimer(boolean daemon) {
    }

    public TTimer(String name) {
    }

    public TTimer(String name, boolean daemon) {
    }

    public void schedule(TTimerTask task, long delay) {
        add(task, delay, 0);
    }

    public void schedule(TTimerTask task, long delay, long period) {
        add(task, delay, period);
    }

    public void scheduleAtFixedRate(TTimerTask task, long delay, long period) {
        add(task, delay, period);
    }

    private void add(TTimerTask task, long delay, long period) {
        if (cancelled) {
            throw new IllegalStateException("Timer already cancelled.");
        }

        PumpkinTimers.add(this, () -> task.cancelled, scheduled -> {
            task.scheduledTime = scheduled;
            task.run();
        }, delay, period);
    }

    public void cancel() {
        cancelled = true;
        PumpkinTimers.cancel(this);
    }

    public int purge() {
        return PumpkinTimers.purge(this);
    }
}

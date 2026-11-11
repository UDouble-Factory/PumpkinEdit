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

package com.sk89q.worldedit.pumpkin.sub.java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class TReentrantReadWriteLock implements ReadWriteLock {
    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();

    public TReentrantReadWriteLock() {
    }

    public TReentrantReadWriteLock(boolean fair) {
    }

    @Override
    public ReadLock readLock() {
        return readLock;
    }

    @Override
    public WriteLock writeLock() {
        return writeLock;
    }

    public boolean isWriteLocked() {
        return writeLock.holds > 0;
    }

    public boolean isWriteLockedByCurrentThread() {
        return writeLock.holds > 0;
    }

    public static class ReadLock implements Lock {
        private int holds;

        @Override
        public void lock() {
            holds++;
        }

        @Override
        public void lockInterruptibly() {
            holds++;
        }

        @Override
        public boolean tryLock() {
            holds++;
            return true;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) {
            holds++;
            return true;
        }

        @Override
        public void unlock() {
            if (holds == 0) {
                throw new IllegalMonitorStateException();
            }

            holds--;
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    public static class WriteLock implements Lock {
        private int holds;

        @Override
        public void lock() {
            holds++;
        }

        @Override
        public void lockInterruptibly() {
            holds++;
        }

        @Override
        public boolean tryLock() {
            holds++;
            return true;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) {
            holds++;
            return true;
        }

        @Override
        public void unlock() {
            if (holds == 0) {
                throw new IllegalMonitorStateException();
            }

            holds--;
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        public boolean isHeldByCurrentThread() {
            return holds > 0;
        }
    }
}

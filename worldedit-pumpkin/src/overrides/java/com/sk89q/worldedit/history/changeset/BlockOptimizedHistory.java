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

package com.sk89q.worldedit.history.changeset;

import com.google.common.collect.Iterators;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkNotNull;

public class BlockOptimizedHistory extends ArrayListHistory {
    private static final int PAGE_BITS = 14;
    private static final int PAGE_SIZE = 1 << PAGE_BITS;
    private static final int PAGE_MASK = PAGE_SIZE - 1;

    private final List<long[]> positions = new ArrayList<>();
    private final List<int[]> previous = new ArrayList<>();
    private final List<int[]> current = new ArrayList<>();
    private final List<BaseBlock> detailed = new ArrayList<>();
    private final Object2IntOpenHashMap<BaseBlock> detailedIndexes = new Object2IntOpenHashMap<>();
    private int count;

    public BlockOptimizedHistory() {
        detailedIndexes.defaultReturnValue(-1);
    }

    @Override
    public void add(Change change) {
        checkNotNull(change);

        if (isRecordingChanges()) {
            if (change instanceof BlockChange blockChange && BlockVector3.isLongPackable(blockChange.position())) {
                addBlockChange(blockChange);
            } else {
                super.add(change);
            }
        }
    }

    private void addBlockChange(BlockChange change) {
        int page = count >>> PAGE_BITS;
        int offset = count & PAGE_MASK;

        if (page == positions.size()) {
            positions.add(new long[PAGE_SIZE]);
            previous.add(new int[PAGE_SIZE]);
            current.add(new int[PAGE_SIZE]);
        }

        positions.get(page)[offset] = change.position().toLongPackedForm();
        previous.get(page)[offset] = encode(change.previous());
        current.get(page)[offset] = encode(change.current());
        count++;
    }

    private int encode(BaseBlock block) {
        if (block.getNbtReference() == null) {
            int id = BlockStateIdAccess.getBlockStateId(block.toImmutableState());
            if (BlockStateIdAccess.isValidInternalId(id) && id >= 0) {
                return id;
            }
        }

        int index = detailedIndexes.getInt(block);
        if (index < 0) {
            index = detailed.size();
            detailed.add(block);
            detailedIndexes.put(block, index);
        }

        return -index - 1;
    }

    private BaseBlock decode(int value) {
        if (value < 0) {
            return detailed.get(-value - 1);
        }

        BlockState state = BlockStateIdAccess.getBlockStateById(value);
        if (state == null) {
            throw new IllegalStateException("Unknown block state in history: " + value);
        }

        return state.toBaseBlock();
    }

    private Change changeAt(int index) {
        int page = index >>> PAGE_BITS;
        int offset = index & PAGE_MASK;
        BlockVector3 position = BlockVector3.fromLongPackedForm(positions.get(page)[offset]);

        return new BlockChange(position, decode(previous.get(page)[offset]), decode(current.get(page)[offset]));
    }

    @Override
    public Iterator<Change> forwardIterator() {
        return Iterators.concat(super.forwardIterator(), new BlockChangeIterator(0, 1));
    }

    @Override
    public Iterator<Change> backwardIterator() {
        return Iterators.concat(super.backwardIterator(), new BlockChangeIterator(count - 1, -1));
    }

    @Override
    public int size() {
        return super.size() + count;
    }

    private final class BlockChangeIterator implements Iterator<Change> {
        private final int step;
        private int index;

        private BlockChangeIterator(int start, int step) {
            this.index = start;
            this.step = step;
        }

        @Override
        public boolean hasNext() {
            return index >= 0 && index < count;
        }

        @Override
        public Change next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Change change = changeAt(index);
            index += step;
            return change;
        }
    }
}

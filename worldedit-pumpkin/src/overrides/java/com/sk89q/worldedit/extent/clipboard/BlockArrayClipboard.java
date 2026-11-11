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

package com.sk89q.worldedit.extent.clipboard;

import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class BlockArrayClipboard implements Clipboard {
    private static final int MAX_STATES = Character.MAX_VALUE + 1;

    private static BlockVector3 getDimensions(Region region) {
        return region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
    }

    private final Region region;
    private BlockVector3 origin;
    private final int yStride;
    private final int zStride;
    private final char[] blocks;
    private final List<BlockState> states = new ArrayList<>();
    private final Reference2IntOpenHashMap<BlockState> stateIndexes = new Reference2IntOpenHashMap<>();
    private final Int2ObjectOpenHashMap<BaseBlock> tiles = new Int2ObjectOpenHashMap<>();
    private BiomeType[] biomes = null;
    private final List<ClipboardEntity> entities = new ArrayList<>();

    public BlockArrayClipboard(Region region) {
        checkNotNull(region);
        this.region = region.clone();
        this.origin = region.getMinimumPoint();

        BlockVector3 dimensions = getDimensions(region);
        blocks = new char[dimensions.x() * dimensions.y() * dimensions.z()];
        yStride = dimensions.x();
        zStride = yStride * dimensions.y();

        stateIndexes.defaultReturnValue(-1);
        stateIndex(BlockTypes.AIR.getDefaultState());
    }

    private int indexBlockVecBasedArray(BlockVector3 v) {
        return v.x() + (v.y() * yStride) + (v.z() * zStride);
    }

    private int stateIndex(BlockState state) {
        int index = stateIndexes.getInt(state);
        if (index >= 0) {
            return index;
        }

        if (states.size() >= MAX_STATES) {
            throw new IllegalStateException("The clipboard holds too many different block states");
        }

        index = states.size();
        states.add(state);
        stateIndexes.put(state, index);
        return index;
    }

    @Override
    public Region getRegion() {
        return region.clone();
    }

    @Override
    public BlockVector3 getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
        this.origin = origin;
    }

    @Override
    public BlockVector3 getDimensions() {
        return getDimensions(region);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return region.getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return region.getMaximumPoint();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : entities) {
            if (region.contains(entity.getLocation().toVector().toBlockPoint())) {
                filtered.add(entity);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        ClipboardEntity ret = new ClipboardEntity(location, entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        if (region.contains(position)) {
            BlockVector3 v = position.subtract(region.getMinimumPoint());
            return states.get(blocks[indexBlockVecBasedArray(v)]);
        }

        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        if (region.contains(position)) {
            BlockVector3 v = position.subtract(region.getMinimumPoint());
            int index = indexBlockVecBasedArray(v);

            if (!tiles.isEmpty()) {
                BaseBlock tile = tiles.get(index);
                if (tile != null) {
                    return tile;
                }
            }

            return states.get(blocks[index]).toBaseBlock();
        }

        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) {
        if (region.contains(position)) {
            BlockVector3 v = position.subtract(region.getMinimumPoint());
            int index = indexBlockVecBasedArray(v);
            BaseBlock base = block.toBaseBlock();

            blocks[index] = (char) stateIndex(base.toImmutableState());

            if (base.getNbtReference() != null) {
                tiles.put(index, base);
            } else if (!tiles.isEmpty()) {
                tiles.remove(index);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasBiomes() {
        return biomes != null;
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        if (biomes != null
                && position.containedWithin(getMinimumPoint(), getMaximumPoint())) {
            BlockVector3 v = position.subtract(region.getMinimumPoint());
            BiomeType biomeType = biomes[indexBlockVecBasedArray(v)];
            if (biomeType != null) {
                return biomeType;
            }
        }

        return BiomeTypes.OCEAN;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        if (position.containedWithin(getMinimumPoint(), getMaximumPoint())) {
            BlockVector3 v = position.subtract(region.getMinimumPoint());
            if (biomes == null) {
                BlockVector3 dimensions = getDimensions();
                biomes = new BiomeType[dimensions.x() * dimensions.y() * dimensions.z()];
            }
            biomes[indexBlockVecBasedArray(v)] = biome;
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

    private class ClipboardEntity extends StoredEntity {
        ClipboardEntity(Location location, BaseEntity entity) {
            super(location, entity);
        }

        @Override
        public boolean remove() {
            return entities.remove(this);
        }

        @Nullable
        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return null;
        }
    }

}

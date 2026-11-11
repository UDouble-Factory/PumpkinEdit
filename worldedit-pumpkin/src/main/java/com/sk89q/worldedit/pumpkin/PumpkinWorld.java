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

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.generation.TreeType;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import org.enginehub.linbus.tree.LinCompoundTag;
import pumpkin.common.BlockPos;
import pumpkin.runtime.Option;
import pumpkin.world.BlockFlags;
import pumpkin.world.World;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

final class PumpkinWorld extends AbstractWorld {
    private final PumpkinWorldEdit plugin;
    private final World handle;
    private final String name;
    private final int minY;
    private final int maxY;

    PumpkinWorld(PumpkinWorldEdit plugin, World handle) {
        this.plugin = plugin;
        this.handle = handle;
        this.name = handle.getName();
        this.minY = handle.getMinY();
        this.maxY = minY + (minY < 0 ? 384 : 256) - 1;
    }

    World handle() {
        return handle;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String id() {
        return name;
    }

    @Nullable
    @Override
    public Path getStoragePath() {
        return null;
    }

    @Override
    public int getMinY() {
        return minY;
    }

    @Override
    public int getMaxY() {
        return maxY;
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        if (position.y() < minY || position.y() > maxY) {
            return BlockTypes.AIR.getDefaultState();
        }

        BlockState state = BlockStateIdAccess.getBlockStateById(handle.getBlockStateId(PumpkinAdapter.blockPos(position)));
        return state == null ? BlockTypes.AIR.getDefaultState() : state;
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        BlockState state = getBlock(position);
        if (state.getBlockType().getMaterial().isAir()) {
            return state.toBaseBlock();
        }

        Option<byte[]> data = handle.getBlockEntityNbt(PumpkinAdapter.blockPos(position));
        if (!data.isSome()) {
            return state.toBaseBlock();
        }

        try {
            return state.toBaseBlock(PumpkinAdapter.readNbt(data.value()));
        } catch (IOException e) {
            return state.toBaseBlock();
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects) throws WorldEditException {
        if (position.y() < minY || position.y() > maxY) {
            return false;
        }

        int id = BlockStateIdAccess.getBlockStateId(block.toImmutableState());
        if (!BlockStateIdAccess.isValidInternalId(id)) {
            return false;
        }

        long flags = BlockFlags.NOTIFY_LISTENERS | BlockFlags.SKIP_DROPS | BlockFlags.SKIP_BLOCK_ENTITY_REPLACED_CALLBACK;
        if (sideEffects.shouldApply(SideEffect.NEIGHBORS)) {
            flags |= BlockFlags.NOTIFY_NEIGHBORS;
        }

        BlockPos target = PumpkinAdapter.blockPos(position);
        handle.setBlockState(target, id, new BlockFlags(flags));

        if (block instanceof BaseBlock base) {
            LinCompoundTag nbt = base.getNbt();
            if (nbt != null) {
                try {
                    handle.setBlockEntityNbt(target, PumpkinAdapter.writeNbt(nbt));
                } catch (IOException e) {
                    return true;
                }
            }
        }

        return true;
    }

    @Override
    public Set<SideEffect> applySideEffects(BlockVector3 position, BlockState previousType, SideEffectSet sideEffectSet) throws WorldEditException {
        return Set.of();
    }

    @Override
    public int getBlockLightLevel(BlockVector3 position) {
        return handle.getBlockLight(PumpkinAdapter.blockPos(position));
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        return false;
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {
    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {
        if (position.y() < minY || position.y() > maxY) {
            return;
        }

        int air = BlockStateIdAccess.getBlockStateId(BlockTypes.AIR.getDefaultState());
        handle.setBlockState(PumpkinAdapter.blockPos(position), air, new BlockFlags(BlockFlags.NOTIFY_NEIGHBORS | BlockFlags.NOTIFY_LISTENERS));
    }

    @Override
    public boolean generateTree(TreeType type, EditSession editSession, BlockVector3 position) {
        return false;
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return BiomeType.REGISTRY.get(PumpkinAdapter.enumKey(handle.getBiome(PumpkinAdapter.blockPos(position))));
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return false;
    }

    @Override
    public WeatherType getWeather() {
        if (handle.isThundering()) {
            return WeatherTypes.THUNDER_STORM;
        }

        return handle.isRaining() ? WeatherTypes.RAIN : WeatherTypes.CLEAR;
    }

    @Override
    public long getRemainingWeatherDuration() {
        return 0;
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        handle.setThundering(weatherType == WeatherTypes.THUNDER_STORM);
        handle.setRaining(weatherType != WeatherTypes.CLEAR);
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        setWeather(weatherType);
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        BlockPos spawn = handle.getSpawnLocation().pos();
        return BlockVector3.at(spawn.x(), spawn.y(), spawn.z());
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        List<Entity> result = new ArrayList<>();

        for (Entity entity : getEntities()) {
            if (region.contains(entity.getLocation().toVector().toBlockPoint())) {
                result.add(entity);
            }
        }

        return result;
    }

    @Override
    public List<? extends Entity> getEntities() {
        List<Entity> result = new ArrayList<>();

        for (pumpkin.world.Entity entity : handle.getEntities()) {
            result.add(new PumpkinEntity(plugin, this, entity));
        }

        return result;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        return null;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PumpkinWorld that && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

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

import com.sk89q.worldedit.world.registry.BlockMaterial;
import pumpkin.world.Block;
import pumpkin.world.BlockState;
import pumpkin.world.PistonBehavior;

final class PumpkinBlockMaterial implements BlockMaterial {
    private final Block block;
    private final BlockState state;

    PumpkinBlockMaterial(Block block, BlockState state) {
        this.block = block;
        this.state = state;
    }

    @Override
    public boolean isAir() {
        return state.isAir();
    }

    @Override
    public boolean isFullCube() {
        return state.isFullCube();
    }

    @Override
    public boolean isOpaque() {
        return state.opacity() >= 15;
    }

    @Override
    public boolean isPowerSource() {
        return false;
    }

    @Override
    public boolean isLiquid() {
        return state.isLiquid();
    }

    @Override
    public boolean isSolid() {
        return state.isSolid();
    }

    @Override
    public float getHardness() {
        return state.hardness();
    }

    @Override
    public float getResistance() {
        return block.blastResistance();
    }

    @Override
    public float getSlipperiness() {
        return block.slipperiness();
    }

    @Override
    public int getLightValue() {
        return state.luminance();
    }

    @Override
    public boolean isFragileWhenPushed() {
        return state.pistonBehavior() == PistonBehavior.DESTROY;
    }

    @Override
    public boolean isUnpushable() {
        return state.pistonBehavior() == PistonBehavior.BLOCK;
    }

    @Override
    public boolean isTicksRandomly() {
        return state.hasRandomTicks();
    }

    @Override
    public boolean isMovementBlocker() {
        return state.isSolid();
    }

    @Override
    public boolean isBurnable() {
        return state.burnable();
    }

    @Override
    public boolean isToolRequired() {
        return state.toolRequired();
    }

    @Override
    public boolean isReplacedDuringPlacement() {
        return state.replaceable();
    }

    @Override
    public boolean isTranslucent() {
        return state.opacity() < 15;
    }

    @Override
    public boolean hasContainer() {
        return false;
    }
}

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

import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.extent.inventory.OutOfBlocksException;
import com.sk89q.worldedit.extent.inventory.OutOfSpaceException;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.item.ItemType;
import pumpkin.itemstack.ItemStack;
import pumpkin.player.Player;
import pumpkin.runtime.Option;

final class PumpkinBlockBag extends BlockBag {
    private static final int DEFAULT_STACK_SIZE = 64;

    private final PumpkinPlayer player;
    private String[] items;
    private int[] counts;
    private int[] limits;
    private boolean[] changed;

    PumpkinBlockBag(PumpkinPlayer player) {
        this.player = player;
    }

    private void loadInventory() {
        if (items != null) {
            return;
        }

        Player handle = player.handle();
        int size = PumpkinPlayer.INVENTORY_SLOTS;
        items = new String[size];
        counts = new int[size];
        limits = new int[size];
        changed = new boolean[size];

        for (int slot = 0; slot < size; slot++) {
            Option<ItemStack> existing = handle.getInventoryItem(slot);
            if (!existing.isSome()) {
                continue;
            }

            try (ItemStack stack = existing.value()) {
                items[slot] = PumpkinAdapter.key(stack.getRegistryKey());
                counts[slot] = stack.getCount();
                limits[slot] = stack.getMaxCount();
            }
        }
    }

    @Override
    public void fetchBlock(BlockState blockState) throws BlockBagException {
        BlockType type = blockState.getBlockType();
        if (type.getMaterial().isAir()) {
            throw new IllegalArgumentException("Can't fetch air block");
        }

        ItemType item = type.getItemType();
        if (item == null) {
            throw new OutOfBlocksException();
        }

        loadInventory();

        for (int slot = 0; slot < items.length; slot++) {
            if (!item.id().equals(items[slot])) {
                continue;
            }

            counts[slot]--;
            changed[slot] = true;
            if (counts[slot] <= 0) {
                items[slot] = null;
                counts[slot] = 0;
            }
            return;
        }

        throw new OutOfBlocksException();
    }

    @Override
    public void storeBlock(BlockState blockState, int amount) throws BlockBagException {
        BlockType type = blockState.getBlockType();
        if (type.getMaterial().isAir()) {
            throw new IllegalArgumentException("Can't store air block");
        }

        ItemType item = type.getItemType();
        if (item == null) {
            throw new IllegalArgumentException("This block cannot be stored");
        }

        loadInventory();
        int remaining = amount;

        for (int slot = 0; slot < items.length && remaining > 0; slot++) {
            if (!item.id().equals(items[slot]) || counts[slot] >= limits[slot]) {
                continue;
            }

            int moved = Math.min(limits[slot] - counts[slot], remaining);
            counts[slot] += moved;
            changed[slot] = true;
            remaining -= moved;
        }

        for (int slot = 0; slot < items.length && remaining > 0; slot++) {
            if (items[slot] != null) {
                continue;
            }

            int moved = Math.min(DEFAULT_STACK_SIZE, remaining);
            items[slot] = item.id();
            counts[slot] = moved;
            limits[slot] = DEFAULT_STACK_SIZE;
            changed[slot] = true;
            remaining -= moved;
        }

        if (remaining > 0) {
            throw new OutOfSpaceException(type);
        }
    }

    @Override
    public void flushChanges() {
        if (items == null) {
            return;
        }

        Player handle = player.handle();

        for (int slot = 0; slot < items.length; slot++) {
            if (!changed[slot]) {
                continue;
            }

            if (items[slot] == null) {
                handle.setInventoryItem(slot, Option.none());
            } else {
                handle.setInventoryItem(slot, Option.some(stack(handle, slot)));
            }
        }

        items = null;
        counts = null;
        limits = null;
        changed = null;
    }

    private ItemStack stack(Player handle, int slot) {
        Option<ItemStack> existing = handle.getInventoryItem(slot);
        if (existing.isSome()) {
            ItemStack stack = existing.value();
            if (PumpkinAdapter.key(stack.getRegistryKey()).equals(items[slot])) {
                stack.setCount(counts[slot]);
                return stack;
            }

            stack.close();
        }

        return ItemStack.create(items[slot], counts[slot]);
    }

    @Override
    public void addSourcePosition(Location pos) {
    }

    @Override
    public void addSingleSourcePosition(Location pos) {
    }
}

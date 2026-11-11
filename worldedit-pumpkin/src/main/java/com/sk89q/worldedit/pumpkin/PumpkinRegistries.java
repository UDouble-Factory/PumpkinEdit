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

import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.translation.TranslationManager;
import com.sk89q.worldedit.world.biome.BiomeData;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import com.sk89q.worldedit.world.registry.BlockCategoryRegistry;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.EntityRegistry;
import com.sk89q.worldedit.world.registry.ItemCategoryRegistry;
import com.sk89q.worldedit.world.registry.ItemMaterial;
import com.sk89q.worldedit.world.registry.ItemRegistry;
import com.sk89q.worldedit.world.registry.Registries;

import java.util.Set;
import javax.annotation.Nullable;

final class PumpkinRegistries implements Registries {
    private final PumpkinBlockRegistry blocks = new PumpkinBlockRegistry();

    private final ItemRegistry items = new ItemRegistry() {
        @Override
        public Component getRichName(ItemType itemType) {
            return TranslatableComponent.of(TranslationManager.makeTranslationKey("item", itemType.id()));
        }

        @Nullable
        @Override
        public ItemMaterial getMaterial(ItemType itemType) {
            return null;
        }
    };

    private final EntityRegistry entities = new EntityRegistry() {
        @Nullable
        @Override
        public BaseEntity createFromId(String id) {
            return null;
        }
    };

    private final BiomeRegistry biomes = new BiomeRegistry() {
        @Override
        public Component getRichName(BiomeType biomeType) {
            return TranslatableComponent.of(TranslationManager.makeTranslationKey("biome", biomeType.id()));
        }

        @Nullable
        @Override
        @SuppressWarnings("removal")
        public BiomeData getData(BiomeType biome) {
            return null;
        }
    };

    private final BlockCategoryRegistry blockCategories = new BlockCategoryRegistry() {
        @Override
        public Set<BlockType> getCategorisedByName(String category) {
            return Set.of();
        }
    };

    private final ItemCategoryRegistry itemCategories = new ItemCategoryRegistry() {
        @Override
        public Set<ItemType> getCategorisedByName(String category) {
            return Set.of();
        }
    };

    PumpkinBlockRegistry blocks() {
        return blocks;
    }

    @Override
    public BlockRegistry getBlockRegistry() {
        return blocks;
    }

    @Override
    public ItemRegistry getItemRegistry() {
        return items;
    }

    @Override
    public EntityRegistry getEntityRegistry() {
        return entities;
    }

    @Override
    public BiomeRegistry getBiomeRegistry() {
        return biomes;
    }

    @Override
    public BlockCategoryRegistry getBlockCategoryRegistry() {
        return blockCategories;
    }

    @Override
    public ItemCategoryRegistry getItemCategoryRegistry() {
        return itemCategories;
    }
}

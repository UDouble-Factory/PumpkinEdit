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

import com.sk89q.worldedit.entity.metadata.EntityProperties;
import pumpkin.entitytypes.EntityType;
import pumpkin.runtime.Option;
import pumpkin.text.TextComponent;
import pumpkin.world.Entity;
import pumpkin.world.Mob;
import pumpkin.world.MobData;

import java.util.Set;

final class PumpkinEntityProperties implements EntityProperties {
    private static final Set<String> PROJECTILES = Set.of(
        "ARROW", "SPECTRAL_ARROW", "TRIDENT", "SNOWBALL", "EGG", "ENDER_PEARL", "EXPERIENCE_BOTTLE", "SPLASH_POTION", "LINGERING_POTION",
        "FIREBALL", "SMALL_FIREBALL", "DRAGON_FIREBALL", "WITHER_SKULL", "SHULKER_BULLET", "LLAMA_SPIT", "FIREWORK_ROCKET", "WIND_CHARGE",
        "BREEZE_WIND_CHARGE", "FISHING_BOBBER"
    );
    private static final Set<String> ANIMALS = Set.of(
        "ARMADILLO", "AXOLOTL", "BEE", "CAMEL", "CAMEL_HUSK", "CAT", "CHICKEN", "COW", "DONKEY", "FOX", "FROG", "GOAT", "HAPPY_GHAST", "HOGLIN",
        "HORSE", "LLAMA", "MOOSHROOM", "MULE", "NAUTILUS", "OCELOT", "PANDA", "PARROT", "PIG", "POLAR_BEAR", "RABBIT", "SHEEP", "SKELETON_HORSE",
        "SNIFFER", "STRIDER", "TRADER_LLAMA", "TURTLE", "WOLF", "ZOMBIE_HORSE", "ZOMBIE_NAUTILUS"
    );
    private static final Set<String> WATER_CREATURES = Set.of("COD", "DOLPHIN", "GLOW_SQUID", "PUFFERFISH", "SALMON", "SQUID", "TADPOLE", "TROPICAL_FISH");
    private static final Set<String> GOLEMS = Set.of("COPPER_GOLEM", "IRON_GOLEM", "SHULKER", "SNOW_GOLEM");
    private static final Set<String> NPCS = Set.of("VILLAGER", "WANDERING_TRADER");

    private final Entity handle;
    private final EntityType type;
    private final String name;

    PumpkinEntityProperties(Entity handle, EntityType type) {
        this.handle = handle;
        this.type = type;
        this.name = type.name();
    }

    @Override
    public boolean isPlayerDerived() {
        return type == EntityType.PLAYER;
    }

    @Override
    public boolean isProjectile() {
        return PROJECTILES.contains(name);
    }

    @Override
    public boolean isItem() {
        return type == EntityType.ITEM;
    }

    @Override
    public boolean isFallingBlock() {
        return type == EntityType.FALLING_BLOCK;
    }

    @Override
    public boolean isPainting() {
        return type == EntityType.PAINTING;
    }

    @Override
    public boolean isItemFrame() {
        return type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME;
    }

    @Override
    public boolean isBoat() {
        return name.endsWith("_BOAT") || name.endsWith("_RAFT");
    }

    @Override
    public boolean isMinecart() {
        return name.endsWith("MINECART");
    }

    @Override
    public boolean isTNT() {
        return type == EntityType.TNT;
    }

    @Override
    public boolean isExperienceOrb() {
        return type == EntityType.EXPERIENCE_ORB;
    }

    @Override
    public boolean isLiving() {
        return handle.isLiving();
    }

    @Override
    public boolean isAnimal() {
        return ANIMALS.contains(name);
    }

    @Override
    public boolean isAmbient() {
        return type == EntityType.BAT;
    }

    @Override
    public boolean isNPC() {
        return NPCS.contains(name);
    }

    @Override
    public boolean isGolem() {
        return GOLEMS.contains(name);
    }

    @Override
    public boolean isTamed() {
        Option<Mob> found = handle.asMob();
        if (!found.isSome()) {
            return false;
        }

        try (Mob mob = found.value()) {
            MobData data = mob.getMobData();
            if (data instanceof MobData.Wolf wolf) {
                return wolf.value().isTamed();
            }
            if (data instanceof MobData.Cat cat) {
                return cat.value().isTamed();
            }

            return false;
        }
    }

    @Override
    public boolean isTagged() {
        Option<TextComponent> custom = handle.getCustomName();
        if (!custom.isSome()) {
            return false;
        }

        custom.value().close();
        return true;
    }

    @Override
    public boolean isArmorStand() {
        return type == EntityType.ARMOR_STAND;
    }

    @Override
    public boolean isPasteable() {
        return type != EntityType.PLAYER && type != EntityType.ENDER_DRAGON;
    }

    @Override
    public boolean isWaterCreature() {
        return WATER_CREATURES.contains(name);
    }
}

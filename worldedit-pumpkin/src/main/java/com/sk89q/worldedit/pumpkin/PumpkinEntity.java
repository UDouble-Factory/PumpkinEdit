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
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.metadata.EntityProperties;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import pumpkin.entitytypes.EntityType;
import pumpkin.runtime.Option;

import javax.annotation.Nullable;

final class PumpkinEntity implements Entity {
    private final PumpkinWorldEdit plugin;
    private final PumpkinWorld world;
    private final pumpkin.world.Entity handle;
    private final EntityType type;
    private boolean removed;

    PumpkinEntity(PumpkinWorldEdit plugin, PumpkinWorld world, pumpkin.world.Entity handle) {
        this.plugin = plugin;
        this.world = world;
        this.handle = handle;
        this.type = handle.getType();
    }

    @Nullable
    @Override
    public BaseEntity getState() {
        return null;
    }

    @Override
    public boolean remove() {
        if (removed || type == EntityType.PLAYER) {
            return false;
        }

        handle.remove();
        removed = true;
        return true;
    }

    @Override
    public Location getLocation() {
        return new Location(world, PumpkinAdapter.vector(handle.getPosition()), handle.getYaw(), handle.getPitch());
    }

    @Override
    public boolean setLocation(Location location) {
        if (removed || !(location.getExtent() instanceof PumpkinWorld target)) {
            return false;
        }

        Option<pumpkin.world.World> opened = plugin.openWorld(target.getName());
        if (!opened.isSome()) {
            return false;
        }

        handle.teleport(PumpkinAdapter.position(location.toVector()), opened.value());
        handle.setRotation(location.getYaw(), location.getPitch());
        return true;
    }

    @Override
    public Extent getExtent() {
        return world;
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        if (EntityProperties.class.isAssignableFrom(cls)) {
            return cls.cast(new PumpkinEntityProperties(handle, type));
        }

        return null;
    }
}

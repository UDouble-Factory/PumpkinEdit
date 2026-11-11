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

import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlatform;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.MultiUserPlatform;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.io.ResourceLoader;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.Registries;
import org.enginehub.piston.CommandManager;
import pumpkin.entitytypes.EntityType;

import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

final class PumpkinPlatform extends AbstractPlatform implements MultiUserPlatform {
    private final PumpkinWorldEdit plugin;
    private final PumpkinRegistries registries = new PumpkinRegistries();
    private final ResourceLoader resourceLoader;

    PumpkinPlatform(PumpkinWorldEdit plugin) {
        this.plugin = plugin;
        this.resourceLoader = new ResourceLoader() {
            @Override
            public Path getLocalResource(String pathName) {
                return plugin.dataDirectory().resolve(pathName);
            }
        };
    }

    PumpkinRegistries registries() {
        return registries;
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Override
    public Registries getRegistries() {
        return registries;
    }

    @Override
    public int getDataVersion() {
        return Constants.DATA_VERSION_MC_26_3;
    }

    @Override
    public boolean isValidMobType(String type) {
        String id = PumpkinAdapter.key(type.toLowerCase(Locale.ROOT));

        for (EntityType entity : EntityType.values()) {
            if (PumpkinAdapter.enumKey(entity).equals(id)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void reload() {
        getConfiguration().load();
        super.reload();
    }

    @Override
    public int schedule(long delay, long period, Runnable task) {
        return plugin.schedule(delay, period, task);
    }

    @Override
    public List<? extends World> getWorlds() {
        return plugin.worlds();
    }

    @Nullable
    @Override
    public Player matchPlayer(Player player) {
        if (player instanceof PumpkinPlayer) {
            return player;
        }

        return plugin.player(player.getUniqueId());
    }

    @Nullable
    @Override
    public World matchWorld(World world) {
        if (world instanceof PumpkinWorld) {
            return world;
        }

        for (World candidate : plugin.worlds()) {
            if (candidate.getName().equals(world.getName())) {
                return candidate;
            }
        }

        return null;
    }

    @Override
    public void registerCommands(CommandManager commandManager) {
        plugin.registerCommands(commandManager);
    }

    @Override
    public void setGameHooksEnabled(boolean enabled) {
    }

    @Override
    public LocalConfiguration getConfiguration() {
        return plugin.configuration();
    }

    @Override
    public String getVersion() {
        return PumpkinBuild.VERSION;
    }

    @Override
    public String getPlatformName() {
        return "Pumpkin-Official";
    }

    @Override
    public String getPlatformVersion() {
        return plugin.serverVersion();
    }

    @Override
    public String id() {
        return "enginehub:pumpkin";
    }

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        capabilities.put(Capability.CONFIGURATION, Preference.NORMAL);
        capabilities.put(Capability.GAME_HOOKS, Preference.NORMAL);
        capabilities.put(Capability.PERMISSIONS, Preference.NORMAL);
        capabilities.put(Capability.USER_COMMANDS, Preference.NORMAL);
        capabilities.put(Capability.WORLDEDIT_CUI, Preference.NORMAL);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFERRED);
        return capabilities;
    }

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
        return Set.of(SideEffect.NEIGHBORS);
    }

    @Override
    public Collection<Actor> getConnectedUsers() {
        return plugin.connectedUsers();
    }
}

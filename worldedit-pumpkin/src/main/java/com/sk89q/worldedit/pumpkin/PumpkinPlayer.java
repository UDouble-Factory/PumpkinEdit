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

import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.gamemode.GameModes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import pumpkin.common.Hand;
import pumpkin.itemstack.ItemStack;
import pumpkin.player.JavaPlayer;
import pumpkin.player.Player;
import pumpkin.runtime.Option;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;

final class PumpkinPlayer extends AbstractPlayerActor {
    static final String CUI_CHANNEL = "worldedit:cui";
    static final int INVENTORY_SLOTS = 36;

    private final PumpkinWorldEdit plugin;
    private final UUID id;
    private final String name;
    private Player handle;
    private boolean online = true;

    PumpkinPlayer(PumpkinWorldEdit plugin, Player handle) {
        this.plugin = plugin;
        this.handle = handle;
        this.id = PumpkinAdapter.uuid(handle.getId());
        this.name = handle.getName();
    }

    void replace(Player replacement) {
        if (replacement != handle) {
            handle.close();
            handle = replacement;
        }
    }

    void disconnect() {
        online = false;
        handle.close();
    }

    Player handle() {
        return handle;
    }

    @Override
    public UUID getUniqueId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public World getWorld() {
        return plugin.world(handle.getWorld());
    }

    @Override
    public Location getLocation() {
        return new Location(getWorld(), PumpkinAdapter.vector(handle.getPosition()), handle.getYaw(), handle.getPitch());
    }

    @Override
    public boolean setLocation(Location location) {
        if (!(location.getExtent() instanceof PumpkinWorld world)) {
            return false;
        }

        Option<pumpkin.world.World> target = plugin.openWorld(world.getName());
        if (!target.isSome()) {
            return false;
        }

        handle.teleport(PumpkinAdapter.position(location.toVector()), Option.some(location.getYaw()), Option.some(location.getPitch()), target.value());
        return true;
    }

    @Override
    public boolean trySetPosition(Vector3 pos, float pitch, float yaw) {
        return setLocation(new Location(getWorld(), pos, yaw, pitch));
    }

    @Override
    public BaseItemStack getItemInHand(HandSide handSide) {
        Option<ItemStack> held = handle.getItemInHand(handSide == HandSide.MAIN_HAND ? Hand.RIGHT : Hand.LEFT);
        if (!held.isSome()) {
            return new BaseItemStack(ItemTypes.AIR, 0);
        }

        try (ItemStack stack = held.value()) {
            String key = PumpkinAdapter.key(stack.getRegistryKey());
            ItemType type = ItemType.REGISTRY.get(key);
            if (type == null) {
                type = new ItemType(key);
                ItemType.REGISTRY.register(key, type);
            }

            return new BaseItemStack(type, stack.getCount());
        }
    }

    @Override
    public void giveItem(BaseItemStack itemStack) {
        String key = itemStack.getType().id();
        int remaining = itemStack.getAmount();

        for (int slot = 0; slot < INVENTORY_SLOTS && remaining > 0; slot++) {
            Option<ItemStack> existing = handle.getInventoryItem(slot);
            if (existing.isSome()) {
                existing.value().close();
                continue;
            }

            ItemStack stack = ItemStack.create(key, 1);
            if (!PumpkinAdapter.key(stack.getRegistryKey()).equals(key)) {
                stack.close();
                return;
            }

            int amount = Math.min(remaining, stack.getMaxCount());
            stack.setCount(amount);
            handle.setInventoryItem(slot, Option.some(stack));
            remaining -= amount;
        }
    }

    @Nullable
    @Override
    public BlockBag getInventoryBlockBag() {
        return new PumpkinBlockBag(this);
    }

    @Override
    public GameMode getGameMode() {
        return GameModes.get(handle.getGamemode().name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        for (pumpkin.common.GameMode mode : pumpkin.common.GameMode.values()) {
            if (mode.name().toLowerCase(Locale.ROOT).equals(gameMode.id())) {
                handle.setGamemode(mode);
                return;
            }
        }
    }

    @Override
    @Deprecated
    public void printRaw(String message) {
        for (String part : message.split("\n")) {
            send(TextComponent.of(part));
        }
    }

    @Override
    @Deprecated
    public void printDebug(String message) {
        for (String part : message.split("\n")) {
            send(TextComponent.of(part, TextColor.GRAY));
        }
    }

    @Override
    @Deprecated
    public void print(String message) {
        for (String part : message.split("\n")) {
            send(TextComponent.of(part, TextColor.LIGHT_PURPLE));
        }
    }

    @Override
    @Deprecated
    public void printError(String message) {
        for (String part : message.split("\n")) {
            send(TextComponent.of(part, TextColor.RED));
        }
    }

    @Override
    public void print(Component component) {
        send(component);
    }

    private void send(Component component) {
        if (!online) {
            return;
        }

        handle.sendSystemMessage(PumpkinText.convert(component, getLocale()), false);
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public boolean hasPermission(String permission) {
        if (!online) {
            return false;
        }

        String node = PumpkinAdapter.permission(permission);
        plugin.registerPermission(node);
        return handle.hasPermission(node);
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        if (!online) {
            return;
        }

        Option<JavaPlayer> client = handle.asJava();
        if (!client.isSome()) {
            return;
        }

        String[] parameters = event.getParameters();
        String message = event.getTypeId();
        if (parameters.length > 0) {
            message = message + "|" + StringUtil.joinString(parameters, "|");
        }

        try (JavaPlayer java = client.value()) {
            java.sendCustomPayload(CUI_CHANNEL, message.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public Locale getLocale() {
        if (!online) {
            return Locale.ENGLISH;
        }

        String tag = handle.getLocale();
        int separator = tag.indexOf('_');
        if (separator < 0) {
            return Locale.of(tag);
        }

        return Locale.of(tag.substring(0, separator), tag.substring(separator + 1).toUpperCase(Locale.ROOT));
    }

    @Nullable
    @Override
    public BaseEntity getState() {
        return null;
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public SessionKey getSessionKey() {
        return new Key(plugin, id, name);
    }

    private record Key(PumpkinWorldEdit plugin, UUID id, String name) implements SessionKey {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isActive() {
            return plugin.isOnline(id);
        }

        @Override
        public boolean isPersistent() {
            return true;
        }

        @Override
        public UUID getUniqueId() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key that && id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}

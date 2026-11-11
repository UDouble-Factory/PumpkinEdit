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

import com.sk89q.worldedit.extension.platform.AbstractNonPlayerActor;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import pumpkin.command.CommandSender;

import java.util.Locale;
import java.util.UUID;

final class PumpkinCommandSender extends AbstractNonPlayerActor {
    private static final UUID CONSOLE_ID = new UUID(0L, 0x776f726c64656474L);

    private final CommandSender handle;
    private final String name;

    PumpkinCommandSender(CommandSender handle) {
        this.handle = handle;
        this.name = handle.getName();
    }

    @Override
    public UUID getUniqueId() {
        return CONSOLE_ID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @Deprecated
    public void printRaw(String message) {
        for (String part : message.split("\n")) {
            print(TextComponent.of(part));
        }
    }

    @Override
    @Deprecated
    public void printDebug(String message) {
        for (String part : message.split("\n")) {
            print(TextComponent.of(part, TextColor.GRAY));
        }
    }

    @Override
    @Deprecated
    public void print(String message) {
        for (String part : message.split("\n")) {
            print(TextComponent.of(part, TextColor.LIGHT_PURPLE));
        }
    }

    @Override
    @Deprecated
    public void printError(String message) {
        for (String part : message.split("\n")) {
            print(TextComponent.of(part, TextColor.RED));
        }
    }

    @Override
    public void print(Component component) {
        handle.sendMessage(PumpkinText.convert(component, getLocale()));
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    @Override
    public void checkPermission(String permission) {
    }

    @Override
    public Locale getLocale() {
        return Locale.ENGLISH;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKey() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public boolean isPersistent() {
                return false;
            }

            @Override
            public UUID getUniqueId() {
                return CONSOLE_ID;
            }
        };
    }
}

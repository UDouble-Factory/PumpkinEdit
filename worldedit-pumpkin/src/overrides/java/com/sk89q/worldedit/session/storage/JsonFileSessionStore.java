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

package com.sk89q.worldedit.session.storage;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.NavigationWand;
import com.sk89q.worldedit.command.tool.SelectionWand;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.regions.selector.RegionSelectorType;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

public class JsonFileSessionStore implements SessionStore {
    private final File dir;

    public JsonFileSessionStore(File dir) {
        this.dir = dir;
        dir.mkdirs();
    }

    public JsonFileSessionStore(Path dir) {
        this(dir.toFile());
    }

    private File file(UUID id) {
        return new File(dir, id + ".json");
    }

    @Override
    public LocalSession load(UUID id) throws IOException {
        LocalSession session = new LocalSession();
        session.setReorderMode(EditSession.ReorderMode.NONE);
        session.setSideEffectSet(session.getSideEffectSet().with(SideEffect.NEIGHBORS, SideEffect.State.OFF));

        File file = file(id);
        if (!file.exists()) {
            return session;
        }

        String wandItem = null;
        String navWandItem = null;
        boolean wandItemDefault = true;
        boolean navWandItemDefault = true;

        try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            reader.beginObject();

            while (reader.hasNext()) {
                String name = reader.nextName();
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    continue;
                }

                switch (name) {
                    case "lastScript" -> session.setLastScript(reader.nextString());
                    case "defaultSelector" -> applySelector(session, reader.nextString());
                    case "useServerCUI" -> session.setUseServerCUI(reader.nextBoolean());
                    case "wandItem" -> wandItem = reader.nextString();
                    case "wandItemDefault" -> wandItemDefault = reader.nextBoolean();
                    case "navWandItem" -> navWandItem = reader.nextString();
                    case "navWandItemDefault" -> navWandItemDefault = reader.nextBoolean();
                    default -> reader.skipValue();
                }
            }

            reader.endObject();
        } catch (IllegalStateException | NumberFormatException e) {
            throw new IOException(e);
        }

        session.setConfiguration(WorldEdit.getInstance().getConfiguration());
        if (!wandItemDefault) {
            applyTool(session, wandItem, new SelectionWand());
        }
        if (!navWandItemDefault) {
            applyTool(session, navWandItem, new NavigationWand());
        }

        return session;
    }

    private static void applySelector(LocalSession session, String name) {
        for (RegionSelectorType type : RegionSelectorType.values()) {
            if (type.name().equals(name)) {
                session.setDefaultRegionSelector(type);
                return;
            }
        }
    }

    private static void applyTool(LocalSession session, String id, Tool tool) {
        ItemType item = id == null ? null : ItemTypes.get(id);
        if (item == null || item.hasBlockType()) {
            return;
        }

        try {
            session.setTool(item, tool);
        } catch (InvalidToolBindException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(UUID id, LocalSession session) throws IOException {
        dir.mkdirs();

        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(file(id)), StandardCharsets.UTF_8))) {
            writer.beginObject();

            if (session.getLastScript() != null) {
                writer.name("lastScript").value(session.getLastScript());
            }
            if (session.getDefaultRegionSelector() != null) {
                writer.name("defaultSelector").value(session.getDefaultRegionSelector().name());
            }

            writer.name("useServerCUI").value(session.shouldUseServerCUI());

            if (session.getWandItem() != null) {
                writer.name("wandItem").value(session.getWandItem());
            }

            writer.name("wandItemDefault").value(session.isWandItemDefault());

            if (session.getNavWandItem() != null) {
                writer.name("navWandItem").value(session.getNavWandItem());
            }

            writer.name("navWandItemDefault").value(session.isNavWandItemDefault());
            writer.endObject();
        }
    }
}

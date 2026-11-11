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

import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.translation.TranslationManager;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import pumpkin.runtime.Option;
import pumpkin.runtime.Tuple2;
import pumpkin.world.Block;
import pumpkin.world.WorldApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import javax.annotation.Nullable;

final class PumpkinBlockRegistry implements BlockRegistry {
    private static final Set<String> DIRECTIONS = Set.of("north", "east", "south", "west", "up", "down");

    private final Map<String, Layout> layouts = new HashMap<>();
    private final Map<String, BlockMaterial> materials = new HashMap<>();

    private static final class Layout {
        private int base;
        private int defaultState;
        private String[] names = new String[0];
        private int[] strides = new int[0];
        private List<List<String>> values = new ArrayList<>();
        private Map<String, Property<?>> properties = new LinkedHashMap<>();
    }

    @Override
    public Component getRichName(BlockType blockType) {
        return TranslatableComponent.of(TranslationManager.makeTranslationKey("block", blockType.id()));
    }

    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        return materials.computeIfAbsent(blockType.id(), id -> {
            Option<Block> block = WorldApi.getBlockByName(id);
            if (!block.isSome()) {
                return null;
            }

            return new PumpkinBlockMaterial(block.value(), WorldApi.getDefaultStateFromBlock(block.value()));
        });
    }

    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        return layout(blockType.id()).properties;
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockState state) {
        Layout layout = layout(state.getBlockType().id());
        if (layout.base < 0) {
            return OptionalInt.empty();
        }

        int id = layout.base;

        for (int i = 0; i < layout.names.length; i++) {
            if (layout.strides[i] == 0) {
                continue;
            }

            Property<?> property = layout.properties.get(layout.names[i]);
            int index = property.values().indexOf(state.getState(property));
            if (index < 0) {
                return OptionalInt.empty();
            }

            id += index * layout.strides[i];
        }

        return OptionalInt.of(id);
    }

    Map<String, String> defaultProperties(String id) {
        Layout layout = layout(id);
        Map<String, String> result = new LinkedHashMap<>();
        if (layout.base < 0) {
            return result;
        }

        for (Tuple2<String, String> entry : WorldApi.getBlockProperties(layout.defaultState)) {
            result.put(entry.f0(), entry.f1());
        }

        return result;
    }

    private Layout layout(String id) {
        Layout cached = layouts.get(id);
        if (cached != null) {
            return cached;
        }

        Layout layout = new Layout();
        layouts.put(id, layout);

        Option<Block> found = WorldApi.getBlockByName(id);
        if (!found.isSome()) {
            layout.base = -1;
            return layout;
        }

        Block block = found.value();
        int base = Integer.MAX_VALUE;
        for (int stateId : block.stateIds()) {
            base = Math.min(base, stateId);
        }

        int total = block.stateIds().size();
        layout.base = base;
        layout.defaultState = block.defaultStateId();

        List<Tuple2<String, String>> first = WorldApi.getBlockProperties(base);
        int count = first.size();
        layout.names = new String[count];
        layout.strides = new int[count];

        for (int i = 0; i < count; i++) {
            layout.names[i] = first.get(i).f0();
            List<String> values = new ArrayList<>();
            values.add(first.get(i).f1());
            layout.values.add(values);
        }

        boolean[] resolved = new boolean[count];
        int stride = 1;

        while (stride < total) {
            List<Tuple2<String, String>> sample = WorldApi.getBlockProperties(base + stride);
            int index = -1;

            for (int i = 0; i < count; i++) {
                if (!resolved[i] && !sample.get(i).f1().equals(first.get(i).f1())) {
                    index = i;
                    break;
                }
            }

            if (index < 0) {
                break;
            }

            List<String> values = layout.values.get(index);
            values.add(sample.get(index).f1());

            for (int step = 2; step * stride < total; step++) {
                String value = WorldApi.getBlockProperties(base + step * stride).get(index).f1();
                if (value.equals(values.get(0))) {
                    break;
                }

                values.add(value);
            }

            layout.strides[index] = stride;
            resolved[index] = true;
            stride *= values.size();
        }

        for (int i = 0; i < count; i++) {
            layout.properties.put(layout.names[i], createProperty(layout.names[i], layout.values.get(i)));
        }

        return layout;
    }

    private static Property<?> createProperty(String name, List<String> values) {
        boolean booleans = true;
        boolean integers = true;
        boolean directions = true;

        for (String value : values) {
            booleans &= value.equals("true") || value.equals("false");
            integers &= isInteger(value);
            directions &= DIRECTIONS.contains(value);
        }

        if (booleans) {
            List<Boolean> converted = new ArrayList<>();
            for (String value : values) {
                converted.add(Boolean.valueOf(value));
            }
            return new BooleanProperty(name, converted);
        }

        if (integers) {
            List<Integer> converted = new ArrayList<>();
            for (String value : values) {
                converted.add(Integer.valueOf(value));
            }
            return new IntegerProperty(name, converted);
        }

        if (directions) {
            List<Direction> converted = new ArrayList<>();
            for (String value : values) {
                converted.add(Direction.valueOf(value.toUpperCase(Locale.ROOT)));
            }
            return new DirectionalProperty(name, converted);
        }

        return new EnumProperty(name, new ArrayList<>(values));
    }

    private static boolean isInteger(String value) {
        if (value.isEmpty()) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }
}

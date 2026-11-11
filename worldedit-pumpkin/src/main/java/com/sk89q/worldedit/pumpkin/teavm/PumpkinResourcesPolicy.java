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

package com.sk89q.worldedit.pumpkin.teavm;

import org.teavm.extension.spi.resources.ResourcesPolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PumpkinResourcesPolicy implements ResourcesPolicy {
    private static final String LOCALES = "pumpkin/locales.txt";

    @Override
    public String[] supplyResources(Collection<? extends String> availableClassNames) {
        List<String> resources = new ArrayList<>(List.of(
            "lang/strings.json",
            "pumpkin/items.txt",
            "pumpkin/block-categories.txt",
            "pumpkin/item-categories.txt"
        ));

        try (InputStream in = PumpkinResourcesPolicy.class.getClassLoader().getResourceAsStream(LOCALES)) {
            if (in == null) {
                throw new IllegalStateException(LOCALES + " is missing from the build");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String locale;

            while ((locale = reader.readLine()) != null) {
                if (!locale.isEmpty()) {
                    resources.add("lang/" + locale + "/strings.json");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return resources.toArray(new String[0]);
    }
}

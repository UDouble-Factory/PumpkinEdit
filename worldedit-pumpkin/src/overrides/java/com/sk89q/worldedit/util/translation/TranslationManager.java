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

package com.sk89q.worldedit.util.translation;

import com.google.gson.stream.JsonReader;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.renderer.TranslatableComponentRenderer;
import com.sk89q.worldedit.util.io.ResourceLoader;
import com.sk89q.worldedit.util.io.file.ArchiveUnpacker;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class TranslationManager {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final String DEFAULT_STRINGS = "lang/strings.json";

    public static String makeTranslationKey(String type, String id) {
        String[] parts = id.split(":", 2);
        return type + '.' + parts[0] + '.' + parts[1].replace('/', '.');
    }

    private final TranslatableComponentRenderer<Locale> friendlyComponentRenderer = TranslatableComponentRenderer.from(this::getTranslation);
    private final Map<Locale, Map<String, MessageFormat>> translations = new HashMap<>();
    private final Set<Locale> loadedLocales = new HashSet<>();
    private final List<Consumer<Locale>> localeChangeListeners = new ArrayList<>();
    private final Path userProvidedFlatRoot;
    private Locale defaultLocale = Locale.ENGLISH;

    public TranslationManager(ArchiveUnpacker archiveUnpacker, ResourceLoader resourceLoader) throws IOException {
        this.userProvidedFlatRoot = resourceLoader.getLocalResource("lang");
    }

    public void reload() {
        loadedLocales.clear();
        translations.clear();
    }

    public void addLocaleChangeListener(Consumer<Locale> listener, boolean includePreviouslyLoaded) {
        localeChangeListeners.add(listener);

        if (includePreviouslyLoaded) {
            for (Locale locale : new ArrayList<>(loadedLocales)) {
                listener.accept(locale);
            }
        }
    }

    public Component convertText(Component component, Locale locale) {
        return friendlyComponentRenderer.render(component, locale);
    }

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    private MessageFormat getTranslation(Locale locale, String key) {
        if (!loadedLocales.contains(locale)) {
            loadLocale(locale);
        }

        Map<String, MessageFormat> table = translations.get(locale);
        MessageFormat format = table == null ? null : table.get(key);

        if (format == null && !defaultLocale.equals(locale)) {
            if (!locale.getCountry().isEmpty()) {
                return getTranslation(Locale.of(locale.getLanguage()), key);
            }
            return getTranslation(defaultLocale, key);
        }

        return format;
    }

    private void loadLocale(Locale locale) {
        loadedLocales.add(locale);

        try {
            loadTranslations(locale);
        } catch (Exception e) {
            LOGGER.warn("Failed to load translations, locale=" + locale, e);
        }
    }

    private void loadTranslations(Locale locale) throws IOException {
        Map<String, String> entries = new HashMap<>();

        String bundled = defaultLocale.equals(locale) ? DEFAULT_STRINGS : "lang/" + getLocalePath(locale);
        try (InputStream in = TranslationManager.class.getClassLoader().getResourceAsStream(bundled)) {
            if (in != null) {
                putTranslationData(entries, in);
            }
        }

        Path userFile = userProvidedFlatRoot == null ? null : userProvidedFlatRoot.resolve(getLocalePath(locale));
        if (userFile != null && Files.exists(userFile)) {
            try (InputStream in = Files.newInputStream(userFile)) {
                putTranslationData(entries, in);
            }
        }

        Map<String, MessageFormat> table = new HashMap<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            try {
                table.put(entry.getKey(), new MessageFormat(entry.getValue().replace("'", "''"), locale));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Failed to load translation, locale=" + locale + ", key=" + entry.getKey(), e);
            }
        }
        translations.put(locale, table);

        for (Consumer<Locale> listener : new ArrayList<>(localeChangeListeners)) {
            listener.accept(locale);
        }
    }

    private String getLocalePath(Locale locale) {
        if (defaultLocale.equals(locale)) {
            return "strings.json";
        }

        String country = locale.getCountry().isEmpty() ? "" : "-" + locale.getCountry();
        return locale.getLanguage() + country + "/strings.json";
    }

    private void putTranslationData(Map<String, String> data, InputStream inputStream) throws IOException {
        try (JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            reader.beginObject();

            while (reader.hasNext()) {
                String key = reader.nextName();
                String value = reader.nextString();
                if (!value.isEmpty()) {
                    data.put(key, value);
                }
            }

            reader.endObject();
        }
    }
}

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

import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.KeybindComponent;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.formatting.text.format.TextDecoration;
import pumpkin.common.NamedColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PumpkinText {
    private PumpkinText() {
    }

    static pumpkin.text.TextComponent convert(Component component, Locale locale) {
        return build(WorldEditText.format(component, locale));
    }

    static pumpkin.text.TextComponent plain(String text) {
        return pumpkin.text.TextComponent.text(text);
    }

    private static pumpkin.text.TextComponent build(Component component) {
        pumpkin.text.TextComponent result;

        if (component instanceof TextComponent text) {
            result = pumpkin.text.TextComponent.text(text.content());
        } else if (component instanceof TranslatableComponent translatable) {
            List<pumpkin.text.TextComponent> arguments = new ArrayList<>();
            for (Component argument : translatable.args()) {
                arguments.add(build(argument));
            }

            result = pumpkin.text.TextComponent.translate(translatable.key(), arguments);
        } else if (component instanceof KeybindComponent keybind) {
            result = pumpkin.text.TextComponent.keybind(keybind.keybind());
        } else {
            result = pumpkin.text.TextComponent.text("");
        }

        applyStyle(component, result);

        for (Component child : component.children()) {
            result.addChild(build(child));
        }

        return result;
    }

    private static void applyStyle(Component component, pumpkin.text.TextComponent result) {
        TextColor color = component.color();
        if (color != null) {
            result.colorNamed(NamedColor.valueOf(color.name()));
        }

        if (component.hasDecoration(TextDecoration.BOLD)) {
            result.bold(true);
        }
        if (component.hasDecoration(TextDecoration.ITALIC)) {
            result.italic(true);
        }
        if (component.hasDecoration(TextDecoration.UNDERLINED)) {
            result.underlined(true);
        }
        if (component.hasDecoration(TextDecoration.STRIKETHROUGH)) {
            result.strikethrough(true);
        }
        if (component.hasDecoration(TextDecoration.OBFUSCATED)) {
            result.obfuscated(true);
        }

        String insertion = component.insertion();
        if (insertion != null) {
            result.insertion(insertion);
        }

        ClickEvent click = component.clickEvent();
        if (click != null) {
            applyClick(click, result);
        }

        HoverEvent hover = component.hoverEvent();
        if (hover != null && hover.action() == HoverEvent.Action.SHOW_TEXT) {
            result.hoverShowText(build(hover.value()));
        }
    }

    private static void applyClick(ClickEvent click, pumpkin.text.TextComponent result) {
        String value = click.value();

        switch (click.action()) {
            case OPEN_URL -> result.clickOpenUrl(value);
            case RUN_COMMAND -> result.clickRunCommand(value);
            case SUGGEST_COMMAND -> result.clickSuggestCommand(value);
            case COPY_TO_CLIPBOARD -> result.clickCopyToClipboard(value);
            case CHANGE_PAGE -> {
                try {
                    result.clickChangePage(Long.parseLong(value));
                } catch (NumberFormatException ignored) {
                    result.clickSuggestCommand(value);
                }
            }
            default -> {
            }
        }
    }
}

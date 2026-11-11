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

package com.sk89q.worldedit.session.request;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.world.World;

import java.util.function.Consumer;
import javax.annotation.Nullable;

public final class Request {

    private static Request current;

    private @Nullable World world;
    private @Nullable LocalSession session;
    private @Nullable EditSession editSession;
    private boolean valid;

    private Request() {
    }

    public @Nullable World getWorld() {
        return world;
    }

    public void setWorld(@Nullable World world) {
        this.world = world;
    }

    public @Nullable LocalSession getSession() {
        return session;
    }

    public void setSession(@Nullable LocalSession session) {
        this.session = session;
    }

    public @Nullable EditSession getEditSession() {
        return editSession;
    }

    public void setEditSession(@Nullable EditSession editSession) {
        this.editSession = editSession;
    }

    public static Request request() {
        if (current == null) {
            throw new java.util.NoSuchElementException("No request is active");
        }

        return current;
    }

    public static void applyIfPresent(Consumer<Request> func) {
        if (current != null) {
            func.accept(current);
        }
    }

    public static void runWithRequest(Runnable runnable) {
        Request previous = current;
        Request request = new Request();
        request.valid = true;
        current = request;

        try {
            runnable.run();
        } finally {
            current = previous;
            request.invalidate();
        }
    }

    @Deprecated(forRemoval = true)
    public static void reset() {
        request().invalidate();
    }

    public boolean isValid() {
        return valid;
    }

    private void invalidate() {
        valid = false;
    }
}

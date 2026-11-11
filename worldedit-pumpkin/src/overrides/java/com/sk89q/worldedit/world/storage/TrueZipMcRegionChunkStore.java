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

package com.sk89q.worldedit.world.storage;

import com.sk89q.worldedit.world.DataException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TrueZipMcRegionChunkStore extends McRegionChunkStore {

    public TrueZipMcRegionChunkStore(File zipFile, String folder) throws IOException {
        throw new IOException("Archived backups are not available on this server.");
    }

    public TrueZipMcRegionChunkStore(File zipFile) throws IOException {
        throw new IOException("Archived backups are not available on this server.");
    }

    @Override
    protected InputStream getInputStream(String name, String worldName) throws IOException, DataException {
        throw new IOException("Archived backups are not available on this server.");
    }

    @Override
    public boolean isValid() {
        return false;
    }
}

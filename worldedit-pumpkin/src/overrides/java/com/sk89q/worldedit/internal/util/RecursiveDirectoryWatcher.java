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

package com.sk89q.worldedit.internal.util;

import com.sk89q.worldedit.util.io.file.FilenameException;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class RecursiveDirectoryWatcher implements Closeable {

    public interface DirEntryChangeEvent {
        Path path();
    }

    public record FileCreatedEvent(Path path) implements DirEntryChangeEvent {
    }

    public record FileDeletedEvent(Path path) implements DirEntryChangeEvent {
    }

    public record DirectoryCreatedEvent(Path path) implements DirEntryChangeEvent {
    }

    public record DirectoryDeletedEvent(Path path) implements DirEntryChangeEvent {
    }

    private RecursiveDirectoryWatcher() {
    }

    public static RecursiveDirectoryWatcher create(Path root) throws IOException {
        throw new IOException("Directory watching is not available");
    }

    public static void logInvalidFileName(Logger logger, FilenameException e) {
        logger.warn("Invalid file name detected: {} - {}", e.getMessage(), e.getFilename());
    }

    public void start(Consumer<DirEntryChangeEvent> eventConsumer) {
    }

    @Override
    public void close() {
    }
}

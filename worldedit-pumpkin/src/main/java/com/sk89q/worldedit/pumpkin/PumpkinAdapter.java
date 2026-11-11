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

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import org.enginehub.linbus.stream.LinBinaryIO;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinRootEntry;
import pumpkin.common.BlockPos;
import pumpkin.common.Position;
import pumpkin.uuid.Uuid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

final class PumpkinAdapter {
    private PumpkinAdapter() {
    }

    static UUID uuid(Uuid id) {
        return new UUID(id.high(), id.low());
    }

    static Uuid uuid(UUID id) {
        return new Uuid(id.getMostSignificantBits(), id.getLeastSignificantBits());
    }

    static BlockPos blockPos(BlockVector3 position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    static Position position(Vector3 position) {
        return new Position(position.x(), position.y(), position.z());
    }

    static Vector3 vector(Position position) {
        return Vector3.at(position.f0(), position.f1(), position.f2());
    }

    static String key(String name) {
        return name.indexOf(':') < 0 ? "minecraft:" + name : name;
    }

    static String enumKey(Enum<?> value) {
        return "minecraft:" + value.name().toLowerCase(Locale.ROOT);
    }

    static String permission(String node) {
        if (node.startsWith("worldedit.")) {
            return "worldedit:" + node.substring("worldedit.".length());
        }

        return "worldedit:" + node;
    }

    static LinCompoundTag readNbt(byte[] unnamed) throws IOException {
        byte[] named = new byte[unnamed.length + 2];
        named[0] = unnamed[0];
        System.arraycopy(unnamed, 1, named, 3, unnamed.length - 1);

        LinRootEntry root = LinBinaryIO.readUsing(new DataInputStream(new ByteArrayInputStream(named)), LinRootEntry::readFrom);
        return root.value();
    }

    static byte[] writeNbt(LinCompoundTag tag) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        LinBinaryIO.write(new DataOutputStream(bytes), new LinRootEntry("", tag));

        byte[] named = bytes.toByteArray();
        byte[] unnamed = new byte[named.length - 2];
        unnamed[0] = named[0];
        System.arraycopy(named, 3, unnamed, 1, named.length - 3);
        return unnamed;
    }
}

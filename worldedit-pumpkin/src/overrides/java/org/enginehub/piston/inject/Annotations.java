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

package org.enginehub.piston.inject;

import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.MultiDirection;
import com.sk89q.worldedit.internal.annotation.Radii;

import java.lang.annotation.Annotation;
import javax.annotation.Nullable;

class Annotations {

    private Annotations() {
    }

    static Annotation allDefaultsAnnotation(Class<? extends Annotation> annotationType) {
        if (annotationType == Direction.class) {
            return new DirectionValue(false);
        }
        if (annotationType == MultiDirection.class) {
            return new MultiDirectionValue(false);
        }

        throw new IllegalStateException("No default members for " + annotationType);
    }

    static boolean hasAllDefaults(Class<? extends Annotation> annotationType) {
        return annotationType == Direction.class || annotationType == MultiDirection.class;
    }

    @Nullable
    static Annotation normalize(Annotation annotation) {
        if (annotation instanceof Direction direction) {
            return new DirectionValue(direction.includeDiagonals());
        }
        if (annotation instanceof MultiDirection direction) {
            return new MultiDirectionValue(direction.includeDiagonals());
        }
        if (annotation instanceof Radii radii) {
            return new RadiiValue(radii.value());
        }

        return null;
    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static final class DirectionValue implements Direction {
        private final boolean includeDiagonals;

        private DirectionValue(boolean includeDiagonals) {
            this.includeDiagonals = includeDiagonals;
        }

        @Override
        public boolean includeDiagonals() {
            return includeDiagonals;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Direction.class;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Direction that && includeDiagonals == that.includeDiagonals();
        }

        @Override
        public int hashCode() {
            return (127 * "includeDiagonals".hashCode()) ^ Boolean.hashCode(includeDiagonals);
        }

        @Override
        public String toString() {
            return "@Direction(includeDiagonals=" + includeDiagonals + ")";
        }
    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static final class MultiDirectionValue implements MultiDirection {
        private final boolean includeDiagonals;

        private MultiDirectionValue(boolean includeDiagonals) {
            this.includeDiagonals = includeDiagonals;
        }

        @Override
        public boolean includeDiagonals() {
            return includeDiagonals;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return MultiDirection.class;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MultiDirection that && includeDiagonals == that.includeDiagonals();
        }

        @Override
        public int hashCode() {
            return (127 * "includeDiagonals".hashCode()) ^ Boolean.hashCode(includeDiagonals);
        }

        @Override
        public String toString() {
            return "@MultiDirection(includeDiagonals=" + includeDiagonals + ")";
        }
    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static final class RadiiValue implements Radii {
        private final int value;

        private RadiiValue(int value) {
            this.value = value;
        }

        @Override
        public int value() {
            return value;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Radii.class;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Radii that && value == that.value();
        }

        @Override
        public int hashCode() {
            return (127 * "value".hashCode()) ^ Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return "@Radii(" + value + ")";
        }
    }
}

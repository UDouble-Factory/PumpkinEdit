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

import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import com.google.common.reflect.TypeToken;

import java.lang.annotation.Annotation;
import javax.annotation.Nullable;

@AutoValue
public abstract class Key<T> {

    public static <T> Key<T> of(Class<T> clazz) {
        return of(TypeToken.of(clazz));
    }

    public static <T> Key<T> of(TypeToken<T> typeToken) {
        return of(typeToken, NullAnnotationWrapper.getInstance());
    }

    public static <T> Key<T> of(Class<T> clazz, @Nullable Annotation annotation) {
        return of(TypeToken.of(clazz), annotation);
    }

    public static <T> Key<T> of(Class<T> clazz, @Nullable Class<? extends Annotation> annotation) {
        return of(TypeToken.of(clazz), annotation);
    }

    public static <T> Key<T> of(TypeToken<T> typeToken, @Nullable Annotation annotation) {
        return of(typeToken, strategyFor(annotation));
    }

    public static <T> Key<T> of(TypeToken<T> typeToken, @Nullable Class<? extends Annotation> annotation) {
        return of(typeToken, strategyFor(annotation));
    }

    private static <T> Key<T> of(TypeToken<T> typeToken, AnnotationWrapper annotationWrapper) {
        return new AutoValue_Key<>(typeToken.wrap(), annotationWrapper);
    }

    private static AnnotationWrapper strategyFor(@Nullable Annotation annotation) {
        if (annotation == null) {
            return NullAnnotationWrapper.getInstance();
        }

        Annotation normalized = Annotations.normalize(annotation);
        if (normalized == null) {
            return TypeAnnotationWrapper.from(annotation.annotationType());
        }

        return InstanceAnnotationWrapper.from(normalized);
    }

    private static AnnotationWrapper strategyFor(@Nullable Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return NullAnnotationWrapper.getInstance();
        }

        if (Annotations.hasAllDefaults(annotationType)) {
            return strategyFor(Annotations.allDefaultsAnnotation(annotationType));
        }

        return TypeAnnotationWrapper.from(annotationType);
    }

    public abstract TypeToken<T> getTypeToken();

    abstract AnnotationWrapper getAnnotationWrapper();

    @Nullable
    public final Annotation getAnnotation() {
        return getAnnotationWrapper().getAnnotation();
    }

    @Nullable
    public final Class<? extends Annotation> getAnnotationType() {
        return getAnnotationWrapper().getAnnotationType();
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(Key.class)
            .add("type", getTypeToken())
            .add("annotationWrapper", getAnnotationWrapper())
            .toString();
    }
}

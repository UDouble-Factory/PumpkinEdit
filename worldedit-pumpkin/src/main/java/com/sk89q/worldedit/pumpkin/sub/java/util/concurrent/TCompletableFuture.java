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

package com.sk89q.worldedit.pumpkin.sub.java.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class TCompletableFuture<T> implements Future<T> {
    private boolean done;
    private boolean cancelled;
    private T value;
    private Throwable failure;

    public TCompletableFuture() {
    }

    public static <U> TCompletableFuture<U> completedFuture(U value) {
        TCompletableFuture<U> future = new TCompletableFuture<>();
        future.complete(value);
        return future;
    }

    public static <U> TCompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        TCompletableFuture<U> future = new TCompletableFuture<>();
        future.run(supplier);
        return future;
    }

    public static <U> TCompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        TCompletableFuture<U> future = new TCompletableFuture<>();
        executor.execute(() -> future.run(supplier));
        return future;
    }

    private void run(Supplier<T> supplier) {
        try {
            complete(supplier.get());
        } catch (Throwable t) {
            completeExceptionally(t);
        }
    }

    public boolean complete(T result) {
        if (done) {
            return false;
        }

        value = result;
        done = true;
        return true;
    }

    public boolean completeExceptionally(Throwable throwable) {
        if (done) {
            return false;
        }

        failure = throwable;
        done = true;
        return true;
    }

    public TCompletableFuture<T> exceptionally(Function<Throwable, ? extends T> handler) {
        TCompletableFuture<T> future = new TCompletableFuture<>();

        if (!done) {
            return future;
        }

        if (failure != null) {
            future.complete(handler.apply(failure));
        } else {
            future.complete(value);
        }

        return future;
    }

    public T join() {
        if (!done) {
            throw new IllegalStateException("Blocking waits are not supported");
        }
        if (cancelled) {
            throw new CancellationException();
        }
        if (failure != null) {
            throw new RuntimeException(failure);
        }

        return value;
    }

    @Override
    public T get() throws ExecutionException {
        if (!done) {
            throw new IllegalStateException("Blocking waits are not supported");
        }
        if (cancelled) {
            throw new CancellationException();
        }
        if (failure != null) {
            throw new ExecutionException(failure);
        }

        return value;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException {
        return get();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (done) {
            return false;
        }

        cancelled = true;
        done = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}

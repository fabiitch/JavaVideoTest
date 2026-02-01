package com.nz.media.threading;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MediaThread implements AutoCloseable {

    @FunctionalInterface
    public interface Command {
        void run() throws Exception;
    }

    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final MediaFailureListener failureListener;

    private volatile Throwable failure;

    public MediaThread(String name, MediaFailureListener failureListener) {
        this.failureListener = Objects.requireNonNull(failureListener, "failureHandler");

        ThreadFactory tf = r -> Thread.ofPlatform()
            .name((name == null || name.isBlank()) ? "media-thread" : name)
            .unstarted(r);

        this.executor = Executors.newSingleThreadExecutor(tf);
    }

    public void start() {
        // optionnel : warmup
        post(() -> {});
    }

    public void post(Command cmd) {
        ensureRunning();
        executor.execute(() -> {
            try {
                cmd.run();
            } catch (Throwable t) {
                handleFatal(t);
            }
        });
    }

    public <T> Future<T> submit(Callable<T> call) {
        ensureRunning();
        return executor.submit(() -> {
            try {
                return call.call();
            } catch (Throwable t) {
                handleFatal(t);
                throw t;
            }
        });
    }

    private void handleFatal(Throwable t) {
        failure = t;
        try { failureListener.onFailure(t); } catch (Throwable ignored) {}
        stop();
    }

    private void ensureRunning() {
        if (!running.get()) throw new IllegalStateException("MediaThread is stopped");
    }

    public Throwable failure() { return failure; }

    public void stop() {
        if (!running.getAndSet(false)) return;
        executor.shutdownNow();
    }

    @Override public void close() { stop(); }
}

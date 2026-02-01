package com.nz.media.player;

import com.nz.media.backend.BackendCommand;
import com.nz.media.backend.VideoBackend;
import com.nz.media.clock.MediaClock;
import com.nz.media.event.*;
import com.nz.media.frame.VideoFrame;
import com.nz.media.threading.MediaThread;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class VideoPlayerCore implements VideoPlayer, AutoCloseable {

    private static final long DEFAULT_TTL_MS = 2500;
    private static final long PLAY_PAUSE_TTL_MS = 5000;
    private static final long VOLUME_TTL_MS = 8000;

    // Avoid spamming the media thread from render thread
    private static final long FLUSH_REQUEST_MIN_INTERVAL_NS = TimeUnit.MILLISECONDS.toNanos(50);

    private final VideoBackend backend;
    private final VideoEventBus bus;
    private final MediaThread mediaThread;
    private final MediaClock clock = new MediaClock();

    private final AtomicBoolean opened = new AtomicBoolean(false);

    // Pending commands are owned by the media thread (single-threaded access)
    private final PlayerCommandQueue queue = new PlayerCommandQueue();
    private final AtomicLong lastFlushRequestNs = new AtomicLong(0);

    public VideoPlayerCore(VideoBackend backend, VideoEventBus bus) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.bus = Objects.requireNonNull(bus, "bus");

        // Media thread is low-level; it reports fatal failures via listener,
        // and the core translates that into VideoEvents.
        this.mediaThread = new MediaThread("media-thread", err -> {
            this.bus.publish(new BackendError(System.nanoTime(), err));
            this.bus.publish(new PlaybackStateChanged(System.nanoTime(), PlaybackState.FAILED));
            opened.set(false);
            clock.stop();
            // pending is owned by media thread; safe to clear here because listener is
            // called on media thread
            queue.clear();
        });

        this.mediaThread.start();
    }

    // Backward-compatible ctor if you want it (optional)
    public VideoPlayerCore(VideoBackend backend) {
        this(backend, new VideoEventBus());
    }


    @Override
    public <T extends VideoEvent> VideoEventBus.Subscription on(Class<T> type, Consumer<T> consumer) {
        return bus.subscribe(type, consumer);
    }

    public VideoEventBus events() {
        return bus;
    }

    public void open(String path) {
        bus.publish(new PlaybackStateChanged(System.nanoTime(), PlaybackState.OPENING));

        mediaThread.post(() -> {
            backend.open(path);

            opened.set(true);
            clock.stop();

            // Tell listeners we are open/ready
            bus.publish(new BackendOpened(System.nanoTime(), path));
            bus.publish(new PlaybackStateChanged(System.nanoTime(), PlaybackState.READY));

            flushPendingUnsafe();
        });
    }

    public void play() {
        enqueueOrRun(BackendCommand.PLAY, PLAY_PAUSE_TTL_MS, () -> {
            backend.play();
            clock.play();
            bus.publish(new PlaybackStateChanged(System.nanoTime(), PlaybackState.PLAYING));
        });
    }

    public void pause() {
        enqueueOrRun(BackendCommand.PAUSE, PLAY_PAUSE_TTL_MS, () -> {
            backend.pause();
            clock.pause();
            bus.publish(new PlaybackStateChanged(System.nanoTime(),
                PlaybackState.PAUSED));
        });
    }

    @Override
    public void stop() {
        enqueueOrRun(BackendCommand.STOP, PLAY_PAUSE_TTL_MS, () -> {
            backend.stop();
            clock.stop();
            bus.publish(new PlaybackStateChanged(System.nanoTime(),
                PlaybackState.STOPPED));
        });
    }

    @Override
    public void seekMs(long positionMs) {
        long ns = TimeUnit.MILLISECONDS.toNanos(positionMs);
        enqueueOrRun(BackendCommand.SEEK, DEFAULT_TTL_MS, () -> {
            backend.seekNs(ns);
            clock.seekNs(positionMs); // la clock reste en ms
            bus.publish(new PositionChanged(System.nanoTime(), positionMs));
        });
    }

    @Override
    public double getSpeed() {
        return clock.getSpeed();
    }

    public void setSpeed(double speed) {
        enqueueOrRun(BackendCommand.SET_SPEED, DEFAULT_TTL_MS, () -> {
            backend.setSpeed(speed);
            clock.setSpeed(speed);
        });
    }

    @Override
    public double getVolume() {
        return backend.getVolume();
    }

    public void setVolume(double volume) {
        enqueueOrRun(BackendCommand.SET_VOLUME, VOLUME_TTL_MS,
            () -> backend.setVolume(volume));
    }

    public void setMuted(boolean muted) {
        // Not part of BackendCommand enum, but still benefits from readiness.
        // Most backends accept this early, so no queue by default.
        mediaThread.post(() -> backend.setMuted(muted));
    }

    /**
     * Called by render thread
     */
    public VideoFrame pollFrame() {
        // If we have pending commands, ask the media thread to retry occasionally.
        requestFlushFromRenderThread();
        return backend.pollLatestFrame();
    }

    /**
     * Called by render thread
     */
    public void recycle(VideoFrame frame) {
        backend.recycle(frame);
    }

    public long getPositionMs() {
        // Option B: use clock (logical)
        return clock.nowMediaMs();
    }

    public long getDurationMs() {
        long ns = backend.getDurationNs();
        return ns <= 0 ? -1 : ns / 1_000_000L;
    }

    public boolean isPlaying() {
        return clock.isPlaying();
    }

    public void rethrowIfFailed() {
        Throwable t = mediaThread.failure();
        if (t == null)
            return;
        if (t instanceof RuntimeException re)
            throw re;
        throw new RuntimeException("MediaThread failed", t);
    }

    @Override
    public void close() {
        try {
            // 1) Close backend on the media thread (serialized), and wait a bit.
            mediaThread.submit(() -> {
                try {
                    if (opened.get()) {
                        backend.close();
                        opened.set(false);
                    }

                    queue.clear();

                    // close => nothing loaded anymore
                    bus.publish(new PlaybackStateChanged(System.nanoTime(), PlaybackState.IDLE));

                    return null; // <-- required (Callable<Void>)
                } catch (Throwable t) {
                    // surface the failure
                    bus.publish(new BackendError(System.nanoTime(), t));
                    bus.publish(new PlaybackStateChanged(System.nanoTime(), PlaybackState.FAILED));
                    throw t;
                } finally {
                    // keep it here if you have a clock in this class
                    clock.stop();
                }
            }).get(1500, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            // If we failed to close cleanly, notify once (bus still alive here)
            bus.publish(new BackendError(System.nanoTime(), e));
            bus.publish(new PlaybackStateChanged(System.nanoTime(), PlaybackState.FAILED));

        } finally {
            // 2) Hard shutdown infra
            try {
                mediaThread.close();
            } finally {
                bus.close();
            }
        }
    }

    // -----------------------
    // Pending command handling
    // -----------------------

    private void enqueueOrRun(BackendCommand cmd, long ttlMs, Runnable action) {
        mediaThread.post(() -> {
            if (!opened.get()) {
                // Not opened yet -> store
                queue.add(cmd, ttlMs, action);
                return;
            }

            if (backend.can(cmd)) {
                action.run();
                // After any successful action, try to flush more
                queue.flush(backend);
            } else {
                queue.add(cmd, ttlMs, action);
            }
        });
    }

    private void flushPendingUnsafe() {
        queue.flush(backend);
    }

    private void requestFlushFromRenderThread() {
        if (queue.isEmpty())
            return; // cheap fast-path; not strictly accurate cross-thread but fine

        long now = System.nanoTime();
        long last = lastFlushRequestNs.get();
        if (now - last < FLUSH_REQUEST_MIN_INTERVAL_NS)
            return;
        if (!lastFlushRequestNs.compareAndSet(last, now))
            return;

        mediaThread.post(this::flushPendingUnsafe);
    }
}

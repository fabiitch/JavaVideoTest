package com.nz.media.backend.gst;

import com.nz.media.backend.BackendCommand;
import com.nz.media.backend.BackendState;
import com.nz.media.backend.VideoBackend;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.event.SeekFlags;
import org.freedesktop.gstreamer.event.SeekType;

import java.io.File;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for GStreamer backends (reader/interop) that share:
 * - Gst init
 * - pipeline lifecycle
 * - bus error/eos/state
 * - seek/speed
 * - volume/mute (when an element exists)
 * - duration/position queries
 *
 * Does NOT know about frame types or appsink consumption logic.
 */
public abstract class AbstractGstBackendBase implements VideoBackend {

    protected static final EnumSet<SeekFlags> DEFAULT_SEEK_FLAGS =
        EnumSet.of(SeekFlags.FLUSH, SeekFlags.KEY_UNIT);

    protected final AtomicReference<BackendState> state = new AtomicReference<>(BackendState.NEW);

    protected Pipeline pipeline;
    protected AppSink appSink;
    protected Element volumeElement;

    protected volatile double speed = 1.0;
    protected volatile double volume = 1.0;
    protected volatile boolean muted = false;

    protected volatile long durationNs = 0L;
    protected volatile long positionNs = 0L;

    protected volatile Throwable lastError;

    @Override
    public BackendState getState() {
        return state.get();
    }

    @Override
    public boolean isOpenedForCommands() {
        return pipeline != null;
    }

    @Override
    public void open(String path) {
        state.set(BackendState.OPENING);
        lastError = null;
        openPipeline(path);
    }

    @Override
    public void close() {
        stop();
        closePipeline();
        state.set(BackendState.CLOSED);
    }

    @Override
    public void play() {
        if (pipeline == null) return;
        pipeline.setState(State.PLAYING);
        state.set(BackendState.PLAYING);
    }

    @Override
    public void pause() {
        if (pipeline == null) return;
        pipeline.setState(State.PAUSED);
        state.set(BackendState.PAUSED);
    }

    @Override
    public void stop() {
        if (pipeline == null) return;
        pipeline.setState(State.READY);
        state.set(BackendState.READY);
    }

    @Override
    public void seekNs(long ns) {
        if (pipeline == null) return;
        long target = Math.max(0, ns);
        sendSeek(target, speed, DEFAULT_SEEK_FLAGS);
    }

    @Override
    public void setSpeed(double speed) {
        this.speed = clamp(speed, 0.05, 8.0);
        if (!can(BackendCommand.SET_SPEED)) return;
        BackendState prevState = state.get();
        state.set(BackendState.SEEKING);

        long pos = getPositionNs();

        boolean ok = sendSeek(
            Math.max(0, pos),
            this.speed,
            EnumSet.of(SeekFlags.FLUSH, SeekFlags.TRICKMODE, SeekFlags.KEY_UNIT)
        );
        if (shouldLogSpeedSeek()) {
            System.out.println("seek(rate=" + this.speed + ") ok=" + ok);
        }
        if (ok) {
            pipeline.setState(State.PLAYING);
            state.set(BackendState.PLAYING);
        } else {
            state.set(prevState);
        }
    }

    @Override
    public double getVolume() {
        return volume;
    }

    @Override
    public void setVolume(double volume) {
        this.volume = clamp(volume, 0.0, 1.0);
        if (!can(BackendCommand.SET_VOLUME)) return;
        applyVolumeUnsafe();
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        if (!can(BackendCommand.SET_VOLUME)) return;
        applyVolumeUnsafe();
    }

    @Override
    public long getDurationNs() {
        if (pipeline == null) return -1L;
        refreshPositionAndDuration();
        return durationNs;
    }

    @Override
    public long getPositionNs() {
        if (pipeline == null) return 0L;
        refreshPositionAndDuration();
        return positionNs;
    }

    protected void ensureGstInitialized() {
        if (!Gst.isInitialized()) {
            Gst.init("Gdx-Video-Engine", new String[]{});
        }
    }

    protected final void openPipeline(String path) {
        if (pipeline != null) {
            throw new IllegalStateException("Backend already opened");
        }

        ensureGstInitialized();

        pipeline = buildPipeline(path);
        if (pipeline == null) {
            throw new IllegalStateException("GStreamer: failed to create pipeline");
        }

        appSink = findAppSink(pipeline);
        volumeElement = findVolumeElement(pipeline);
        applyVolumeUnsafe();

        onPipelineBuilt(pipeline);
        attachBusWatch(pipeline);

        pipeline.setState(State.PAUSED);
        state.set(BackendState.READY);
    }

    protected void attachBusWatch(Pipeline p) {
        Bus bus = p.getBus();
        bus.connect((Bus.ERROR) (source, code, message) -> {
            RuntimeException ex = new RuntimeException("ERROR from " + source + " : " + message);
            lastError = ex;
            onBusError(ex);
        });
        bus.connect((Bus.EOS) source -> onEos());
        bus.connect((Bus.STATE_CHANGED) (source, old, now, pending) -> onStateChanged(old, now));
    }

    protected final void closePipeline() {
        Pipeline p = pipeline;
        pipeline = null;
        appSink = null;
        volumeElement = null;
        if (p == null) return;
        try {
            p.setState(State.NULL);
        } catch (Throwable ignored) {
        }
        try {
            p.dispose();
        } catch (Throwable ignored) {
        }
    }

    protected void refreshPositionAndDuration() {
        if (pipeline == null) return;
        try {
            long d = pipeline.queryDuration(Format.TIME);
            if (d > 0) durationNs = d;
            long p = pipeline.queryPosition(Format.TIME);
            if (p >= 0) positionNs = p;
        } catch (Throwable ignored) {
        }
    }

    protected final boolean sendSeek(long ns, double rate, EnumSet<SeekFlags> flags) {
        if (pipeline == null) return false;
        return pipeline.seek(
            rate,
            Format.TIME,
            flags,
            SeekType.SET, ns,
            SeekType.NONE, -1
        );
    }

    protected void applyVolumeUnsafe() {
        if (volumeElement == null) return;
        try {
            volumeElement.set("volume", volume);
            volumeElement.set("mute", muted);
        } catch (Throwable ignored) {
        }
    }

    protected String filePathToUri(String path) {
        return new File(path).toURI().toString();
    }

    protected boolean shouldLogSpeedSeek() {
        return false;
    }

    protected static double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    protected static Optional<Long> nsToMs(long ns) {
        if (ns < 0) return Optional.empty();
        return Optional.of(ns / 1_000_000L);
    }

    protected static Optional<Double> avgMs(long totalNs, long count) {
        if (count <= 0L) return Optional.empty();
        return Optional.of((double) totalNs / (double) count / 1_000_000.0);
    }

    protected static Optional<Double> maxMs(long maxNs) {
        if (maxNs <= 0L) return Optional.empty();
        return Optional.of((double) maxNs / 1_000_000.0);
    }

    protected static Optional<Integer> optionalPositiveInt(int value) {
        return value > 0 ? Optional.of(value) : Optional.empty();
    }

    /** Build and return a fully configured Pipeline for the given file path. */
    protected abstract Pipeline buildPipeline(String path);

    /** Return the AppSink used by the backend (or null if none). */
    protected abstract AppSink findAppSink(Pipeline p);

    /** Find volume element. Default: try by name "vol" then "volume" if present. */
    protected Element findVolumeElement(Pipeline p) {
        Element element = p.getElementByName("vol");
        if (element == null) {
            element = p.getElementByName("volume");
        }
        return element;
    }

    protected void onPipelineBuilt(Pipeline p) {}

    protected void onBusError(Throwable t) {}

    protected void onEos() {}

    protected void onStateChanged(State old, State now) {}

    // Non-regression notes:
    // - open/play/pollLatestFrame/recycle on a short and a long file
    // - pause/seek/stop/close sequences run multiple times
    // - volume/mute does not fail when no audio is present
}

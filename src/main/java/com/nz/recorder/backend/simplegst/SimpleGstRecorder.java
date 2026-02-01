package com.nz.recorder.backend.simplegst;

import com.nz.recorder.api.core.Recorder;
import com.nz.recorder.api.core.RecorderState;
import com.nz.recorder.api.core.RecordingResult;
import com.nz.recorder.api.core.VideoStats;
import com.nz.recorder.api.subscriptions.RecorderSubscriptions;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.event.EOSEvent;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal "string pipeline" backend.
 * - Parses a gst-launch-like pipeline description
 * - Controls Pipeline state PLAYING/PAUSED/NULL
 * - Collects minimal duration/stats
 *
 * Notes:
 * - This class assumes the pipeline writes to a file sink if needed.
 * - For "proper mp4 finalize", sending EOS + waiting for EOS can be necessary.
 */
public final class SimpleGstRecorder implements Recorder {

    private final AtomicReference<RecorderState> state = new AtomicReference<>(RecorderState.New);

    private final String pipelineDescription;
    private final Optional<Path> outputFile;

    private Pipeline pipeline;

    // Minimal session info
    private volatile long startNanos = -1L;
    private volatile long stopNanos = -1L;

    private final AtomicLong framesEncoded = new AtomicLong(0);
    private final AtomicLong framesDropped = new AtomicLong(0);

    // TODO: wire actual fpsRequested/fpsActual if you add caps probing / identity callbacks
    private volatile double fpsRequested = 60.0;
    private volatile double fpsActual = 0.0;

    private final RecorderSubscriptions subscriptions;
    private final Object eosLock = new Object();
    private volatile CountDownLatch eosLatch = new CountDownLatch(1);
    private volatile CountDownLatch errorLatch = new CountDownLatch(1);

    /**
     * Full ctor, allows injecting subscriptions implementation if you have one.
     */
    public SimpleGstRecorder(String pipelineDescription,
                             Optional<Path> outputFile,
                             RecorderSubscriptions subscriptions) {
        this.pipelineDescription = Objects.requireNonNull(pipelineDescription, "pipelineDescription");
        this.outputFile = outputFile == null ? Optional.empty() : outputFile;
        this.subscriptions = subscriptions; // can be null if you don't have a noop impl
        this.state.set(RecorderState.Idle);
    }

    /**
     * Convenience ctor: creates a basic pipeline for Linux X11 screen recording (H264 -> MP4).
     */

    @Override
    public RecorderState getState() {
        return state.get();
    }

    @Override
    public void init() {
        ensureNotClosed();

        RecorderState s = state.get();
        if (s == RecorderState.Recording || s == RecorderState.Paused) {
            throw new IllegalStateException("init() not allowed while recording. Current=" + s);
        }
        if (pipeline != null) {
            // idempotent-ish
            return;
        }
        try {
            Gst.init("SimpleGstRecorder", new String[0]);
            pipeline = (Pipeline) Gst.parseLaunch(pipelineDescription);

            attachBusHandlers(pipeline);

            state.set(RecorderState.Idle);
        } catch (Exception e) {
            state.set(RecorderState.Error);
            throw new RuntimeException("Failed to init GStreamer pipeline", e);
        }
    }

    @Override
    public void start() {
        ensureNotClosed();
        ensurePipeline();

        RecorderState s = state.get();
        if (s == RecorderState.Recording) return; // idempotent

        if (s.notIn(RecorderState.Idle, RecorderState.Paused, RecorderState.Stopped)) {
            throw new IllegalStateException("start() allowed from IDLE/PAUSED/STOPPED. Current=" + s);
        }

        state.set(RecorderState.Starting);

        setPipelineState(State.PLAYING);

        startNanos = System.nanoTime();
        stopNanos = -1L;

        state.set(RecorderState.Recording);
    }

    @Override
    public void pause() {
        ensureNotClosed();
        ensurePipeline();

        RecorderState s = state.get();
        if (s == RecorderState.Paused) return;

        if (s != RecorderState.Recording) {
            throw new IllegalStateException("pause() only allowed from RECORDING. Current=" + s);
        }

        setPipelineState(State.PAUSED);
        state.set(RecorderState.Paused);
    }

    @Override
    public void resume() {
        ensureNotClosed();
        ensurePipeline();

        RecorderState s = state.get();
        if (s == RecorderState.Recording) return;

        if (s != RecorderState.Paused) {
            throw new IllegalStateException("resume() only allowed from PAUSED. Current=" + s);
        }

        setPipelineState(State.PLAYING);
        state.set(RecorderState.Recording);
    }

    @Override
    public RecordingResult stop() {
        RecorderState s = state.get();
        if (s == RecorderState.Closed) {
            return buildResult(Optional.of("Recorder already closed"));
        }
        if (s == RecorderState.Stopped) {
            return buildResult(Optional.empty());
        }
        if (pipeline == null) {
            state.set(RecorderState.Stopped);
            stopNanos = System.nanoTime();
            return buildResult(Optional.of("stop() called before init(); no pipeline."));
        }

        state.set(RecorderState.Stopping);

        try {
            // Important: mp4mux finalize happens on EOS
            // Ensure pipeline is running so EOS can flow
            setPipelineState(State.PLAYING);

            resetLatches(); // (optionnel ici si déjà reset au start, mais safe)

            boolean sent = pipeline.sendEvent(new EOSEvent());
            if (!sent) {
                System.err.println("[Gst] Warning: failed to send EOS event");
            }

            boolean eosOk;
            synchronized (eosLock) {
                // attend EOS (ou ERROR) max X sec
                eosOk = eosLatch.await(10, TimeUnit.SECONDS);
            }
            if (!eosOk) {
                System.err.println("[Gst] Warning: EOS not received within timeout; forcing NULL");
            }
            // Maintenant seulement on libère
            setPipelineState(State.NULL);

            stopNanos = System.nanoTime();
            state.set(RecorderState.Stopped);
            return buildResult(Optional.empty());
        } catch (Exception e) {
            state.set(RecorderState.Error);
            try { setPipelineState(State.NULL); } catch (Exception ignored) {}
            return buildResult(Optional.of("stop() failed: " + e.getMessage()));
        }
    }
    private void resetLatches() {
        synchronized (eosLock) {
            eosLatch = new CountDownLatch(1);
            errorLatch = new CountDownLatch(1);
        }
    }

    @Override
    public RecorderSubscriptions events() {
        // If you have a noop helper in your project, replace this with it.
        // e.g. return RecorderSubscriptions.noop();
        return subscriptions;
    }

    @Override
    public void close() {
        RecorderState s = state.get();
        if (s == RecorderState.Closed) return;

        try {
            stop();
        } finally {
            if (pipeline != null) {
                try {
                    pipeline.dispose();
                } catch (Exception ignored) {
                }
                pipeline = null;
            }
            state.set(RecorderState.Closed);
        }
    }

    // ----------------- internals -----------------

    private void attachBusHandlers(Pipeline p) {
        Bus bus = p.getBus();
        bus.connect((Bus.ERROR) (source, code, message) -> {
            state.set(RecorderState.Error);
            stopNanos = System.nanoTime();
            System.err.println("[Gst][ERROR] " + message + " (code=" + code + ")");
            synchronized (eosLock) {
                errorLatch.countDown();
                eosLatch.countDown(); // débloque stop() aussi
            }
        });

        bus.connect((Bus.EOS) source -> {
            stopNanos = System.nanoTime();
            System.out.println("[Gst] EOS");
            synchronized (eosLock) {
                eosLatch.countDown();
            }
        });
    }

    private void ensurePipeline() {
        if (pipeline == null) {
            throw new IllegalStateException("Pipeline is null. Did you call init()?");
        }
        if (state.get() == RecorderState.Error) {
            throw new IllegalStateException("Recorder is in ERROR state.");
        }
    }

    private void ensureNotClosed() {
        if (state.get() == RecorderState.Closed) {
            throw new IllegalStateException("Recorder is CLOSED.");
        }
    }

    private void setPipelineState(State target) {
        ensurePipeline();

        StateChangeReturn r = pipeline.setState(target);

        // ASYNC is normal in GStreamer; FAILURE is the important one.
        if (r == StateChangeReturn.FAILURE) {
            state.set(RecorderState.Error);
            throw new RuntimeException("Failed to change pipeline state to " + target);
        }
    }

    private RecordingResult buildResult(Optional<String> warning) {
        long end = stopNanos > 0 ? stopNanos : System.nanoTime();
        long start = startNanos > 0 ? startNanos : end;

        Duration duration = Duration.ofNanos(Math.max(0, end - start));

        // If you never started, you might prefer duration=0; here it's already handled.
        VideoStats stats = new VideoStats(
            fpsRequested,
            fpsActual,
            framesEncoded.get(),
            framesDropped.get()
        );

        return new RecordingResult(
            outputFile,
            duration,
            stats,
            warning
        );
    }
}

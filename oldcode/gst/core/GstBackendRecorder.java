//package com.nz.recorder.backend.gst.core;
//
//import com.nz.recorder.api.core.Recorder;
//import com.nz.recorder.api.core.RecorderSettings;
//import com.nz.recorder.api.core.RecorderState;
//import com.nz.recorder.api.core.RecordingResult;
//import com.nz.recorder.api.output.OutputSettings;
//import com.nz.recorder.api.save.FileSave;
//import com.nz.recorder.api.save.NoSave;
//import com.nz.recorder.api.save.SaveToFileRequest;
//import com.nz.recorder.api.subscriptions.RecorderSubscriptions;
//import com.nz.recorder.api.targets.WindowTarget;
//import com.nz.recorder.backend.gst.events.GstEventHub;
//import com.nz.recorder.backend.gst.io.GstResults;
//import com.nz.recorder.backend.gst.session.GstSession;
//import com.nz.recorder.backend.gst.builder.GstSessionFactory;
//import com.nz.recorder.backend.gst.session.GstSessionMetrics;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Objects;
//import java.util.Optional;
//
//public final class GstBackendRecorder implements Recorder {
//
//    private final RecorderSettings settings;
//    private final GstEventHub events;
//
//    private RecorderState state = RecorderState.Idle;
//    private GstSession session;
//    private RecordingResult lastResult;
//    private Throwable lastError;
//
//    public GstBackendRecorder(RecorderSettings settings) {
//        this.settings = Objects.requireNonNull(settings, "settings");
//        this.events = new GstEventHub();
//        validateSettings(settings);
//    }
//
//    @Override
//    public synchronized RecorderState state() {
//        return state;
//    }
//
//    @Override
//    public void init() {
//        try {
//            this.session = GstSessionFactory.create(settings, events);
//        } catch (Throwable t) {
//            fail(t);
//            throw t;
//        }
//    }
//
//
//    @Override
//    public synchronized void start() {
//        ensureState(RecorderState.Idle);
//        transition(RecorderState.Starting);
//        try {
//            session.start();
//            transition(RecorderState.Recording);
//        } catch (Throwable t) {
//            fail(t);
//            throw t;
//        }
//    }
//
//    @Override
//    public synchronized void pause() {
//        if (state == RecorderState.Paused) return;
//        ensureState(RecorderState.Recording);
//        try {
//            session.pause();
//            transition(RecorderState.Paused);
//        } catch (Throwable t) {
//            fail(t);
//            throw t;
//        }
//    }
//
//    @Override
//    public synchronized void resume() {
//        if (state == RecorderState.Recording) return;
//        ensureState(RecorderState.Paused);
//        try {
//            session.resume();
//            transition(RecorderState.Recording);
//        } catch (Throwable t) {
//            fail(t);
//            throw t;
//        }
//    }
//
//    @Override
//    public synchronized RecordingResult stop() {
//        if (state == RecorderState.Stopped) {
//            return lastResult != null ? lastResult : GstResults.defaultResult(settings, Optional.empty());
//        }
//        if (state == RecorderState.Error) {
//            return lastResult != null ? lastResult : GstResults.defaultResult(settings, Optional.empty());
//        }
//        if (state == RecorderState.Idle) {
//            transition(RecorderState.Stopped);
//            lastResult = GstResults.defaultResult(settings, Optional.empty());
//            return lastResult;
//        }
//
//        transition(RecorderState.Stopping);
//        Optional<Path> output = Optional.empty();
//        try {
//            if (session != null) {
//                output = session.stopAndFinalize();
//            }
//            GstSessionMetrics metrics = session != null ? session.metrics() : null;
//            lastResult = GstResults.resultFromMetrics(settings, output, metrics);
//            transition(RecorderState.Stopped);
//            return lastResult;
//        } catch (Throwable t) {
//            fail(t);
//            throw t;
//        } finally {
//            session = null;
//        }
//    }
//
//    @Override
//    public synchronized RecordingResult saveSnapshot(SaveRequest request) {
//        if (settings.runMode() != RecordRunMode.RING_BUFFER) {
//            throw new UnsupportedOperationException("saveSnapshot only supported in RING_BUFFER mode");
//        }
//        if (!(request instanceof SaveToFileRequest)) {
//            throw new UnsupportedOperationException("Only SaveToFileRequest is supported for snapshots in this backend");
//        }
//        if (state != RecorderState.Recording && state != RecorderState.Paused) {
//            throw new IllegalStateException("saveSnapshot requires RECORDING or PAUSED state");
//        }
//        if (session == null) {
//            throw new IllegalStateException("No active session");
//        }
//        try {
//            Optional<Path> output = session.saveSnapshot(request);
//            RecordingResult result = GstResults.resultFromMetrics(settings, output, session.metrics());
//            lastResult = result;
//            return result;
//        } catch (Throwable t) {
//            fail(t);
//            throw t;
//        }
//    }
//
//    @Override
//    public RecorderSubscriptions events() {
//        return events;
//    }
//
//    @Override
//    public void close() {
//        stop();
//    }
//
//    private void validateSettings(RecorderSettings s) {
//        Objects.requireNonNull(s, "settings");
//
//        if (s.frameAccess().isPresent()) {
//            events.emitWarning("FrameAccessSettings is currently ignored by Gst backend (not implemented yet).");
//        }
//
//        if (s.target() instanceof WindowTarget) {
//            throw new UnsupportedOperationException("WINDOW capture not supported by Gst backend yet (only SCREEN).");
//        }
//
//        if (s.saveStrategy() instanceof StreamSave) {
//            throw new UnsupportedOperationException("StreamSave is not supported by Gst backend yet.");
//        }
//
//        if (s.runMode() == RecordRunMode.CONTINUOUS) {
//            if (s.saveStrategy() instanceof NoSave) {
//                throw new UnsupportedOperationException("CONTINUOUS mode requires FileSave in this backend");
//            }
//            if (s.saveStrategy() instanceof FileSave fs) {
//                validateFileOutput(fs.output());
//            }
//        }
//
//        if (s.runMode() == RecordRunMode.RING_BUFFER) {
//            if (s.saveStrategy() instanceof FileSave fs) {
//                validateFileOutput(fs.output());
//            }
//        }
//    }
//
//    private void validateFileOutput(OutputSettings output) {
//        Path file = output.file().toAbsolutePath();
//        if (!output.overwrite() && Files.exists(file)) {
//            throw new IllegalArgumentException("Output file already exists and overwrite=false: " + file);
//        }
//    }
//
//    private void ensureState(RecorderState expected) {
//        if (state != expected) {
//            throw new IllegalStateException("Expected state " + expected + " but was " + state);
//        }
//    }
//
//    private void transition(RecorderState next) {
//        state = next;
//        events.emitState(next);
//    }
//
//    private void fail(Throwable t) {
//        lastError = t;
//        state = RecorderState.Error;
//        events.emitError(t);
//        events.emitState(RecorderState.Error);
//    }
//}

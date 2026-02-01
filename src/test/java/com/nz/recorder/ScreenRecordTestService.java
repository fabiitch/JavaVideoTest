package com.nz.recorder;

import com.nz.recorder.backend.simplegst.SimpleGstRecorder;
import com.nz.recorder.backend.simplegst.SimpleGstRecorderBuilder;
import com.nz.sr.gstreamer.GstTestUtils;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenRecordTestService {

    private static final AtomicBoolean GST_INITIALIZED = new AtomicBoolean(false);

    private final ExecutorService executor;
    private final AtomicBoolean recording;
    private final Path output;
    private SimpleGstRecorder recorder;

    public ScreenRecordTestService(Path output) {
        this.output = output;
        this.recording = new AtomicBoolean(false);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "recorder-test-executor");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        executor.submit(() -> {
            if (recording.get()) {
                return;
            }

            System.out.println("[GDX] Start record -> " + output.toAbsolutePath());
            recording.set(true);
            if (GST_INITIALIZED.compareAndSet(false, true)) {
                GstTestUtils.initGStreamer();
            }
            if (!recording.get()) {
                return;
            }
            recorder = SimpleGstRecorderBuilder.windowsDesktopToMp4(output);
            recorder.init();
            recorder.start();
        });
    }

    public void stop() {
        if (!recording.get()) {
            return;
        }
        recording.set(false);
        executor.submit(() -> {
            System.out.println("[GDX] Stop record");
            try {
                recorder.stop();
            } catch (Exception ex) {
                System.err.println("[GDX] stop() failed: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                recorder = null;
            }
        });
    }

    public boolean isRecording() {
        return recording.get();
    }

    public void dispose() {
        executor.submit(() -> {
            if (recording.get() && recorder != null) {
                try {
                    System.out.println("[GDX] dispose() -> stopping recorder");
                    recorder.stop();
                } catch (Exception ignored) {
                    // ignore shutdown errors
                } finally {
                    recorder = null;
                    recording.set(false);
                }
            }
        });
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

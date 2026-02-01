package com.nz.recorder.backend.gst.session.continuous;

import com.nz.recorder.api.core.RecorderSettings;
import com.nz.recorder.api.output.OutputSettings;
import com.nz.recorder.api.save.FileSave;
import com.nz.recorder.api.targets.ScreenTarget;
import com.nz.recorder.backend.gst.builder.GstRecorderBuilder;
import com.nz.recorder.backend.gst.config.GstQualityMapper;
import com.nz.recorder.backend.gst.builder.GstTargetsBuilder;
import com.nz.recorder.backend.gst.events.GstEventHub;
import com.nz.recorder.backend.gst.io.GstFiles;
import com.nz.recorder.backend.gst.io.GstResults;
import com.nz.recorder.backend.gst.session.GstSession;
import com.nz.recorder.backend.gst.session.GstSessionMetrics;
import com.nz.recorder.backend.gst.config.settings.GstEncoderSettings;
import com.nz.recorder.backend.gst.source.GstVideoSource;
import com.nz.recorder.backend.gst.config.settings.GstMuxSettings;
import com.nz.recorder.backend.gst.source.video.GstScreenCapture;
import com.nz.recorder.backend.gst.recorder.ModularGstRecorder;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class GstContinuousSession implements GstSession {

    private final RecorderSettings settings;
    private final GstEventHub events;
    private ModularGstRecorder recorder;
    private Path outputPath;
    private final GstSessionMetrics metrics = new GstSessionMetrics();

    public GstContinuousSession(RecorderSettings settings, GstEventHub events) {
        this.settings = settings;
        this.events = events;
    }

    @Override
    public void start() {
        if (!(settings.saveStrategy() instanceof FileSave fs)) {
            throw new UnsupportedOperationException("CONTINUOUS mode requires FileSave in this backend");
        }

        OutputSettings out = fs.output();
        this.outputPath = out.file().toAbsolutePath();
        ensureParentDirectory(outputPath);

        Rectangle bounds = GstTargetsBuilder.resolveScreenBounds((ScreenTarget) settings.target());

        GstEncoderSettings enc = GstQualityMapper.toEncoderSettings(settings.quality());
        GstMuxSettings mux = GstMuxSettings.fromExtension(GstFiles.extension(outputPath));

        GstVideoSource video = new GstScreenCapture(
            GstScreenCapture.API.D3D11,
            bounds.x, bounds.y, bounds.width, bounds.height,
            settings.fps()
        );

        this.recorder = GstRecorderBuilder.newBuilder()
                .video(video)
                .noAudio()
                .encoder(enc)
                .mux(mux)
                .output(outputPath.toFile())
                .onError(events::emitError)
                .build();

        metrics.markStart();
        recorder.start();
        events.emitStats(GstResults.statsRequestedOnly(settings));
    }

    @Override
    public void pause() {
        if (recorder != null) {
            recorder.pause();
        }
    }

    @Override
    public void resume() {
        if (recorder != null) {
            recorder.resume();
        }
    }

    @Override
    public Optional<Path> stopAndFinalize() {
        if (recorder == null) return Optional.empty();
        try {
            recorder.stop();
            return Optional.ofNullable(outputPath);
        } finally {
            metrics.markStop();
            recorder = null;
        }
    }


    @Override
    public GstSessionMetrics metrics() {
        return metrics;
    }

    private void ensureParentDirectory(Path file) {
        Path parent = file.getParent();
        if (parent == null) return;
        try {
            Files.createDirectories(parent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create output directory: " + parent, e);
        }
    }
}

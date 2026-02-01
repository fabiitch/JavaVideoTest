package com.nz.recorder.backend.gst.io;

import com.nz.recorder.api.core.RecorderSettings;
import com.nz.recorder.api.core.RecordingResult;
import com.nz.recorder.api.core.VideoStats;
import com.nz.recorder.backend.gst.session.GstSessionMetrics;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public final class GstResults {

    public static VideoStats statsRequestedOnly(RecorderSettings settings) {
        return new VideoStats(
                settings.fps(),
                0.0,
                0,
                0
        );
    }

    public static RecordingResult defaultResult(RecorderSettings settings, Optional<Path> output) {
        return new RecordingResult(
                output,
                Duration.ZERO,
                statsRequestedOnly(settings),
                Optional.empty()
        );
    }

    public static RecordingResult resultFromMetrics(RecorderSettings settings, Optional<Path> output, GstSessionMetrics metrics) {
        if (metrics == null) {
            return defaultResult(settings, output);
        }
        return new RecordingResult(
                output,
                metrics.duration(),
                metrics.toVideoStats(settings),
                Optional.empty()
        );
    }

    private GstResults() {}
}

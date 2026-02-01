package com.nz.recorder.api.core;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/** Result returned on stop/finalize or snapshot. */
public record RecordingResult(
        Optional<Path> outputFile,
        Duration duration,
        VideoStats stats,
        Optional<String> warning
) {
    public RecordingResult {
        if (outputFile == null) outputFile = Optional.empty();
        if (duration == null) throw new IllegalArgumentException("duration");
        if (stats == null) throw new IllegalArgumentException("stats");
        if (warning == null) warning = Optional.empty();
    }
}

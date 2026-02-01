package com.nz.recorder.api.output;

import java.nio.file.Path;

/** Output file settings. */
public record OutputSettings(Path file, boolean overwrite) {
    public OutputSettings {
        if (file == null) throw new IllegalArgumentException("file");
    }
}

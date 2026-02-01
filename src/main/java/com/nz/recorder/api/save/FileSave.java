package com.nz.recorder.api.save;

import com.nz.recorder.api.output.OutputSettings;

public record FileSave(OutputSettings output) implements SaveStrategy {
    public FileSave {
        if (output == null) throw new IllegalArgumentException("output");
    }
}

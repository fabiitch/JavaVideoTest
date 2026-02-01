package com.nz.recorder.api.save;

import com.nz.recorder.api.output.OutputSettings;

public record SaveToFileRequest(OutputSettings output) {
    public SaveToFileRequest {
        if (output == null) throw new IllegalArgumentException("output");
    }
}

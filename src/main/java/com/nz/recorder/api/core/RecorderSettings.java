package com.nz.recorder.api.core;

import com.nz.recorder.api.output.QualityPreset;
import com.nz.recorder.api.save.SaveStrategy;
import com.nz.recorder.api.targets.CaptureTarget;

/** Immutable settings used to create a Recorder. */
public record RecorderSettings(
        CaptureTarget target,
        QualityPreset quality,
        int fps,
        SaveStrategy saveStrategy
) {
    public RecorderSettings {
        if (target == null) throw new IllegalArgumentException("target");
        if (quality == null) throw new IllegalArgumentException("quality");
        if (fps <= 0) throw new IllegalArgumentException("fps must be > 0");
        if (saveStrategy == null) throw new IllegalArgumentException("saveStrategy");
    }
}

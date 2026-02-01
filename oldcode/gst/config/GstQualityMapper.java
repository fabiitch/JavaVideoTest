package com.nz.recorder.backend.gst.config;

import com.nz.recorder.api.output.QualityPreset;
import com.nz.recorder.backend.gst.config.settings.GstEncoderSettings;

public final class GstQualityMapper {

    public static GstEncoderSettings toEncoderSettings(QualityPreset preset) {
        return new GstEncoderSettings(
                true,
                videoKbps(preset),
                audioKbps(preset),
                gopSeconds(preset)
        );
    }

    public static int videoKbps(QualityPreset preset) {
        return switch (preset) {
            case LOW -> 2500;
            case MID -> 6000;
            case HIGH -> 12000;
            case ULTRA -> 20000;
        };
    }

    public static int audioKbps(QualityPreset preset) {
        return switch (preset) {
            case LOW -> 128;
            case MID -> 160;
            case HIGH -> 192;
            case ULTRA -> 192;
        };
    }

    public static int gopSeconds(QualityPreset preset) {
        return 2;
    }

    private GstQualityMapper() {}
}

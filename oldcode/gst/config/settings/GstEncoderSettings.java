package com.nz.recorder.backend.gst.config.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GstEncoderSettings {
    public final boolean preferNvenc;
    public final int videoKbps; // kilobits per second
    public final int audioKbps; // kilobits per second
    public final int gop; // seconds

    public static GstEncoderSettings defaultH264() {
        return new GstEncoderSettings(true, 8000, 160, 2);
    }

}

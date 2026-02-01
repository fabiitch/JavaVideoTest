package com.nz.recorder.backend.gst.config.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GstMuxSettings {
    public final String container; // "mp4" or "mkv"

    public static GstMuxSettings fromExtension(String ext) {
        String low = ext.toLowerCase();
        if (low.endsWith(".mkv")) return new GstMuxSettings("mkv");
        return new GstMuxSettings("mp4");
    }
}

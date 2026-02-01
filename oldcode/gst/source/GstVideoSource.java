package com.nz.recorder.backend.gst.source;

import org.freedesktop.gstreamer.Bin;

public interface GstVideoSource {
    /**
     * Return a BIN with a single src pad named "src".
     */
    Bin build();
}

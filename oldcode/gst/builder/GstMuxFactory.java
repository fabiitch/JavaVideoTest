package com.nz.recorder.backend.gst.builder;

import com.nz.recorder.backend.gst.config.settings.GstMuxSettings;
import lombok.experimental.UtilityClass;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;

@UtilityClass
public class GstMuxFactory {
    public static Element build(GstMuxSettings mux) {
        Element m;
        if ("mkv".equalsIgnoreCase(mux.container)) {
            m = ElementFactory.make("matroskamux", "mux");
        } else {
            m = ElementFactory.make("mp4mux", "mux");
            // Fast start: try to move moov atom to start for progressive download
            try {
                m.set("faststart", true);
            } catch (Exception ignored) {
            }
            try {
                m.set("fragment-duration", 1000);
            } catch (Exception ignored) {
            }
        }
        if (m == null) throw new IllegalStateException("Missing muxer for container: " + mux.container);
        return m;
    }
}

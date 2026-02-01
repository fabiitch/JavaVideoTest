package com.nz.recorder.backend.gst.support;

import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Element;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GstSupport {

    private static final AtomicBoolean INIT = new AtomicBoolean(false);

    public static void ensureGstInitOnce() {
        if (INIT.compareAndSet(false, true)) {
            try {
                Gst.init("nz-recorder", new String[]{});
                logInfo("GStreamer initialized");
            } catch (Throwable t) {
                logError("GStreamer init failed: " + t.getMessage());
                throw new IllegalStateException("GStreamer init failed", t);
            }
        }
    }

    public static void trySet(Element e, String property, Object value) {
        try {
            e.set(property, value);
        } catch (Exception ignored) {
        }
    }

    public static void logInfo(String message) {
        System.out.println("[GST][INFO] " + message);
    }

    public static void logWarn(String message) {
        System.out.println("[GST][WARN] " + message);
    }

    public static void logError(String message) {
        System.err.println("[GST][ERROR] " + message);
    }

    private GstSupport() {}
}

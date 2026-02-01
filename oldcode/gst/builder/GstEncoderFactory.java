package com.nz.recorder.backend.gst.builder;

import com.nz.recorder.backend.gst.config.settings.GstEncoderSettings;
import com.nz.share.gst.recorder.GstElementProbe;
import com.nz.share.gst.recorder.GstRecorderLog;
import lombok.experimental.UtilityClass;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;

@UtilityClass
public class GstEncoderFactory {
   public static GstEncoderPick pickH264(GstEncoderSettings enc, int fps) {
        GstElementProbe probe = GstElementProbe.defaultProbe();
        int gopFrames = Math.max(1, enc.gop * fps);
        if (enc.preferNvenc) {
            Element nv = tryMake("nvh264enc", "nvenc"); // requires nvcodec plugin
            if (nv != null) {
                safeSet(nv, "bitrate", enc.videoKbps, probe);
                safeSet(nv, "rc-mode", "cbr", probe);
                safeSet(nv, "preset", "low-latency-hq", probe);
                safeSet(nv, "zerolatency", true, probe);
                safeSet(nv, "bframes", 0, probe);
                safeSet(nv, "gop-size", gopFrames, probe);
                GstRecorderLog.info("Encoder=nvh264enc bitrateKbps=" + enc.videoKbps + " gopFrames=" + gopFrames);
                return new GstEncoderPick(nv, true);
            }
        }
        // Fallback to x264enc
        Element x = ElementFactory.make("x264enc", "x264");
        if (x == null) throw new IllegalStateException("Missing encoder: x264enc");
        safeSet(x, "bitrate", enc.videoKbps, probe);
        safeSet(x, "tune", "zerolatency", probe);
        safeSet(x, "speed-preset", "veryfast", probe);
        safeSet(x, "key-int-max", gopFrames, probe);
        safeSet(x, "bframes", 0, probe);
        GstRecorderLog.info("Encoder=x264enc bitrateKbps=" + enc.videoKbps + " gopFrames=" + gopFrames);
        return new GstEncoderPick(x, false);
    }

    private static Element tryMake(String factory, String name) {
        try {
            return ElementFactory.make(factory, name);
        } catch (Exception e) {
            return null;
        }
    }

    private static void safeSet(Element e, String prop, Object value, GstElementProbe probe) {
        try {
            if (probe.hasProperty(e, prop)) {
                e.set(prop, value);
            }
        } catch (Exception ignored) {
        }
    }
}

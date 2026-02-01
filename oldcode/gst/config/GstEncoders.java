package com.nz.recorder.backend.gst.config;

import com.nz.recorder.api.output.QualityPreset;
import com.nz.recorder.backend.gst.support.GstSupport;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import com.nz.share.gst.recorder.GstElementProbe;

/**
 * Encoder chain builder (v1): choose NVENC if available, fallback to x264.
 */
public final class GstEncoders {

    public static final class EncoderChain {
        public final Element encoder;
        public final Element parser;
        public final boolean hardware;

        EncoderChain(Element encoder, Element parser, boolean hardware) {
            this.encoder = encoder;
            this.parser = parser;
            this.hardware = hardware;
        }
    }

    public static EncoderChain buildH264Chain(QualityPreset preset, int fps, int gopSeconds) {
        GstElementProbe probe = GstElementProbe.defaultProbe();
        int kbps = GstQualityMapper.videoKbps(preset);
        int gopFrames = Math.max(1, gopSeconds * fps);

        Element enc = ElementFactory.make("nvh264enc", "enc");
        if (enc != null) {
            safeSet(enc, "bitrate", kbps, probe);
            safeSet(enc, "rc-mode", "cbr", probe);
            safeSet(enc, "preset", "low-latency-hq", probe);
            safeSet(enc, "zerolatency", true, probe);
            safeSet(enc, "bframes", 0, probe);
            safeSet(enc, "gop-size", gopFrames, probe);
            GstSupport.logInfo("Encoder=nvh264enc bitrateKbps=" + kbps + " gopFrames=" + gopFrames);
            Element parse = ElementFactory.make("h264parse", "parse");
            if (parse == null) throw new IllegalStateException("Missing element: h264parse");
            return new EncoderChain(enc, parse, true);
        }

        enc = ElementFactory.make("x264enc", "enc");
        if (enc == null) throw new IllegalStateException("Missing encoders: nvh264enc and x264enc");
        safeSet(enc, "bitrate", kbps, probe);
        safeSet(enc, "speed-preset", "veryfast", probe);
        safeSet(enc, "tune", "zerolatency", probe);
        safeSet(enc, "key-int-max", gopFrames, probe);
        safeSet(enc, "bframes", 0, probe);
        GstSupport.logInfo("Encoder=x264enc bitrateKbps=" + kbps + " gopFrames=" + gopFrames);

        Element parse = ElementFactory.make("h264parse", "parse");
        if (parse == null) throw new IllegalStateException("Missing element: h264parse");
        return new EncoderChain(enc, parse, false);
    }

    private static void safeSet(Element element, String property, Object value, GstElementProbe probe) {
        try {
            if (probe.hasProperty(element, property)) {
                element.set(property, value);
            }
        } catch (Exception ignored) {
        }
    }

    private GstEncoders() {}
}

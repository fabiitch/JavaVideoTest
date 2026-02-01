package com.nz.recorder.backend.simplegst;

import org.freedesktop.gstreamer.ElementFactory;

import java.nio.file.Path;
import java.util.Optional;

public class SimpleGstRecorderBuilder {
    /**
     * Windows desktop -> MP4 (H264), with best-effort source selection:
     * - Prefer d3d11screencapturesrc (Windows 10/11, best perf when available)
     * - Fallback to gdiscreencapsrc (GDI)
     */
    public static SimpleGstRecorder windowsDesktopToMp4(Path outputFile) {
        return windowsDesktopToMp4(outputFile, 0, 60, 8000, true);
    }

    /**
     * @param monitorIndex 0-based monitor index (used by d3d11screencapturesrc when available)
     * @param fps          target framerate
     * @param bitrateKbps  x264enc bitrate (kbps)
     * @param preferD3D11  if true, tries d3d11 first
     */
    public static SimpleGstRecorder windowsDesktopToMp4(Path outputFile,
                                                        int monitorIndex,
                                                        int fps,
                                                        int bitrateKbps,
                                                        boolean preferD3D11) {
        String out = outputFile.toAbsolutePath().toString().replace("\\", "/");

        // Keep it simple: convert -> (optional framerate cap) -> x264 -> mp4
        String desc =
            "d3d11screencapturesrc monitor-index=" + monitorIndex +
                " do-timestamp=true " +
                "! d3d11convert " +
                "! video/x-raw(memory:D3D11Memory),format=NV12,framerate=" + fps + "/1 " +
                "! queue " +
                "! nvd3d11h264enc rc-mode=cbr bitrate=" + bitrateKbps +
                " gop-size=" + (fps * 2) +
                " preset=p4 tune=high-quality zerolatency=true " +
                " repeat-sequence-header=true aud=true " +
                "! h264parse config-interval=-1 " +
                "! video/x-h264,stream-format=avc,alignment=au " +
                "! queue ! mux. " +

                // AUDIO (PC sound / loopback)
                "wasapisrc loopback=true do-timestamp=true " +
                "! audioconvert ! audioresample " +
                "! audio/x-raw,rate=48000,channels=2 " +
                "! queue " +
                "! avenc_aac bitrate=160000 " +
                "! aacparse " +
                "! queue ! mux. " +
                // MUX
                "qtmux name=mux faststart=true " +
                "! filesink location=\"" + out + "\"";

        return new SimpleGstRecorder(desc, Optional.of(outputFile), null);
    }

    private static boolean elementExists(String factoryName) {
        try {
            return ElementFactory.find(factoryName) != null;
        } catch (Throwable t) {
            return false;
        }
    }
}

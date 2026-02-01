package com.nz.share.gst.init;

import com.nz.share.gst.exception.GStreamInitException;
import org.freedesktop.gstreamer.ElementFactory;

import java.io.File;

/**
 * Simple runtime checks for essential GStreamer elements.
 * Returns boolean instead of throwing exceptions.
 */
public final class GStreamInstallCheck {

    private GStreamInstallCheck() {
    }

    public static boolean checkRecording(boolean includeHevc) throws GStreamInitException {
        boolean ok = true;

        if (!onePresent("d3d11screencapturesrc", "gdiscreencapsrc"))
            return fail("Missing screen capture source (d3d11screencapture or gdiscreencapsrc)");

        if (!onePresent("wasapi2src", "wasapisrc"))
            return fail("Missing Windows audio loopback source (wasapi2src or wasapisrc)");

        if (!allPresent("videoconvert", "videoscale", "queue", "capsfilter"))
            return fail("Missing basic video elements (videoconvert/videoscale/queue/capsfilter)");

        if (!allPresent("audioconvert", "audioresample", "queue"))
            return fail("Missing basic audio elements (audioconvert/audioresample/queue)");

        if (!onePresent("nvh264enc", "x264enc"))
            return fail("Missing H.264 encoder (nvh264enc or x264enc)");

        if (!present("h264parse"))
            return fail("Missing h264parse");

        if (includeHevc) {
            if (!onePresent("nvh265enc", "x265enc"))
                return fail("Missing HEVC encoder (nvh265enc or x265enc)");
            if (!present("h265parse"))
                return fail("Missing h265parse");
        }

        if (!onePresent("voaacenc", "avenc_aac", "fdkaacenc"))
            return fail("Missing AAC encoder (voaacenc / avenc_aac / fdkaacenc)");

        if (!present("aacparse"))
            return fail("Missing aacparse");

        if (!onePresent("mp4mux", "matroskamux"))
            return fail("Missing container muxer (mp4mux or matroskamux)");

        if (!present("filesink"))
            return fail("Missing filesink (output)");

        return ok;
    }

    public static boolean checkPlayback(boolean includeHevc) throws GStreamInitException {
        boolean ok = true;

        if (!present("playbin"))
            return fail("Missing playbin (core playback element)");

        if (!onePresent("qtdemux", "matroskademux"))
            return fail("Missing demuxer (qtdemux or matroskademux)");

        if (!onePresent("d3d11h264dec", "nvh264dec", "avdec_h264", "openh264dec"))
            return fail("Missing H.264 decoder");

        if (!present("h264parse"))
            return fail("Missing h264parse");

        if (includeHevc) {
            if (!onePresent("d3d11h265dec", "nvh265dec", "avdec_h265"))
                return fail("Missing HEVC decoder");
            if (!present("h265parse"))
                return fail("Missing h265parse");
        }

        if (!onePresent("avdec_aac", "faad"))
            return fail("Missing AAC decoder (avdec_aac or faad)");

        if (!present("aacparse"))
            return fail("Missing aacparse");

        if (!onePresent("d3d11videosink", "glimagesink", "autovideosink"))
            return fail("Missing video sink (d3d11videosink / glimagesink / autovideosink)");

        if (!onePresent("wasapisink", "autoaudiosink"))
            return fail("Missing audio sink (wasapisink / autoaudiosink)");

        return ok;
    }

    public static boolean checkAll(File gstRoot, boolean includeHevc) throws GStreamInitException {
        return checkRecording(includeHevc) && checkPlayback(includeHevc);
    }

    /* ===== Generic helpers ===== */

    private static boolean present(String factory) {
        return ElementFactory.find(factory) != null;
    }

    private static boolean allPresent(String... factories) {
        for (String f : factories) {
            if (!present(f)) return false;
        }
        return true;
    }

    private static boolean onePresent(String... factories) {
        for (String f : factories) {
            if (present(f)) return true;
        }
        return false;
    }

    private static boolean fail(String message) throws GStreamInitException {
        throw new GStreamInitException(message);
    }
}

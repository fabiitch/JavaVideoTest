package com.nz.recorder.backend.gst.source.audio;

import com.nz.recorder.backend.gst.source.GstAudioSource;
import org.freedesktop.gstreamer.*;

public class GstWasapiMicSource implements GstAudioSource {
    private final boolean loopback; // true = capture system audio
    private final int channels;
    private final int rate;

    public GstWasapiMicSource(boolean loopback, int channels, int rate) {
        this.loopback = loopback;
        this.channels = channels;
        this.rate = rate;
    }

    @Override
    public Bin build() {
        Element src = ElementFactory.make("wasapisrc", "asrc");
        if (src == null) throw new IllegalStateException("Missing element: wasapisrc");
        try {
            src.set("loopback", loopback);
        } catch (Exception ignored) {
        }

        Element aconv = ElementFactory.make("audioconvert", "aconv");
        Element ares = ElementFactory.make("audioresample", "ares");
        Element acap = ElementFactory.make("capsfilter", "acap");
        Caps caps = Caps.fromString(String.format("audio/x-raw,channels=%d,rate=%d", channels, rate));
        acap.set("caps", caps);

        Element aq = ElementFactory.make("queue", "aq");

        Bin bin = new Bin("audio_source");
        bin.addMany(src, aconv, ares, acap, aq);
        Element.linkMany(src, aconv, ares, acap, aq);
        bin.addPad(new GhostPad("src", aq.getStaticPad("src")));
        return bin;
    }
}

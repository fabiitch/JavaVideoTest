package com.nz.recorder.backend.gst.builder;

import com.nz.recorder.backend.gst.config.settings.GstEncoderSettings;
import com.nz.recorder.backend.gst.source.GstAudioSource;
import com.nz.recorder.backend.gst.source.GstVideoSource;
import com.nz.recorder.backend.gst.recorder.ModularGstRecorder;
import com.nz.recorder.backend.gst.config.settings.GstMuxSettings;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

public class GstRecorderBuilder {
    private GstVideoSource videoSource;
    private GstAudioSource audioSource; // optional
    private GstEncoderSettings encoder = GstEncoderSettings.defaultH264();
    private GstMuxSettings mux = GstMuxSettings.fromExtension(".mp4");
    private File output;
    private Consumer<Throwable> errorHandler = t -> {
    };

    public static GstRecorderBuilder newBuilder() {
        return new GstRecorderBuilder();
    }
    public GstRecorderBuilder video(GstVideoSource src) {
        this.videoSource = src;
        return this;
    }

    public GstRecorderBuilder audio(GstAudioSource src) {
        this.audioSource = src;
        return this;
    }

    public GstRecorderBuilder noAudio() {
        this.audioSource = null;
        return this;
    }

    public GstRecorderBuilder encoder(GstEncoderSettings enc) {
        this.encoder = enc;
        return this;
    }

    public GstRecorderBuilder mux(GstMuxSettings m) {
        this.mux = m;
        return this;
    }

    public GstRecorderBuilder output(File file) {
        this.output = file;
        return this;
    }

    public GstRecorderBuilder onError(Consumer<Throwable> handler) {
        this.errorHandler = handler != null ? handler : t -> {};
        return this;
    }

    public ModularGstRecorder build() {
        Objects.requireNonNull(videoSource, "videoSource is required");
        Objects.requireNonNull(output, "output file is required");
        return new ModularGstRecorder(videoSource, audioSource, encoder, mux, output, errorHandler);
    }
}

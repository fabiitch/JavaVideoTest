package com.nz.recorder.backend.gst.recorder;

import com.nz.recorder.backend.gst.builder.GstChainBuilders;
import com.nz.recorder.backend.gst.builder.GstMuxFactory;
import com.nz.recorder.backend.gst.tt.GstVideoChain;
import com.nz.recorder.backend.gst.config.settings.GstEncoderSettings;
import com.nz.recorder.backend.gst.config.settings.GstMuxSettings;
import com.nz.recorder.backend.gst.source.GstAudioSource;
import com.nz.recorder.backend.gst.source.GstVideoSource;
import com.nz.share.gst.recorder.GstRecorderLog;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.event.EOSEvent;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A modular, builder-style screen recorder on top of gst1-java-core.
 * - Pluggable sources (video, audio)
 * - Pluggable encoder selection (NVENC/x264, etc.)
 * - Pluggable container/mux (mp4/mkv)
 * - Small, cohesive methods (easy to unit-test and extend)
 * <p>
 * You can later split inner classes into files (SourceChain, AudioChain, EncoderFactory, MuxFactory, etc.).
 */
public class ModularGstRecorder {
    private final GstVideoSource videoSource;
    private final GstAudioSource audioSource; // nullable
    private final GstEncoderSettings enc;
    private final GstMuxSettings mux;
    private final File out;
    private final Consumer<Throwable> errorHandler;

    private Pipeline pipeline;
    private Bus bus;
    private CountDownLatch eosLatch;

    public ModularGstRecorder(GstVideoSource videoSource, GstAudioSource audioSource,
                              GstEncoderSettings enc, GstMuxSettings mux, File out,
                              Consumer<Throwable> errorHandler) {
        this.videoSource = videoSource;
        this.audioSource = audioSource;
        this.enc = enc;
        this.mux = mux;
        this.out = out;
        this.errorHandler = errorHandler;
    }

    public synchronized void init() {
        GstRecorderLog.info("Pipeline init start!");
        Pipeline p = new Pipeline("recorder");

        GstVideoChain vChain = GstChainBuilders.buildVideoChain(videoSource, enc);
        GstRecorderLog.info("Pipeline video plan: " + vChain.getDebug());

        Element muxer = GstMuxFactory.build(mux);
        Element fileSink = ElementFactory.make("filesink", "sink");
        fileSink.set("location", out.getAbsolutePath());
        fileSink.set("sync", false);

        p.addMany(vChain.getBin(), muxer, fileSink);
        if (!Element.linkMany(vChain.getBin(), muxer, fileSink)) {
            throw new IllegalStateException("Failed to link video → muxer → filesink");
        }

        if (audioSource != null) {
            Element aChain = GstChainBuilders.buildAudioChain(audioSource, enc);
            p.add(aChain);
            if (!aChain.link(muxer)) {
                throw new IllegalStateException("Failed to link audio → muxer");
            }
        }

        Bus b = p.getBus();
        b.connect((Bus.ERROR) (source, code, message) -> {
            String errorMessage = "[GST][ERROR] " + code + " / " + message;
            GstRecorderLog.error(errorMessage);
            errorHandler.accept(new IllegalStateException(errorMessage));
            stop(); // try tear down
        });
        b.connect((Bus.EOS) (source) -> {
            GstRecorderLog.info("EOS received");
            if (eosLatch != null) eosLatch.countDown();
        });

        this.pipeline = p;
        this.bus = b;
        this.eosLatch = new CountDownLatch(1);
        GstRecorderLog.info("Pipeline init done !");
    }
    public synchronized void start() {
        GstRecorderLog.info("Pipeline start");
        pipeline.play();
    }


    public synchronized void stop() {
        if (pipeline == null) return;
        try {
            // 1) Ask nicely: EOS so muxers can finalize moov/cues (duration)
            try {
                pipeline.sendEvent(new EOSEvent());
            } catch (Exception ignored) {
            }

            // 2) Wait a little for EOS; don't hang forever
            CountDownLatch latch = eosLatch;
            if (latch != null) {
                latch.await(10, TimeUnit.SECONDS);
            }

            // 3) Force teardown
            pipeline.setState(State.NULL);
            try {
                pipeline.getState(TimeUnit.SECONDS.toNanos(5));
            } catch (Exception ignored) {
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            Pipeline p = pipeline;
            pipeline = null;
            Bus b = bus;
            bus = null;
            if (b != null) try {
                b.dispose();
            } catch (Exception ignored) {
            }
            if (p != null) try {
                p.dispose();
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void pause() {
        if (pipeline != null) pipeline.pause();
    }

    public synchronized void resume() {
        if (pipeline != null) pipeline.play();
    }

    public void awaitEos() throws InterruptedException {
        CountDownLatch latch = eosLatch;
        if (latch != null) latch.await();
    }

    public Optional<State> getState() {
        return pipeline == null ? Optional.empty() : Optional.of(pipeline.getState());
    }
}

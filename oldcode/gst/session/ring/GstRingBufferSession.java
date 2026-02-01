//package com.nz.recorder.backend.gst.session.ring;
//
//import com.nz.recorder.api.core.RecorderSettings;
//import com.nz.recorder.api.save.FileSave;
//import com.nz.recorder.api.save.SaveStrategy;
//import com.nz.recorder.api.save.SaveToFileRequest;
//import com.nz.recorder.api.targets.ScreenTarget;
//import com.nz.recorder.backend.gst.config.GstEncoders;
//import com.nz.recorder.backend.gst.config.GstQualityMapper;
//import com.nz.recorder.backend.gst.builder.GstTargetsBuilder;
//import com.nz.recorder.backend.gst.events.GstEventHub;
//import com.nz.recorder.backend.gst.io.GstFiles;
//import com.nz.recorder.backend.gst.io.GstResults;
//import com.nz.recorder.backend.gst.session.GstSession;
//import com.nz.recorder.backend.gst.session.GstSessionMetrics;
//import com.nz.recorder.backend.gst.support.GstSupport;
//import com.nz.share.gst.recorder.GstElementProbe;
//import com.nz.share.gst.recorder.GstVideoPipelinePlanner;
//import org.freedesktop.gstreamer.*;
//import org.freedesktop.gstreamer.event.EOSEvent;
//
//import java.awt.Rectangle;
//import java.io.IOException;
//import java.nio.file.*;
//import java.time.Duration;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
///**
// * Ring-buffer session (v1): uses splitmuxsink to write rotating segments into a temp folder.
// * Snapshot export returns a directory of segments (no concat yet).
// */
//public final class GstRingBufferSession implements GstSession {
//
//    private final RecorderSettings settings;
//    private final GstEventHub events;
//
//    private Pipeline pipeline;
//    private Bus bus;
//    private CountDownLatch eosLatch;
//
//    private Path ringDir;
//    private String segmentPattern;
//    private final GstSessionMetrics metrics = new GstSessionMetrics();
//
//    public GstRingBufferSession(RecorderSettings settings, GstEventHub events) {
//        this.settings = settings;
//        this.events = events;
//    }
//
//    @Override
//    public void start() {
//        if (!(settings.target() instanceof ScreenTarget st)) {
//            throw new UnsupportedOperationException("RingBuffer supports SCREEN only in this backend");
//        }
//
//        this.ringDir = GstFiles.createTempDirectory("gst-ring-");
//        String ext = ringContainerExtension(settings.saveStrategy());
//        this.segmentPattern = ringDir.resolve("segment-%05d" + ext).toString();
//
//        Rectangle bounds = GstTargetsBuilder.resolveScreenBounds(st);
//
//        Pipeline p = new Pipeline("gst-ring-buffer");
//
//        Element src = ElementFactory.make("d3d11screencapturesrc", "vsrc");
//        if (src == null) throw new IllegalStateException("Missing element: d3d11screencapturesrc");
//        GstSupport.trySet(src, "left", bounds.x);
//        GstSupport.trySet(src, "top", bounds.y);
//        GstSupport.trySet(src, "right", bounds.x + bounds.width);
//        GstSupport.trySet(src, "bottom", bounds.y + bounds.height);
//        GstSupport.trySet(src, "framerate", new Fraction(settings.fps(), 1));
//
//        Element queue1 = ElementFactory.make("queue", "q1");
//        if (queue1 == null) throw new IllegalStateException("Missing element: queue");
//
//        int gopSeconds = GstQualityMapper.gopSeconds(settings.quality());
//        GstEncoders.EncoderChain enc = GstEncoders.buildH264Chain(settings.quality(), settings.fps(), gopSeconds);
//        Element encoder = enc.encoder;
//        Element parser = enc.parser;
//
//        GstElementProbe probe = GstElementProbe.defaultProbe();
//        GstVideoPipelinePlanner.Plan plan = GstVideoPipelinePlanner.planD3d11Capture(
//            enc.hardware ? GstVideoPipelinePlanner.EncoderTarget.H264_HARDWARE : GstVideoPipelinePlanner.EncoderTarget.H264_SOFTWARE,
//            bounds.width,
//            bounds.height,
//            settings.fps(),
//            probe
//        );
//
//        StringBuilder pipelinePlan = new StringBuilder("pipeline=vsrc ! caps(")
//            .append(plan.sourceCaps()).append(")");
//        if (plan.useD3d11Convert()) pipelinePlan.append(" ! d3d11convert");
//        if (plan.useD3d11Download()) pipelinePlan.append(" ! d3d11download");
//        if (plan.memoryPath() == GstVideoPipelinePlanner.MemoryPath.SYSTEM) {
//            pipelinePlan.append(" ! videorate");
//        }
//        if (plan.useVideoConvert()) pipelinePlan.append(" ! videoconvert");
//        pipelinePlan.append(" ! caps(").append(plan.encoderCaps()).append(") ! ")
//            .append(enc.hardware ? "nvh264enc" : "x264enc");
//        GstSupport.logInfo("Ring buffer plan: " + plan.reason() + " " + pipelinePlan);
//
//        Element srcCapsFilter = ElementFactory.make("capsfilter", "vsrc_caps");
//        Element convert = plan.useD3d11Convert() ? ElementFactory.make("d3d11convert", "vconv_d3d11") : null;
//        Element download = plan.useD3d11Download() ? ElementFactory.make("d3d11download", "vdownload") : null;
//        Element rate = plan.memoryPath() == GstVideoPipelinePlanner.MemoryPath.SYSTEM
//            ? ElementFactory.make("videorate", "vrate")
//            : null;
//        Element sysConvert = plan.useVideoConvert() ? ElementFactory.make("videoconvert", "vconv_sys") : null;
//        Element encCapsFilter = ElementFactory.make("capsfilter", "venc_caps");
//
//        if (srcCapsFilter == null || encCapsFilter == null) {
//            throw new IllegalStateException("Missing capsfilter for ring buffer pipeline");
//        }
//        if (plan.useD3d11Convert() && convert == null) {
//            throw new IllegalStateException("Missing element: d3d11convert");
//        }
//        if (plan.useD3d11Download() && download == null) {
//            throw new IllegalStateException("Missing element: d3d11download");
//        }
//        if (plan.useVideoConvert() && sysConvert == null) {
//            throw new IllegalStateException("Missing element: videoconvert");
//        }
//        srcCapsFilter.set("caps", Caps.fromString(plan.sourceCaps()));
//        encCapsFilter.set("caps", Caps.fromString(plan.encoderCaps()));
//
//        Element queue2 = ElementFactory.make("queue", "q2");
//        if (queue2 == null) throw new IllegalStateException("Missing element: queue");
//
//        Element splitmux = ElementFactory.make("splitmuxsink", "splitmux");
//        if (splitmux == null) throw new IllegalStateException("Missing element: splitmuxsink");
//
//        long segmentNs = TimeUnit.SECONDS.toNanos(1);
//        int maxFiles = Math.max(1, (int) Math.ceil(maxDur.toMillis() / 1000.0));
//
//        GstSupport.trySet(splitmux, "location", segmentPattern);
//        GstSupport.trySet(splitmux, "max-size-time", segmentNs);
//        GstSupport.trySet(splitmux, "max-files", maxFiles);
//
//        Element muxer = ElementFactory.make(ext.equals(".mp4") ? "mp4mux" : "matroskamux", "smux");
//        if (muxer != null) {
//            GstSupport.trySet(splitmux, "muxer", muxer);
//        }
//
//        p.addMany(src, queue1, srcCapsFilter, encoder, parser, queue2, splitmux);
//        if (convert != null) p.add(convert);
//        if (download != null) p.add(download);
//        if (rate != null) p.add(rate);
//        if (sysConvert != null) p.add(sysConvert);
//        p.add(encCapsFilter);
//
//        Element upstream = src;
//        if (!upstream.link(queue1)) {
//            throw new IllegalStateException("Failed to link source -> queue");
//        }
//        upstream = queue1;
//        if (!upstream.link(srcCapsFilter)) {
//            throw new IllegalStateException("Failed to link queue -> src caps");
//        }
//        upstream = srcCapsFilter;
//        if (convert != null) {
//            if (!upstream.link(convert)) {
//                throw new IllegalStateException("Failed to link d3d11convert");
//            }
//            upstream = convert;
//        }
//        if (download != null) {
//            if (!upstream.link(download)) {
//                throw new IllegalStateException("Failed to link d3d11download");
//            }
//            upstream = download;
//        }
//        if (rate != null) {
//            if (!upstream.link(rate)) {
//                throw new IllegalStateException("Failed to link videorate");
//            }
//            upstream = rate;
//        }
//        if (sysConvert != null) {
//            if (!upstream.link(sysConvert)) {
//                throw new IllegalStateException("Failed to link videoconvert");
//            }
//            upstream = sysConvert;
//        }
//        if (!upstream.link(encCapsFilter)) {
//            throw new IllegalStateException("Failed to link encoder caps");
//        }
//        upstream = encCapsFilter;
//        if (!upstream.link(encoder)) {
//            throw new IllegalStateException("Failed to link encoder");
//        }
//        if (!Element.linkMany(encoder, parser, queue2)) {
//            throw new IllegalStateException("Failed to link encoder -> parser -> queue");
//        }
//        if (!queue2.link(splitmux)) {
//            throw new IllegalStateException("Failed to link queue2 -> splitmuxsink");
//        }
//
//        this.eosLatch = new CountDownLatch(1);
//        this.bus = p.getBus();
//        bus.connect((Bus.ERROR) (srcEl, code, message) -> {
//            String errorMessage = "[GST][ERROR] " + code + " / " + message;
//            GstSupport.logError(errorMessage);
//            events.emitError(new RuntimeException(errorMessage));
//            if (eosLatch != null) eosLatch.countDown();
//        });
//        bus.connect((Bus.EOS) (srcEl) -> {
//            if (eosLatch != null) eosLatch.countDown();
//        });
//
//        this.pipeline = p;
//        metrics.markStart();
//        p.play();
//        events.emitStats(GstResults.statsRequestedOnly(settings));
//    }
//
//    @Override
//    public void pause() {
//        if (pipeline != null) {
//            pipeline.pause();
//        }
//    }
//
//    @Override
//    public void resume() {
//        if (pipeline != null) {
//            pipeline.play();
//        }
//    }
//
//    @Override
//    public Optional<Path> stopAndFinalize() {
//        if (pipeline == null) return Optional.empty();
//        try {
//            try {
//                pipeline.sendEvent(new EOSEvent());
//            } catch (Exception ignored) {
//            }
//            try {
//                if (eosLatch != null) eosLatch.await(10, TimeUnit.SECONDS);
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//            }
//            pipeline.setState(State.NULL);
//            try {
//                pipeline.getState(TimeUnit.SECONDS.toNanos(5));
//            } catch (Exception ignored) {
//            }
//            metrics.markStop();
//            metrics.setSegmentsProduced(countSegmentsSafe());
//            GstSupport.logInfo("Ring buffer segments produced=" + metrics.segmentsProduced());
//            return Optional.ofNullable(ringDir);
//        } finally {
//            dispose();
//        }
//    }
//
//    private void dispose() {
//        Pipeline p = pipeline;
//        pipeline = null;
//        Bus b = bus;
//        bus = null;
//        if (b != null) {
//            try {
//                b.dispose();
//            } catch (Exception ignored) {
//            }
//        }
//        if (p != null) {
//            try {
//                p.dispose();
//            } catch (Exception ignored) {
//            }
//        }
//    }
//
//    private long countSegmentsSafe() {
//        try {
//            return GstFiles.listSegments(ringDir).size();
//        } catch (IOException e) {
//            return 0L;
//        }
//    }
//
//    private String ringContainerExtension(SaveStrategy strategy) {
//        if (strategy instanceof FileSave fs) {
//            return GstFiles.extension(fs.output().file());
//        }
//        return ".mkv";
//    }
//
//    @Override
//    public GstSessionMetrics metrics() {
//        return metrics;
//    }
//}

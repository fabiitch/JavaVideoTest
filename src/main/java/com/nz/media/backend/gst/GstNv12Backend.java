package com.nz.media.backend.gst;

import com.nz.media.backend.BackendState;
import com.nz.media.frame.nv12.Nv12Frame;
import com.nz.media.metrics.VideoMetricsSnapshot;
import com.nz.share.gst.GstUtils;
import org.freedesktop.gstreamer.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GStreamer NV12 decode backend (decode-only, no LibGDX/GL).
 * Produces pooled Nv12Frame (tightly packed) through VideoFrame + Nv12CpuView via views.
 * <p>
 * Pooling is backend-owned: consumer MUST call recycle(frame) after upload.
 */
public final class GstNv12Backend extends AbstractGstAppSinkReaderBackend<Nv12Frame> {

    private final Stats stats = new Stats();
    private final AtomicInteger lastWidth = new AtomicInteger();
    private final AtomicInteger lastHeight = new AtomicInteger();
    private final com.nz.media.metrics.VideoMetricsProvider metricsProvider = this::buildSnapshot;

    public GstNv12Backend() {
        this(4);
    }

    public GstNv12Backend(int poolSize) {
        super(poolSize);
    }

    @Override
    public void open(String path) {
        state.set(BackendState.OPENING);
        lastError = null;
        stats.reset();
        if (pipeline != null) throw new IllegalStateException("Backend already opened");
        initPool(Nv12Frame::new);
        openPipeline(path);
    }

    @Override
    public long getDurationNs() {
        if (pipeline == null) return -1;
        return pipeline.queryDuration(Format.TIME);
    }

    @Override
    public long getPositionNs() {
        if (pipeline == null) return 0;
        return pipeline.queryPosition(Format.TIME);
    }

    @Override
    public Optional<com.nz.media.metrics.VideoMetricsProvider> metrics() {
        return Optional.of(metricsProvider);
    }

    @Override
    protected Pipeline buildPipeline(String path) {
        String uri = filePathToUri(path);

        String pipeStr = """
            uridecodebin uri=%s name=d \
            d. ! queue ! videoconvert ! video/x-raw,format=NV12 ! \
            appsink name=mysink sync=true max-buffers=2 drop=true emit-signals=true \
            d. ! queue ! audioconvert ! audioresample ! \
            volume name=vol volume=1.0 mute=false ! autoaudiosink sync=true""".formatted(uri);

        Element e = Gst.parseLaunch(pipeStr);
        if (!(e instanceof Pipeline)) {
            throw new IllegalStateException("parseLaunch did not return a Pipeline");
        }
        return GstUtils.getPipeline(e);
    }

    @Override
    protected String appSinkCaps() {
        return "video/x-raw,format=NV12";
    }

    @Override
    protected Nv12Frame decodeSample(Sample sample) {
        Buffer buffer = sample.getBuffer();
        Caps caps = sample.getCaps();
        if (caps == null || buffer == null) {
            stats.droppedFramesBadCaps.incrementAndGet();
            return null;
        }

        Structure s = caps.getStructure(0);
        if (s == null || !s.hasField("width") || !s.hasField("height")) {
            stats.droppedFramesBadCaps.incrementAndGet();
            return null;
        }
        int w = s.getInteger("width");
        int h = s.getInteger("height");
        if (w <= 0 || h <= 0) {
            stats.droppedFramesBadCaps.incrementAndGet();
            return null;
        }
        lastWidth.set(w);
        lastHeight.set(h);

        Nv12Frame out = obtainFrame(w, h);
        if (out == null) return null;

        out.ensureCapacity(w, h);

        try {
            fillFrameFromSample(out, sample);
        } catch (FrameDropException ignored) {
            recycleTyped(out);
            return null;
        }
        return out;
    }

    @Override
    protected Nv12Frame obtainFrame(int width, int height) {
        Nv12Frame out = pool.poll();
        if (out == null) {
            stats.droppedFramesPoolEmpty.incrementAndGet();
            return null;
        }
        return out;
    }

    @Override
    protected void fillFrameFromSample(Nv12Frame out, Sample sample) {
        Buffer buffer = sample.getBuffer();
        Caps caps = sample.getCaps();
        Structure s = caps == null ? null : caps.getStructure(0);
        int w = s == null ? 0 : s.getInteger("width");
        int h = s == null ? 0 : s.getInteger("height");

        ByteBuffer data = buffer.map(false);
        if (data == null) {
            stats.droppedFramesMapFailed.incrementAndGet();
            dropFrame();
        }

        try {
            int ySize = w * h;
            int uvSize = w * (h / 2);
            int needed = ySize + uvSize;

            if (data.remaining() < needed) {
                stats.droppedFramesSizeMismatch.incrementAndGet();
                dropFrame();
            }

            out.y().clear();
            ByteBuffer ySrc = data.duplicate();
            ySrc.position(0).limit(ySize);
            out.y().put(ySrc);
            out.y().flip();

            out.uv().clear();
            ByteBuffer uvSrc = data.duplicate();
            uvSrc.position(ySize).limit(ySize + uvSize);
            out.uv().put(uvSrc);
            out.uv().flip();

            long pts = buffer.getPresentationTimestamp();
            if (pts < 0) pts = 0;

            out.setWidth(w);
            out.setHeight(h);
            out.setPtsNs(pts);
            out.setYStride(w);
            out.setUvStride(w);
        } finally {
            buffer.unmap();
        }
    }

    @Override
    protected void recycleTyped(Nv12Frame frame) {
        frame.reset();
        pool.offer(frame);
    }

    @Override
    protected void onNullSample() {
        stats.onNullSample();
    }

    @Override
    protected void onDecodeTime(long decodeNs) {
        stats.onDecodeTime(decodeNs);
    }

    @Override
    protected void onFrameReceived(Nv12Frame frame) {
        stats.onFrameReceived();
    }

    @Override
    protected boolean shouldLogSpeedSeek() {
        return true;
    }

    private VideoMetricsSnapshot buildSnapshot() {
        Optional<Integer> width = optionalPositiveInt(lastWidth.get());
        Optional<Integer> height = optionalPositiveInt(lastHeight.get());

        long durationNs = pipeline == null ? -1L : pipeline.queryDuration(Format.TIME);
        long positionNs = pipeline == null ? -1L : pipeline.queryPosition(Format.TIME);

        long droppedDecoderFrames = stats.getDroppedFramesPoolEmpty()
            + stats.getDroppedFramesBadCaps()
            + stats.getDroppedFramesMapFailed()
            + stats.getDroppedFramesSizeMismatch();

        return new VideoMetricsSnapshot(
            Optional.of("GstNv12Backend"),
            Optional.empty(),
            Optional.empty(),
            width,
            height,
            Optional.of("NV12"),
            Optional.of(state.get() == BackendState.PLAYING),
            nsToMs(positionNs),
            nsToMs(durationNs),
            Optional.of(speed),
            Optional.of(volume),
            Optional.empty(),
            Optional.empty(),
            Optional.of(stats.getOutputFps()),
            Optional.of(droppedDecoderFrames),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(latest.get() == null ? 0 : 1),
            avgMs(stats.getDecodeTimeNsTotal(), stats.getDecodeTimeCount()),
            maxMs(stats.getDecodeTimeNsMax()),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            List.of()
        );
    }

    private static final class Stats {
        private final AtomicLong droppedFramesPoolEmpty = new AtomicLong();
        private final AtomicLong droppedFramesBadCaps = new AtomicLong();
        private final AtomicLong droppedFramesMapFailed = new AtomicLong();
        private final AtomicLong droppedFramesSizeMismatch = new AtomicLong();
        private final AtomicLong decodeTimeNsTotal = new AtomicLong();
        private final AtomicLong decodeTimeCount = new AtomicLong();
        private final AtomicLong decodeTimeNsMax = new AtomicLong();
        private final Object fpsLock = new Object();
        private long fpsWindowStartNs;
        private int fpsCount;
        private volatile double outputFps;
        private final AtomicLong nullSamples = new AtomicLong();

        private void reset() {
            droppedFramesPoolEmpty.set(0L);
            droppedFramesBadCaps.set(0L);
            droppedFramesMapFailed.set(0L);
            droppedFramesSizeMismatch.set(0L);
            decodeTimeNsTotal.set(0L);
            decodeTimeCount.set(0L);
            decodeTimeNsMax.set(0L);
            nullSamples.set(0L);
            outputFps = 0.0;
            synchronized (fpsLock) {
                fpsWindowStartNs = 0L;
                fpsCount = 0;
            }
        }

        private void onFrameReceived() {
            long now = System.nanoTime();
            synchronized (fpsLock) {
                if (fpsWindowStartNs == 0L) {
                    fpsWindowStartNs = now;
                }
                fpsCount++;
                if (now - fpsWindowStartNs >= 1_000_000_000L) {
                    outputFps = fpsCount;
                    fpsCount = 0;
                    fpsWindowStartNs = now;
                }
            }
        }

        private void onNullSample() {
            nullSamples.incrementAndGet();
        }

        private void onDecodeTime(long decodeNs) {
            if (decodeNs <= 0L) return;
            decodeTimeNsTotal.addAndGet(decodeNs);
            decodeTimeCount.incrementAndGet();
            updateMax(decodeTimeNsMax, decodeNs);
        }

        public long getDroppedFramesPoolEmpty() {
            return droppedFramesPoolEmpty.get();
        }

        public long getDroppedFramesBadCaps() {
            return droppedFramesBadCaps.get();
        }

        public long getDroppedFramesMapFailed() {
            return droppedFramesMapFailed.get();
        }

        public long getDroppedFramesSizeMismatch() {
            return droppedFramesSizeMismatch.get();
        }

        public long getDecodeTimeNsTotal() {
            return decodeTimeNsTotal.get();
        }

        public long getDecodeTimeCount() {
            return decodeTimeCount.get();
        }

        public long getDecodeTimeNsMax() {
            return decodeTimeNsMax.get();
        }

        public double getOutputFps() {
            return outputFps;
        }
    }

    private static void updateMax(AtomicLong maxTarget, long value) {
        long prev;
        do {
            prev = maxTarget.get();
            if (value <= prev) return;
        } while (!maxTarget.compareAndSet(prev, value));
    }
}

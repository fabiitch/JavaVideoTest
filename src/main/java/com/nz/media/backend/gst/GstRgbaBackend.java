package com.nz.media.backend.gst;

import com.nz.media.backend.BackendState;
import com.nz.media.frame.rgba.RgbaFrame;
import com.nz.media.metrics.VideoMetricsSnapshot;
import com.nz.media.render.ReaderFpsCounter;
import com.nz.share.gst.GstUtils;
import org.freedesktop.gstreamer.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GStreamer RGBA decode backend (decode-only, no LibGDX/GL).
 * Produces pooled RgbaFrame (tightly packed) through VideoFrame + RgbaCpuView via views.
 * Ownership: consumer must call {@link #recycle(com.nz.media.frame.VideoFrame)} when done with a frame.
 */
public final class GstRgbaBackend extends AbstractGstAppSinkReaderBackend<RgbaFrame> {

    private Element audioSink;

    private boolean lowLatency = true;
    private boolean audioSync = true;
    private boolean copyOnRead = false;

    private volatile long lastErrorLogNs = 0L;

    private static final long ERROR_LOG_INTERVAL_NS = 5_000_000_000L;
    private final ReaderFpsCounter decodeFpsCounter = new ReaderFpsCounter();
    private final Stats stats = new Stats();
    private final AtomicInteger lastWidth = new AtomicInteger();
    private final AtomicInteger lastHeight = new AtomicInteger();
    private final com.nz.media.metrics.VideoMetricsProvider metricsProvider = this::buildSnapshot;

    public GstRgbaBackend() {
        this(4);
    }

    public GstRgbaBackend(int poolSize) {
        super(poolSize);
    }

    @Override
    public void open(String path) {
        state.set(BackendState.OPENING);
        lastError = null;
        stats.reset();
        if (pipeline != null) throw new IllegalStateException("Backend already opened");
        initPool(RgbaFrame::new);
        openPipeline(path);
    }

    @Override
    public void close() {
        super.close();
        audioSink = null;
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

    public Stats getStats() {
        return stats;
    }

    public void setLowLatency(boolean lowLatency) {
        this.lowLatency = lowLatency;
        if (appSink != null) {
            configureAppSink(appSink);
        }
    }

    public boolean isLowLatency() {
        return lowLatency;
    }

    public void setAudioSync(boolean audioSync) {
        this.audioSync = audioSync;
        if (audioSink != null) {
            audioSink.set("sync", audioSync);
        }
    }

    public boolean isAudioSync() {
        return audioSync;
    }

    public void setCopyOnRead(boolean copyOnRead) {
        this.copyOnRead = copyOnRead;
    }

    public boolean isCopyOnRead() {
        return copyOnRead;
    }

    @Override
    protected Pipeline buildPipeline(String path) {
        String uri = filePathToUri(path);
        boolean appSinkSync = !lowLatency;
        int maxBuffers = lowLatency ? 2 : 4;
        boolean drop = lowLatency;

        String pipeStr = """
            uridecodebin uri=%s name=d \
            d. ! queue ! videoconvert ! video/x-raw,format=RGBA ! \
            appsink name=mysink sync=%b max-buffers=%d drop=%b emit-signals=true \
            d. ! queue ! audioconvert ! audioresample ! \
            volume name=vol volume=1.0 mute=false ! autoaudiosink name=audiosink sync=%b"""
            .formatted(uri, appSinkSync, maxBuffers, drop, audioSync);

        Element e = Gst.parseLaunch(pipeStr);
        if (!(e instanceof Pipeline)) {
            throw new IllegalStateException("parseLaunch did not return a Pipeline");
        }
        return GstUtils.getPipeline(e);
    }

    @Override
    protected void onPipelineBuilt(Pipeline p) {
        super.onPipelineBuilt(p);
        audioSink = p.getElementByName("audiosink");
        if (audioSink != null) {
            audioSink.set("sync", audioSync);
        }
    }

    @Override
    protected String appSinkCaps() {
        return "video/x-raw,format=RGBA";
    }

    @Override
    protected boolean dropOldFrames() {
        return lowLatency;
    }

    @Override
    protected int maxBuffers() {
        return lowLatency ? 2 : 4;
    }

    @Override
    protected boolean sync() {
        return !lowLatency;
    }

    @Override
    protected RgbaFrame decodeSample(Sample sample) {
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

        String format = s.getString("format");
        if (!"RGBA".equalsIgnoreCase(format)) {
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

        RgbaFrame out = obtainFrame(w, h);
        if (out == null) return null;

        long reallocBefore = out.getReallocCount();
        out.ensureCapacity(w, h);
        long reallocAfter = out.getReallocCount();
        long reallocDelta = Math.max(0L, reallocAfter - reallocBefore);

        try {
            fillFrameFromSample(out, sample);
        } catch (FrameDropException ignored) {
            recycleTyped(out);
            return null;
        }

        stats.onDecodedFrame(decodeFpsCounter, reallocDelta);
        return out;
    }

    @Override
    protected RgbaFrame obtainFrame(int width, int height) {
        RgbaFrame out = pool.poll();
        if (out == null) {
            stats.droppedFramesPoolEmpty.incrementAndGet();
            return null;
        }
        return out;
    }

    @Override
    protected void fillFrameFromSample(RgbaFrame out, Sample sample) {
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
            int rowBytes = w * 4;
            int stride = resolveStrideBytes(s, w, h, data);
            if (stride <= 0 || stride < rowBytes) {
                stats.droppedFramesStrideMismatch.incrementAndGet();
                dropFrame();
            }

            int required = stride * h;
            int needed = rowBytes * h;
            if (data.remaining() < required || data.remaining() < needed) {
                stats.droppedFramesStrideMismatch.incrementAndGet();
                dropFrame();
            }

            out.clearBufferForWrite();
            ByteBuffer dst = out.rgba();
            ByteBuffer src = data.duplicate();
            if (stride == rowBytes) {
                src.limit(src.position() + needed);
                dst.put(src);
            } else {
                int srcLimit = src.limit();
                for (int y = 0; y < h; y++) {
                    int srcPos = y * stride;
                    int dstPos = y * rowBytes;
                    if (srcPos + rowBytes > srcLimit) {
                        stats.droppedFramesStrideMismatch.incrementAndGet();
                        dropFrame();
                    }
                    src.position(srcPos).limit(srcPos + rowBytes);
                    dst.position(dstPos);
                    dst.put(src);
                }
                dst.position(rowBytes * h);
            }
            out.prepareForRead();

            long pts = buffer.getPresentationTimestamp();
            if (pts < 0) pts = 0;

            out.setWidth(w);
            out.setHeight(h);
            out.setPtsNs(pts);
            out.setStrideBytes(rowBytes);
        } finally {
            buffer.unmap();
        }
    }

    @Override
    protected void recycleTyped(RgbaFrame frame) {
        frame.reset();
        pool.offer(frame);
    }

    @Override
    protected void onSampleError(Throwable t) {
        lastError = t;
        stats.onError(t);
        maybeLogError(t);
    }

    @Override
    protected void onDecodeTime(long decodeNs) {
        stats.onDecodeTime(decodeNs);
    }

    private int resolveStrideBytes(Structure s, int w, int h, ByteBuffer data) {
        int stride = 0;
        if (s != null) {
            if (s.hasField("stride")) {
                stride = s.getInteger("stride");
            } else if (s.hasField("rowstride")) {
                stride = s.getInteger("rowstride");
            }
        }
        if (stride <= 0 && data != null && h > 0) {
            int remaining = data.remaining();
            stride = remaining / h;
        }
        return stride;
    }

    private void maybeLogError(Throwable t) {
        long now = System.nanoTime();
        if (now - lastErrorLogNs < ERROR_LOG_INTERVAL_NS) return;
        lastErrorLogNs = now;
        System.err.println("GstRgbaBackend error: " + t.getMessage());
    }

    public static final class Stats {
        private final AtomicLong droppedFramesPoolEmpty = new AtomicLong();
        private final AtomicLong droppedFramesBadCaps = new AtomicLong();
        private final AtomicLong droppedFramesMapFailed = new AtomicLong();
        private final AtomicLong droppedFramesStrideMismatch = new AtomicLong();
        private final AtomicLong reallocCount = new AtomicLong();
        private final AtomicLong errorCount = new AtomicLong();
        private final AtomicLong decodeTimeNsTotal = new AtomicLong();
        private final AtomicLong decodeTimeCount = new AtomicLong();
        private final AtomicLong decodeTimeNsMax = new AtomicLong();
        private volatile int decodeFps = 0;
        private volatile int uploadFps = 0;
        private volatile Throwable lastError;

        private void reset() {
            droppedFramesPoolEmpty.set(0L);
            droppedFramesBadCaps.set(0L);
            droppedFramesMapFailed.set(0L);
            droppedFramesStrideMismatch.set(0L);
            reallocCount.set(0L);
            errorCount.set(0L);
            decodeTimeNsTotal.set(0L);
            decodeTimeCount.set(0L);
            decodeTimeNsMax.set(0L);
            decodeFps = 0;
            uploadFps = 0;
            lastError = null;
        }

        private void onDecodedFrame(ReaderFpsCounter fpsCounter, long reallocDelta) {
            fpsCounter.newFrame();
            decodeFps = fpsCounter.getFps();
            if (reallocDelta > 0) {
                reallocCount.addAndGet(reallocDelta);
            }
        }

        private void onError(Throwable t) {
            lastError = t;
            errorCount.incrementAndGet();
        }

        private void onDecodeTime(long decodeNs) {
            if (decodeNs <= 0L) return;
            decodeTimeNsTotal.addAndGet(decodeNs);
            decodeTimeCount.incrementAndGet();
            updateMax(decodeTimeNsMax, decodeNs);
        }

        public int getDecodeFps() {
            return decodeFps;
        }

        public int getUploadFps() {
            return uploadFps;
        }

        public void setUploadFps(int uploadFps) {
            this.uploadFps = uploadFps;
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

        public long getDroppedFramesStrideMismatch() {
            return droppedFramesStrideMismatch.get();
        }

        public long getReallocCount() {
            return reallocCount.get();
        }

        public long getErrorCount() {
            return errorCount.get();
        }

        public Throwable getLastError() {
            return lastError;
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
    }

    private static void updateMax(AtomicLong maxTarget, long value) {
        long prev;
        do {
            prev = maxTarget.get();
            if (value <= prev) return;
        } while (!maxTarget.compareAndSet(prev, value));
    }

    private VideoMetricsSnapshot buildSnapshot() {
        List<String> notes = new ArrayList<>();
        notes.add("lowLatency=" + lowLatency);
        notes.add("audioSync=" + audioSync);
        notes.add("copyOnRead=" + copyOnRead);

        Optional<Integer> width = optionalPositiveInt(lastWidth.get());
        Optional<Integer> height = optionalPositiveInt(lastHeight.get());

        long durationNs = pipeline == null ? -1L : pipeline.queryDuration(Format.TIME);
        long positionNs = pipeline == null ? -1L : pipeline.queryPosition(Format.TIME);

        long droppedDecoderFrames = stats.getDroppedFramesPoolEmpty()
            + stats.getDroppedFramesBadCaps()
            + stats.getDroppedFramesMapFailed()
            + stats.getDroppedFramesStrideMismatch();

        return new VideoMetricsSnapshot(
            Optional.of("GstRgbaBackend"),
            Optional.empty(),
            Optional.empty(),
            width,
            height,
            Optional.of("RGBA"),
            Optional.of(state.get() == BackendState.PLAYING),
            nsToMs(positionNs),
            nsToMs(durationNs),
            Optional.of(speed),
            Optional.of(volume),
            Optional.empty(),
            Optional.empty(),
            Optional.of((double) stats.getDecodeFps()),
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
            List.copyOf(notes)
        );
    }
}

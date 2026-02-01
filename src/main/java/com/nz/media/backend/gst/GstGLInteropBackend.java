package com.nz.media.backend.gst;

import com.badlogic.gdx.Gdx;
import com.nz.media.backend.BackendState;
import com.nz.media.backend.gst.nativeinterop.GstAddr;
import com.nz.media.backend.gst.nativeinterop.GstMiniObjectPtr;
import com.nz.media.backend.gst.nativeinterop.GstNativePanama;
import com.nz.media.frame.VideoFrame;
import com.nz.media.frame.external.GlTextureFrame;
import com.nz.media.metrics.VideoMetricsSnapshot;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.message.Message;
import org.freedesktop.gstreamer.message.MessageType;
import org.freedesktop.gstreamer.message.NeedContextMessage;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Optional;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * GStreamer backend that outputs GPU textures (GLMemory) to be consumed by LibGDX
 * WITHOUT CPU pixel copies (no map(), no ByteBuffer upload).
 * <p>
 * Pipeline: uridecodebin -> glupload -> glcolorconvert -> video/x-raw(memory:GLMemory),format=RGBA -> appsink
 * <p>
 * IMPORTANT:
 * - This does NOT guarantee the textureId is usable in LWJGL's GL context.
 * If contexts are not shared, you'll see black when drawing.
 */
public final class GstGLInteropBackend extends AbstractGstBackendBase {

    private final AtomicReference<GlTextureFrame> latest = new AtomicReference<>();
    private final Deque<GlTextureFrame> pool = new ArrayDeque<>(8);

    private volatile long sharedGlContextAddr;
    private volatile long sharedGlDisplayAddr;
    private volatile long sharedGstAppContextAddr;
    private final AtomicInteger lastWidth = new AtomicInteger();
    private final AtomicInteger lastHeight = new AtomicInteger();
    private final Stats stats = new Stats();
    private final com.nz.media.metrics.VideoMetricsProvider metricsProvider = this::buildSnapshot;

    @Override
    public void open(String path) {
        close();
        state.set(BackendState.OPENING);
        stats.reset();
        openPipeline(path);
        refreshPositionAndDuration();
        state.set(BackendState.READY);
    }

    @Override
    public void close() {
        try {
            if (pipeline != null) {
                pipeline.setState(State.NULL);
                pipeline.dispose();
            }
        } catch (Throwable ignored) {
        } finally {
            pipeline = null;
            appSink = null;
            volumeElement = null;
            durationNs = 0L;
            positionNs = 0L;
            latest.set(null);
            state.set(BackendState.ENDED);
        }
    }

    @Override
    public void play() {
        if (pipeline == null) return;
        pipeline.setState(State.PLAYING);
        state.set(BackendState.PLAYING);
    }

    @Override
    public void pause() {
        if (pipeline == null) return;
        pipeline.setState(State.PAUSED);
        state.set(BackendState.PAUSED);
    }

    @Override
    public void stop() {
        if (pipeline == null) return;
        pipeline.setState(State.READY);
        state.set(BackendState.READY);
    }

    @Override
    public void seekNs(long positionNs) {
        if (pipeline == null) return;
        BackendState prevState = state.get();
        long target = Math.max(0L, positionNs);

        boolean seek = sendSeek(target, speed, DEFAULT_SEEK_FLAGS);
        state.set(getState() == BackendState.SEEKING ? BackendState.READY : prevState);
        if (!seek) {
            System.err.println("[GstGLInteroptBackend] seek failed");
        }
    }

    @Override
    public double getVolume() {
        return volume;
    }

    @Override
    public void setVolume(double volume) {
        this.volume = clamp(volume, 0.0, 1.0);
        applyVolumeUnsafe();
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        applyVolumeUnsafe();
    }

    @Override
    public long getDurationNs() {
        if (pipeline != null && durationNs == 0L) refreshPositionAndDuration();
        return durationNs;
    }

    @Override
    public long getPositionNs() {
        if (pipeline != null) refreshPositionAndDuration();
        return positionNs;
    }

    @Override
    public VideoFrame pollLatestFrame() {
        return latest.getAndSet(null);
    }

    @Override
    public void recycle(VideoFrame frame) {
        if (frame == null) return;
        if (frame instanceof GlTextureFrame f) {
            f.reset();
            pool.offerFirst(f);
        }
    }

    @Override
    public Optional<com.nz.media.metrics.VideoMetricsProvider> metrics() {
        return Optional.of(metricsProvider);
    }

    @Override
    protected Pipeline buildPipeline(String path) {
        final String uri = toUri(path);

        final String pipeStr = """
            uridecodebin uri=%s name=d
            d. ! queue ! videoconvert ! glupload ! glcolorconvert !
                 video/x-raw(memory:GLMemory),format=RGBA !
                 appsink name=mysink sync=true max-buffers=2 drop=true emit-signals=true
            d. ! queue ! audioconvert ! audioresample !
                 volume name=vol volume=1.0 mute=false ! autoaudiosink sync=true
            """.formatted(uri);

        return (Pipeline) Gst.parseLaunch(pipeStr);
    }

    @Override
    protected AppSink findAppSink(Pipeline p) {
        Element e = p.getElementByName("mysink");
        if (e instanceof AppSink s) return s;
        return (AppSink) e;
    }

    @Override
    protected void onPipelineBuilt(Pipeline p) {
        if (appSink == null) {
            state.set(BackendState.ERROR);
            throw new IllegalStateException("GStreamer: appsink 'mysink' not found");
        }

        appSink.setCaps(Caps.fromString("video/x-raw(memory:GLMemory),format=RGBA"));

        appSink.connect((AppSink.NEW_SAMPLE) elem -> {
            Sample sample = null;
            try {
                sample = appSink.pullSample();
                if (sample == null) {
                    stats.onNullSample();
                    return FlowReturn.OK;
                }
                GlTextureFrame frame = null;
                try {
                    long decodeStartNs = System.nanoTime();
                    frame = decodeGlFrame(sample);
                    stats.onDecodeTime(System.nanoTime() - decodeStartNs);
                } catch (Throwable t) {
                    stats.onFailedDecode();
                    System.err.println("frame decode failed =" + t.getMessage());
                    return FlowReturn.OK;
                }

                if (frame != null) {
                    stats.onFrameReceived();
                    final GlTextureFrame prev = latest.getAndSet(frame);
                    if (prev != null) recycle(prev);
                }
                return FlowReturn.OK;

            } catch (Throwable t) {
                t.printStackTrace();
                state.set(BackendState.ERROR);
                return FlowReturn.ERROR;
            } finally {
                if (sample != null) sample.dispose();
            }
        });

        createSharedGlContextOnGdxThreadOrThrow();
        installNeedContextHandler();
    }

    @Override
    protected void onBusError(Throwable t) {
        System.err.println("[GstGLInteroptBackend] " + t.getMessage());
        state.set(BackendState.ERROR);
    }

    @Override
    protected void onEos() {
        state.set(BackendState.ENDED);
    }

    @Override
    protected boolean shouldLogSpeedSeek() {
        return true;
    }

    private void installNeedContextHandler() {
        Bus bus = pipeline.getBus();

        bus.connect((Bus.MESSAGE) (Bus b, Message msg) -> {
            if (msg.getType() != MessageType.NEED_CONTEXT) return;

            NeedContextMessage need = (NeedContextMessage) msg;
            String ctxType = need.getContextType();

            if (!"gst.gl.app_context".equals(ctxType)) return;

            GstObject src = msg.getSource();
            long srcAddr = GstAddr.addr(src);

            long appCtxAddr = sharedGstAppContextAddr;
            if (appCtxAddr == 0L) {
                System.err.println("[GStreamer-GL] NEED_CONTEXT but appCtxAddr=0");
                return;
            }

            GstNativePanama.gstElementSetContext(
                MemorySegment.ofAddress(srcAddr),
                MemorySegment.ofAddress(appCtxAddr)
            );

            System.out.println("[GStreamer-GL] injected gst.gl.app_context into " + src + " addr=0x" + Long.toHexString(srcAddr));
        });
    }

    private GlTextureFrame decodeGlFrame(Sample sample) {
        Caps caps = sample.getCaps();
        Buffer buffer = sample.getBuffer();
        if (caps == null || buffer == null) {
            stats.onNullSample();
            return null;
        }

        Structure st = caps.getStructure(0);
        int w = st.hasField("width") ? st.getInteger("width") : 0;
        int h = st.hasField("height") ? st.getInteger("height") : 0;
        if (w > 0 && h > 0) {
            lastWidth.set(w);
            lastHeight.set(h);
        }

        long gstBufAddr = GstMiniObjectPtr.address(buffer);
        System.out.println("gstBuffer* = 0x" + Long.toHexString(gstBufAddr));
        if (gstBufAddr == 0L) {
            stats.onUnsupportedSample();
            return null;
        }

        MemorySegment gstBuf = GstNativePanama.ptr(gstBufAddr);
        System.out.println("gstBuf.size = " + gstBuf.byteSize());
        MemorySegment mem0 = GstNativePanama.bufferPeekMemory(gstBuf, 0);
        if (mem0 == null || mem0.address() == 0) {
            stats.onUnsupportedSample();
            return null;
        }

        if (!GstNativePanama.isGlMemory(mem0)) {
            stats.onUnsupportedSample();
            return null;
        }

        int texId = GstNativePanama.glMemoryGetTextureId(mem0);
        System.out.println("caps=" + caps);
        System.out.println("texId=" + texId);
        if (texId <= 0) {
            stats.onUnsupportedSample();
            return null;
        }

        MemorySegment gstBufStruct = GstNativePanama.gstBufferStruct(gstBufAddr);
        long pts = GstNativePanama.bufferGetPts(gstBufStruct);

        GlTextureFrame out = pool.pollFirst();
        if (out == null) {
            stats.onPoolEmpty();
            out = new GlTextureFrame();
        }
        out.set(texId, w, h, pts);
        return out;
    }

    private static String toUri(String path) {
        if (path.startsWith("file:") || path.startsWith("http:") || path.startsWith("https:")) return path;
        return new File(path).toURI().toString();
    }

    private void createSharedGlContextOnGdxThreadOrThrow() {
        if (sharedGlContextAddr != 0L) return;

        var latch = new java.util.concurrent.CountDownLatch(1);
        var err = new java.util.concurrent.atomic.AtomicReference<Throwable>();

        Gdx.app.postRunnable(() -> {
            try {
                long mainWindow = GLFW.glfwGetCurrentContext();
                if (mainWindow == 0) throw new IllegalStateException("No current GLFW context");

                GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
                GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
                GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);

                long sharedWindow = GLFW.glfwCreateWindow(1, 1, "gst-share", 0, mainWindow);
                if (sharedWindow == 0) throw new IllegalStateException("Failed to create shared hidden GLFW window");

                GLFW.glfwMakeContextCurrent(sharedWindow);
                GLFW.glfwMakeContextCurrent(mainWindow);
                long hglrcShared = org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext(sharedWindow);
                if (hglrcShared == 0L) throw new IllegalStateException("shared HGLRC=0");

                var display = GstNativePanama.gstGlDisplayNew();
                if (display == null || display.equals(java.lang.foreign.MemorySegment.NULL)) {
                    throw new IllegalStateException("gst_gl_display_new returned NULL");
                }

                var wrapped = GstNativePanama.gstGlContextNewWrapped(
                    display,
                    hglrcShared,
                    GstNativePanama.GST_GL_PLATFORM_WGL,
                    GstNativePanama.GST_GL_API_OPENGL
                );
                if (wrapped == null || wrapped.equals(java.lang.foreign.MemorySegment.NULL)) {
                    throw new IllegalStateException("gst_gl_context_new_wrapped returned NULL");
                }

                sharedGlDisplayAddr = display.address();
                sharedGlContextAddr = wrapped.address();

                System.out.println("[GStreamer-GL] wrapped HGLRC=0x" + Long.toHexString(hglrcShared)
                    + " ctx=0x" + Long.toHexString(sharedGlContextAddr));

                try (var arena = java.lang.foreign.Arena.ofConfined()) {
                    java.lang.foreign.MemorySegment appCtx = GstNativePanama.makeGlAppContext(wrapped, arena);

                    if (appCtx == null || appCtx.equals(java.lang.foreign.MemorySegment.NULL)) {
                        throw new IllegalStateException("makeGlAppContext returned NULL");
                    }

                    sharedGlDisplayAddr = display.address();
                    sharedGlContextAddr = wrapped.address();
                    sharedGstAppContextAddr = appCtx.address();

                    System.out.println("[GStreamer-GL] appCtx=0x" + Long.toHexString(sharedGstAppContextAddr));
                }

            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        if (err.get() != null) throw new RuntimeException(err.get());
    }

    private VideoMetricsSnapshot buildSnapshot() {
        Optional<Integer> width = optionalPositiveInt(lastWidth.get());
        Optional<Integer> height = optionalPositiveInt(lastHeight.get());

        long durationNs = pipeline == null ? -1L : pipeline.queryDuration(Format.TIME);
        long positionNs = pipeline == null ? -1L : pipeline.queryPosition(Format.TIME);

        long droppedDecoderFrames = stats.getNullSamples()
            + stats.getUnsupportedSamples()
            + stats.getFailedDecodes()
            + stats.getPoolEmpty();

        return new VideoMetricsSnapshot(
            Optional.of("GstGLInteropBackend"),
            Optional.empty(),
            Optional.empty(),
            width,
            height,
            Optional.of("RGBA(GLMemory)"),
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
            List.of("GLMemory zero-copy path")
        );
    }

    private static final class Stats {
        private final AtomicLong nullSamples = new AtomicLong();
        private final AtomicLong unsupportedSamples = new AtomicLong();
        private final AtomicLong failedDecodes = new AtomicLong();
        private final AtomicLong poolEmpty = new AtomicLong();
        private final AtomicLong decodeTimeNsTotal = new AtomicLong();
        private final AtomicLong decodeTimeCount = new AtomicLong();
        private final AtomicLong decodeTimeNsMax = new AtomicLong();
        private final Object fpsLock = new Object();
        private long fpsWindowStartNs;
        private int fpsCount;
        private volatile double outputFps;

        private void reset() {
            nullSamples.set(0L);
            unsupportedSamples.set(0L);
            failedDecodes.set(0L);
            poolEmpty.set(0L);
            decodeTimeNsTotal.set(0L);
            decodeTimeCount.set(0L);
            decodeTimeNsMax.set(0L);
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

        private void onUnsupportedSample() {
            unsupportedSamples.incrementAndGet();
        }

        private void onFailedDecode() {
            failedDecodes.incrementAndGet();
        }

        private void onPoolEmpty() {
            poolEmpty.incrementAndGet();
        }

        private void onDecodeTime(long decodeNs) {
            if (decodeNs <= 0L) return;
            decodeTimeNsTotal.addAndGet(decodeNs);
            decodeTimeCount.incrementAndGet();
            updateMax(decodeTimeNsMax, decodeNs);
        }

        public long getNullSamples() {
            return nullSamples.get();
        }

        public long getUnsupportedSamples() {
            return unsupportedSamples.get();
        }

        public long getFailedDecodes() {
            return failedDecodes.get();
        }

        public long getPoolEmpty() {
            return poolEmpty.get();
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

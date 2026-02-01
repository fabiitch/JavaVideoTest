package com.nz.media.backend.gst;

import com.nz.media.backend.BackendCommand;
import com.nz.media.backend.BackendState;
import com.nz.media.frame.VideoFrame;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for decode-only backends that output CPU frames via AppSink.
 * Handles pooling + latest-frame logic. Subclasses implement format-specific copy.
 */
public abstract class AbstractGstAppSinkReaderBackend<F extends VideoFrame> extends AbstractGstBackendBase {

    protected static final class FrameDropException extends RuntimeException {
        private FrameDropException() {
            super(null, null, false, false);
        }
    }

    private static final FrameDropException DROP_FRAME = new FrameDropException();

    protected final int poolSize;
    protected final ConcurrentLinkedQueue<F> pool = new ConcurrentLinkedQueue<>();
    protected final AtomicReference<F> latest = new AtomicReference<>(null);

    protected AbstractGstAppSinkReaderBackend(int poolSize) {
        this.poolSize = Math.max(2, poolSize);
    }

    @Override
    public void play() {
        if (!can(BackendCommand.PLAY)) return;
        super.play();
    }

    @Override
    public void close() {
        F frame = latest.getAndSet(null);
        if (frame != null) recycleTyped(frame);
        closePipeline();
        state.set(BackendState.CLOSED);
        pool.clear();
    }

    @Override
    public void pause() {
        if (!can(BackendCommand.PAUSE)) return;
        super.pause();
    }

    @Override
    public void stop() {
        if (pipeline == null) return;
        if (!can(BackendCommand.STOP)) return;

        BackendState prev = state.get();
        state.set(BackendState.SEEKING);

        try {
            pipeline.setState(State.PAUSED);

            sendSeek(
                0L,
                1.0,
                DEFAULT_SEEK_FLAGS
            );

            F old = latest.getAndSet(null);
            if (old != null) recycleTyped(old);

            state.set(BackendState.READY);
        } catch (Throwable t) {
            lastError = t;
            state.set(BackendState.READY);
            throw t;
        }
    }

    @Override
    public void seekNs(long ns) {
        if (!can(BackendCommand.SEEK)) return;
        BackendState prevState = state.get();
        state.set(BackendState.SEEKING);

        long target = Math.max(0, ns);
        sendSeek(target, speed, DEFAULT_SEEK_FLAGS);
        state.set(prevState == BackendState.SEEKING ? BackendState.READY : prevState);

        F prev = latest.getAndSet(null);
        if (prev != null) recycleTyped(prev);
    }

    @Override
    public VideoFrame pollLatestFrame() {
        return latest.getAndSet(null);
    }

    @Override
    public void recycle(VideoFrame frame) {
        if (frame == null) return;
        try {
            @SuppressWarnings("unchecked")
            F f = (F) frame;
            recycleTyped(f);
        } catch (ClassCastException ignored) {
            // ignore foreign frames
        }
    }

    @Override
    protected AppSink findAppSink(Pipeline p) {
        Element e = p.getElementByName("mysink");
        if (e instanceof AppSink s) return s;
        return (AppSink) e;
    }

    @Override
    protected void onPipelineBuilt(Pipeline p) {
        if (appSink == null) throw new IllegalStateException("appsink 'mysink' not found");

        appSink.setCaps(Caps.fromString(appSinkCaps()));
        configureAppSink(appSink);

        appSink.connect((AppSink.NEW_SAMPLE) sink -> onNewSample(sink));
    }

    protected void configureAppSink(AppSink sink) {
        sink.set("emit-signals", true);
        sink.set("sync", sync());
        sink.set("max-buffers", maxBuffers());
        sink.set("drop", dropOldFrames());
    }

    protected final void initPool(java.util.function.Supplier<F> supplier) {
        pool.clear();
        for (int i = 0; i < poolSize; i++) {
            pool.offer(supplier.get());
        }
    }

    protected final void dropFrame() {
        throw DROP_FRAME;
    }

    private FlowReturn onNewSample(AppSink sink) {
        Sample sample = null;
        try {
            sample = sink.pullSample();
            if (sample == null) {
                onNullSample();
                return FlowReturn.OK;
            }

            long decodeStartNs = System.nanoTime();
            F frame;
            try {
                frame = decodeSample(sample);
            } catch (FrameDropException ignored) {
                return FlowReturn.OK;
            } catch (Throwable t) {
                onSampleError(t);
                return FlowReturn.OK;
            }
            onDecodeTime(System.nanoTime() - decodeStartNs);

            if (frame != null) {
                onFrameReceived(frame);
                F prev = latest.getAndSet(frame);
                if (prev != null) {
                    recycleTyped(prev);
                }
            }
            return FlowReturn.OK;
        } finally {
            if (sample != null) {
                sample.dispose();
            }
        }
    }

    protected void onNullSample() {}

    protected void onSampleError(Throwable t) {}

    protected void onDecodeTime(long decodeNs) {}

    protected void onFrameReceived(F frame) {}

    protected abstract F decodeSample(Sample sample);

    protected abstract F obtainFrame(int width, int height);

    protected abstract void fillFrameFromSample(F frame, Sample sample);

    protected abstract void recycleTyped(F frame);

    protected abstract String appSinkCaps();

    protected boolean dropOldFrames() {
        return true;
    }

    protected int maxBuffers() {
        return 2;
    }

    protected boolean sync() {
        return true;
    }
}

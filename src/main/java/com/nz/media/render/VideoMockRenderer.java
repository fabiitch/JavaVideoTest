package com.nz.media.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.nz.media.frame.VideoFrame;
import com.nz.media.metrics.VideoMetricsSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class VideoMockRenderer implements  VideoRenderer{
    // metrics fields (thread-safe)
    private final RollingFps renderFps = new RollingFps();
    private final TimingStats uploadTiming = new TimingStats();
    private final TimingStats renderTiming = new TimingStats();
    private final AtomicLong droppedRendererFrames = new AtomicLong();
    private volatile int lastWidth = 0;
    private volatile int lastHeight = 0;

    @Override
    public boolean supports(VideoFrame frame) {
        return true;
    }

    @Override
    public void upload(VideoFrame frame) {
        if (frame == null) return;
        long t0 = System.nanoTime();
        boolean uploaded = false;
        try {
            int w = frame.width();
            int h = frame.height();
            if (w <= 0 || h <= 0) {
                droppedRendererFrames.incrementAndGet();
                return;
            }
            lastWidth = w;
            lastHeight = h;
            uploaded = true;
        } finally {
            if (uploaded) {
                uploadTiming.onSampleNs(System.nanoTime() - t0);
            }
        }
    }

    @Override
    public void render(SpriteBatch batch, float x, float y, float width, float height) {
        long t0 = System.nanoTime();
        boolean rendered = false;
        try {
            renderFps.onFrame();
            rendered = true;
        } finally {
            if (rendered) {
                renderTiming.onSampleNs(System.nanoTime() - t0);
            }
        }
    }

    @Override
    public void dispose() {

    }

    @Override
    public Optional<com.nz.media.metrics.VideoMetricsProvider> metrics() {
        return Optional.of(this::buildMetricsSnapshot);
    }

    private VideoMetricsSnapshot buildMetricsSnapshot() {
        String name = getClass().getSimpleName();

        return new VideoMetricsSnapshot(
            Optional.empty(),
            Optional.empty(),
            Optional.of(name),
            lastWidth > 0 ? Optional.of(lastWidth) : Optional.empty(),
            lastHeight > 0 ? Optional.of(lastHeight) : Optional.empty(),
            Optional.of(rendererPixelFormat()),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(renderFps.getLastFps()),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(droppedRendererFrames.get()),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            uploadTiming.avgMs(),
            uploadTiming.maxMs(),
            renderTiming.avgMs(),
            renderTiming.maxMs(),
            rendererNotes()
        );
    }

    private String rendererPixelFormat() {
        return "MOCK";
    }

    private List<String> rendererNotes() {
        return List.of();
    }

    private static final class RollingFps {
        private volatile long windowStartNs = System.nanoTime();
        private final AtomicLong frames = new AtomicLong();
        private volatile double lastFps = 0.0;

        void onFrame() {
            frames.incrementAndGet();
            long now = System.nanoTime();
            long dt = now - windowStartNs;
            if (dt >= 1_000_000_000L) {
                long f = frames.getAndSet(0);
                lastFps = f * (1_000_000_000.0 / dt);
                windowStartNs = now;
            }
        }

        double getLastFps() {
            return lastFps;
        }
    }

    private static final class TimingStats {
        private final AtomicLong count = new AtomicLong();
        private volatile double avgMs = 0.0;
        private volatile double maxMs = 0.0;

        void onSampleNs(long dtNs) {
            double ms = dtNs / 1_000_000.0;
            long n = count.incrementAndGet();
            if (n == 1) {
                avgMs = ms;
            } else {
                avgMs = avgMs + (ms - avgMs) / Math.min(n, 120);
            }
            if (ms > maxMs) {
                maxMs = ms;
            }
        }

        Optional<Double> avgMs() {
            return count.get() > 0 ? Optional.of(avgMs) : Optional.empty();
        }

        Optional<Double> maxMs() {
            return count.get() > 0 ? Optional.of(maxMs) : Optional.empty();
        }
    }
}

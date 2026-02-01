package com.nz.media.render.external;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.nz.media.frame.VideoFrame;
import com.nz.media.frame.external.GlTextureFrame;
import com.nz.media.metrics.VideoMetricsSnapshot;
import com.nz.media.render.VideoRenderer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Zero-copy renderer for GStreamer GLMemory.
 * Wraps an external OpenGL texture ID into a LibGDX Texture.
 */
public final class GstExternalTextureGdxRenderer implements VideoRenderer {

    private ExternalTexture texture;
    private final AtomicLong textureUpdates = new AtomicLong();
    private volatile int lastWidth = 0;
    private volatile int lastHeight = 0;
    private volatile int lastTextureId = 0;

    // metrics fields (thread-safe)
    private final RollingFps renderFps = new RollingFps();
    private final TimingStats uploadTiming = new TimingStats();
    private final TimingStats renderTiming = new TimingStats();
    private final AtomicLong droppedRendererFrames = new AtomicLong();

    @Override
    public boolean supports(VideoFrame frame) {
        return frame instanceof GlTextureFrame;
    }

    @Override
    public void upload(VideoFrame frame) {
        if (!(frame instanceof GlTextureFrame f)) {
            droppedRendererFrames.incrementAndGet();
            return;
        }
        if (f.textureId() <= 0) {
            droppedRendererFrames.incrementAndGet();
            return;
        }

        long t0 = System.nanoTime();
        boolean uploaded = false;
        try {
            lastWidth = f.width();
            lastHeight = f.height();
            lastTextureId = f.textureId();

            if (texture == null) {
                texture = new ExternalTexture(f.textureId(), f.width(), f.height());
            } else {
                texture.update(f.textureId(), f.width(), f.height());
            }
            textureUpdates.incrementAndGet();
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
            if (texture == null) return;
            batch.draw(texture, x, y, width, height);
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
        // IMPORTANT:
        // Do NOT delete GL texture.
        // It is owned by GStreamer.
        texture = null;
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
        return "GLTexture(external)";
    }

    private List<String> rendererNotes() {
        return List.of(
            "zero-copy",
            "external texture id",
            "textureId=" + lastTextureId,
            "textureUpdates=" + textureUpdates.get()
        );
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

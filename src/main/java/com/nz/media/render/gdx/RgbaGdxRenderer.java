package com.nz.media.render.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.nz.media.frame.VideoFrame;
import com.nz.media.frame.rgba.RgbaCpuView;
import com.nz.media.metrics.VideoMetricsSnapshot;
import com.nz.media.render.VideoRenderer;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class RgbaGdxRenderer implements VideoRenderer {

    private Texture texture;
    private int width;
    private int height;
    private final AtomicLong textureReallocs = new AtomicLong();

    // metrics fields (thread-safe)
    private final RollingFps renderFps = new RollingFps();
    private final TimingStats uploadTiming = new TimingStats();
    private final TimingStats renderTiming = new TimingStats();
    private final AtomicLong droppedRendererFrames = new AtomicLong();
    private volatile int lastWidth = 0;
    private volatile int lastHeight = 0;

    @Override
    public boolean supports(VideoFrame frame) {
        return frame != null && frame.view(RgbaCpuView.class) != null;
    }

    @Override
    public void upload(VideoFrame frame) {
        if (frame == null) return;
        RgbaCpuView rgba = frame.view(RgbaCpuView.class);
        if (rgba == null) {
            droppedRendererFrames.incrementAndGet();
            return;
        }

        long t0 = System.nanoTime();
        boolean uploaded = false;
        try {
            int w = frame.width();
            int h = frame.height();
            ensureTexture(w, h);
            lastWidth = w;
            lastHeight = h;

            if (rgba.strideBytes() != w * 4) {
                throw new IllegalArgumentException("Renderer expects tightly packed RGBA (stride=width*4).");
            }

            ByteBuffer data = rgba.rgba();
            if (data == null) {
                droppedRendererFrames.incrementAndGet();
                return;
            }

            texture.bind();
            Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
            data.rewind();
            Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, w, h,
                GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, data);
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
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        width = 0;
        height = 0;
    }

    private void ensureTexture(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (texture != null && w == width && h == height) return;

        dispose();
        width = w;
        height = h;

        texture = new Texture(width, height, Pixmap.Format.RGBA8888);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textureReallocs.incrementAndGet();
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
        return "RGBA";
    }

    private List<String> rendererNotes() {
        return List.of(
            "Texture RGBA8888",
            "textureReallocs=" + textureReallocs.get()
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

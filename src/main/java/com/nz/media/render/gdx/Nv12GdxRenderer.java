package com.nz.media.render.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.nz.lol.shared.all.file.ClassPathFileResolver;
import com.nz.media.frame.VideoFrame;
import com.nz.media.frame.nv12.Nv12CpuView;
import com.nz.media.frame.nv12.Nv12Frame;
import com.nz.media.metrics.VideoMetricsSnapshot;
import com.nz.media.render.VideoRenderer;
import com.nz.media.render.yuv.NV12Utils;
import com.nz.media.render.yuv.Yuv;
import lombok.Setter;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class Nv12GdxRenderer implements VideoRenderer {

    private final ShaderProgram shader;

    private Texture yTex;
    private Texture uvTex;
    private int width, height;
    private final AtomicLong textureReallocs = new AtomicLong();

    // metrics fields (thread-safe)
    private final RollingFps renderFps = new RollingFps();
    private final TimingStats uploadTiming = new TimingStats();
    private final TimingStats renderTiming = new TimingStats();
    private final AtomicLong droppedRendererFrames = new AtomicLong();
    private volatile int lastWidth = 0;
    private volatile int lastHeight = 0;

    @Setter
    private boolean fullRange = true;

    public Nv12GdxRenderer() {
        ShaderProgram.pedantic = false;
        ClassPathFileResolver fileResolver = new ClassPathFileResolver();

        File vert = Yuv.getVert(fileResolver);
        File frag = Yuv.getFrag(fileResolver);
        this.shader = new ShaderProgram(new FileHandle(vert), new FileHandle(frag));
    }


    public void dispose() {
        disposeTextures();
        if (shader != null) shader.dispose();
    }

    @Override
    public boolean supports(VideoFrame frame) {
        return frame.view(Nv12CpuView.class) != null;
    }

    @Override
    public void upload(VideoFrame frame) {
        if (frame == null) return;
        Nv12CpuView view = frame.view(Nv12CpuView.class);
        if (view == null) {
            droppedRendererFrames.incrementAndGet();
            return;
        }

        long t0 = System.nanoTime();
        boolean uploaded = false;
        try {
            //        if (frame.format() != VideoFrame.PixelFormat.NV12) return;

            Nv12Frame nv12Frame = (Nv12Frame) frame;
            int w = frame.width();
            int h = frame.height();
            ensureTextures(w, h);
            lastWidth = w;
            lastHeight = h;

            ByteBuffer y = nv12Frame.y();
            ByteBuffer uv = nv12Frame.uv();

            int yStride = nv12Frame.yStrideBytes();
            int uvStride = nv12Frame.uvStrideBytes();

            if (yStride != w || uvStride != w) {
                throw new IllegalArgumentException("Renderer expects tightly packed NV12 (yStride=w, uvStride=w). Backend must repack.");
            }

            Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);

            // Upload Y
            yTex.bind(0);
            if (Gdx.gl30 != null) {
                Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, w, h, GL30.GL_RED, GL20.GL_UNSIGNED_BYTE, y);
            } else {
                Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, w, h, GL20.GL_LUMINANCE, GL20.GL_UNSIGNED_BYTE, y);
            }

            // Upload UV (half res)
            uvTex.bind(1);
            if (Gdx.gl30 != null) {
                Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, w / 2, h / 2, GL30.GL_RG, GL20.GL_UNSIGNED_BYTE, uv);
            } else {
                // fallback: GL_LUMINANCE_ALPHA for UV if shader supports it (your current shader does via u_uvIsLA)
                Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, w / 2, h / 2, GL20.GL_LUMINANCE_ALPHA, GL20.GL_UNSIGNED_BYTE, uv);
            }
            uploaded = true;
        } finally {
            if (uploaded) {
                uploadTiming.onSampleNs(System.nanoTime() - t0);
            }
        }
    }

    @Override
    public void render(SpriteBatch batch, float x, float y, float drawW, float drawH) {
        long t0 = System.nanoTime();
        boolean rendered = false;
        try {
            if (yTex == null || uvTex == null) return;

            batch.setShader(shader);

            // UV on unit 1
            uvTex.bind(1);
            shader.bind();
            shader.setUniformi("u_texUV", 1);
            shader.setUniformi("u_fullRange", fullRange ? 1 : 0);
            shader.setUniformi("u_uvIsLA", (Gdx.gl30 == null) ? 1 : 0);

            // Y on unit 0 (SpriteBatch uses u_texture)
            yTex.bind(0);
            batch.draw(yTex, x, y, drawW, drawH);
            batch.setShader(null);
            renderFps.onFrame();
            rendered = true;
        } finally {
            if (rendered) {
                renderTiming.onSampleNs(System.nanoTime() - t0);
            }
        }
    }

    private void ensureTextures(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (yTex != null && w == width && h == height) return;

        disposeTextures();
        width = w;
        height = h;

        yTex = NV12Utils.getYTex(w, h);
        uvTex = NV12Utils.getUvTex(w, h);
        textureReallocs.incrementAndGet();

        yTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        uvTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        yTex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        uvTex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
    }

    private void disposeTextures() {
        if (uvTex != null) {
            uvTex.dispose();
            uvTex = null;
        }
        if (yTex != null) {
            yTex.dispose();
            yTex = null;
        }
        width = height = 0;
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
        return "NV12->RGBA(shader)";
    }

    private List<String> rendererNotes() {
        return List.of(
            "shader=nv12",
            "yuv path",
            "uses 2 textures (Y/UV)",
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

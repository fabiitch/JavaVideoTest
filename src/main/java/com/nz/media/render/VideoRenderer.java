package com.nz.media.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.nz.media.frame.VideoFrame;

/**
 * Platform / format specific renderer (LibGDX).
 *
 * - Consumes VideoFrame via views (NV12, RGBA, GPU-share, etc.)
 * - Performs GPU upload and rendering
 * - Does NOT own decoding, timing, or threading
 *
 * All methods must be called from the LibGDX render (GL) thread.
 */
public interface VideoRenderer {

    /**
     * Returns true if this renderer can handle the given frame.
     * Typical implementation: check for a specific view.
     */
    boolean supports(VideoFrame frame);

    /**
     * Uploads the frame to GPU resources (textures, PBO, etc).
     * Called on the GL thread.
     *
     * The frame will be recycled by the caller AFTER this method returns.
     */
    void upload(VideoFrame frame);

    /**
     * Renders the last uploaded frame.
     */
    void render(SpriteBatch batch, float x, float y, float width, float height);

    /**
     * Releases GPU resources (textures, shaders, FBOs).
     */
    void dispose();

    default java.util.Optional<com.nz.media.metrics.VideoMetricsProvider> metrics() {
        return java.util.Optional.empty();
    }
}

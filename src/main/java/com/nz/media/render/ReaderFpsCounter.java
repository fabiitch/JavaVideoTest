package com.nz.media.render;

public class ReaderFpsCounter {
    // Coefficient de lissage EMA (0.1 = réactif, 0.02 = très lissé)
    private static final double FPS_ALPHA = 0.10;
    // --- FPS tracking ---
    private volatile double fpsEma = 0.0;
    private volatile long lastFrameNs = 0L;


    public void newFrame() {
        long now = System.nanoTime();
        long prev = lastFrameNs;
        lastFrameNs = now;
        if (prev != 0L) {
            double instFps = 1_000_000_000.0 / (now - prev);
            if (Double.isFinite(instFps) && instFps > 0) {
                if (fpsEma == 0.0) {
                    fpsEma = instFps;
                } else {
                    fpsEma = fpsEma * (1.0 - FPS_ALPHA) + instFps * FPS_ALPHA;
                }
            }
        }
    }

    public int getFps() {
        return (int) Math.round(fpsEma);
    }
}

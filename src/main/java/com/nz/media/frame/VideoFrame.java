package com.nz.media.frame;

/**
 * Minimal, backend-agnostic video frame.
 * Advanced access is provided through "views" (capabilities).
 * <p>
 * Example:
 * Nv12CpuView nv12 = frame.view(Nv12CpuView.class);
 * if (nv12 != null) { ... }
 */
public interface VideoFrame {

    int width();

    int height();

    /**
     * Presentation timestamp in nanoseconds (0 if unknown).
     */
    long ptsNs();

    /**
     * Capability/view accessor.
     * Return null when unsupported.
     */
    <T> T view(Class<T> type);


    void reset();
}

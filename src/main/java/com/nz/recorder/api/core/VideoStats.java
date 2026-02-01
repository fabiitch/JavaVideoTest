package com.nz.recorder.api.core;

/** Minimal stats; expand later (bitrate, encode latency, queue depths, etc.). */
public record VideoStats(
        double fpsRequested,
        double fpsActual,
        long framesEncoded,
        long framesDropped
) {
    public VideoStats {
        if (fpsRequested <= 0) throw new IllegalArgumentException("fpsRequested");
        if (fpsActual < 0) throw new IllegalArgumentException("fpsActual");
        if (framesEncoded < 0) throw new IllegalArgumentException("framesEncoded");
        if (framesDropped < 0) throw new IllegalArgumentException("framesDropped");
    }
}

package com.nz.recorder.backend.gst.session;

import com.nz.recorder.api.core.RecorderSettings;
import com.nz.recorder.api.core.VideoStats;

import java.time.Duration;

public final class GstSessionMetrics {
    private long startNanos;
    private long stopNanos;
    private long segmentsProduced;
    private long framesDropped;
    private long framesEncoded;

    public void markStart() {
        startNanos = System.nanoTime();
    }

    public void markStop() {
        stopNanos = System.nanoTime();
    }

    public Duration duration() {
        if (startNanos == 0L || stopNanos == 0L) {
            return Duration.ZERO;
        }
        return Duration.ofNanos(Math.max(0L, stopNanos - startNanos));
    }

    public void setSegmentsProduced(long segmentsProduced) {
        this.segmentsProduced = Math.max(0L, segmentsProduced);
    }

    public long segmentsProduced() {
        return segmentsProduced;
    }

    public void setFramesDropped(long framesDropped) {
        this.framesDropped = Math.max(0L, framesDropped);
    }

    public void setFramesEncoded(long framesEncoded) {
        this.framesEncoded = Math.max(0L, framesEncoded);
    }

    public VideoStats toVideoStats(RecorderSettings settings) {
        return new VideoStats(
                settings.fps(),
                0.0,
                framesEncoded,
                framesDropped
        );
    }
}

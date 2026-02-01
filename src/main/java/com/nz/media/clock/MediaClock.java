// file:/C:/Users/fabocc/Documents/dossier_perso/workspace/LoLCopilot/gdx-video-engine/src/main/java/com/nz/media/MediaClock.java
package com.nz.media.clock;

/**
 * Simple media clock that tracks a "media time" (ns) based on wall clock and speed.
 * - play(): starts advancing
 * - pause(): freezes time
 * - seek(): jumps to target time
 * - setSpeed(): changes rate while preserving continuity
 *
 * This clock is intentionally dumb + reliable for a v1 player.
 */
public final class MediaClock {

    private boolean playing;
    private double speed = 1.0;

    // media position at the moment of last (play/pause/seek/speed change)
    private long baseMediaNs = 0;

    // wall time at the moment of last (play/seek/speed change) while playing
    private long baseWallNs = 0;

    public MediaClock() {
    }

    /** Current media time in nanoseconds. */
    public long nowMediaNs() {
        if (!playing) return baseMediaNs;
        long wallDelta = System.nanoTime() - baseWallNs;
        return baseMediaNs + (long) (wallDelta * speed);
    }

    /** Current media time in milliseconds. */
    public long nowMediaMs() {
        return nowMediaNs() / 1_000_000L;
    }

    public boolean isPlaying() {
        return playing;
    }

    public double getSpeed() {
        return speed;
    }

    /** Start/resume advancing media time. */
    public void play() {
        if (playing) return;
        playing = true;
        baseWallNs = System.nanoTime();
    }

    /** Freeze media time. */
    public void pause() {
        if (!playing) return;
        baseMediaNs = nowMediaNs();
        playing = false;
    }

    /**
     * Stop and reset to 0.
     * (Convenient for "stop()" semantics.)
     */
    public void stop() {
        playing = false;
        speed = 1.0;
        baseMediaNs = 0;
        baseWallNs = 0;
    }

    /** Jump to a specific media time (ns). Keeps current playing state. */
    public void seekNs(long mediaNs) {
        baseMediaNs = Math.max(0, mediaNs);
        if (playing) baseWallNs = System.nanoTime();
    }

    public void seekMs(long mediaMs) {
        seekNs(mediaMs * 1_000_000L);
    }

    /**
     * Change playback speed while keeping continuity.
     * Example: 0.5, 1.0, 2.0
     */
    public void setSpeed(double newSpeed) {
        if (newSpeed <= 0.0) newSpeed = 0.0001;
        baseMediaNs = nowMediaNs();
        if (playing) baseWallNs = System.nanoTime();
        speed = newSpeed;
    }
}

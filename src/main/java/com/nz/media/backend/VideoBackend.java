package com.nz.media.backend;

import com.nz.media.frame.VideoFrame;

/**
 * Backend does decoding + audio output.
 * It MUST be usable without LibGDX.
 * <p>
 * Key rule for GDX: backend never touches GL objects.
 * It only produces VideoFrame and exposes transport controls.
 */
public interface VideoBackend {

    BackendState getState();

    default boolean isCommandReady() {
        BackendState s = getState();
        return s == BackendState.READY || s == BackendState.PLAYING || s == BackendState.PAUSED;
    }

    default boolean can(BackendCommand cmd) {
        BackendState s = getState();
        boolean opened = isOpenedForCommands() && BackendStateUtils.isOpened(s);

        return switch (cmd) {
            case PLAY -> opened && BackendStateUtils.isPlayable(s);
            case PAUSE -> opened && BackendStateUtils.isPausable(s);
            case STOP -> opened && (BackendStateUtils.isPlayable(s) || BackendStateUtils.isPausable(s) || s == BackendState.READY);
            case SEEK -> opened && BackendStateUtils.isSeekable(s);
            case SET_SPEED -> opened && BackendStateUtils.isSpeedChangeAllowed(s);
            case SET_VOLUME -> opened && BackendStateUtils.isVolumeChangeAllowed(s);
            case SET_LOOPING -> false; // Looping not implemented here (player-side concern).
        };
    }

    /**
     * Override if additional backend-specific readiness must be satisfied
     * (e.g., pipeline available).
     */
    default boolean isOpenedForCommands() {
        return true;
    }

    void open(String path);

    void close();

    void play();

    void pause();

    void stop();

    /**
     * Seek to absolute position (nanoseconds).
     * Recommended to FLUSH internally (backend-specific).
     */
    void seekNs(long ns);

    /**
     * Playback rate. For proper audio+video speed, backend should implement it.
     */
    void setSpeed(double speed);

    /**
     * Volume in [0..1] (backend can map internally).
     */
    double getVolume();
    void setVolume(double volume);

    void setMuted(boolean muted);

    long getDurationNs();

    /**
     * Optional: backend "real" position (may be jittery).
     * Player can also rely on MediaClock instead.
     */
    long getPositionNs();

    /**
     * Latest-only queue (size ~1).
     * Returns and consumes the most recent frame; null if nothing new.
     * <p>
     * Thread-safety: backend must make this safe to call from GDX render thread.
     */
    VideoFrame pollLatestFrame();

    void recycle(VideoFrame frame);

    default java.util.Optional<com.nz.media.metrics.VideoMetricsProvider> metrics() {
        return java.util.Optional.empty();
    }

}

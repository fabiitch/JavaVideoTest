package com.nz.media.player;

import com.nz.media.event.VideoEvent;
import com.nz.media.event.VideoEventBus;
import com.nz.media.frame.VideoFrame;

public interface VideoPlayer {
    <T extends VideoEvent> VideoEventBus.Subscription on(Class<T> type, java.util.function.Consumer<T> consumer);

    void open(String path);

    void play();

    void pause();

    void stop();

    void close();

    void seekMs(long ms);

    double getSpeed();

    void setSpeed(double speed);

    double getVolume();

    void setVolume(double volume);

    void setMuted(boolean muted);

    VideoFrame pollFrame();        // returns latest decoded frame (may be null)

    void recycle(VideoFrame frame);

    long getPositionMs();

    long getDurationMs();

    boolean isPlaying();

    default java.util.Optional<com.nz.media.metrics.VideoMetricsProvider> metrics() {
        return java.util.Optional.empty();
    }

}

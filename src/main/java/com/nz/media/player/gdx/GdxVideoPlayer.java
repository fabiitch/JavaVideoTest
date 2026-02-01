package com.nz.media.player.gdx;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.nz.media.event.VideoEvent;
import com.nz.media.event.VideoEventBus;
import com.nz.media.frame.VideoFrame;
import com.nz.media.player.VideoPlayer;
import com.nz.media.player.VideoPlayerCore;
import com.nz.media.render.VideoRenderer;

import java.util.Objects;
import java.util.function.Consumer;

public class GdxVideoPlayer implements VideoPlayer {

    private final VideoPlayerCore core;
    private final VideoRenderer renderer;

    public GdxVideoPlayer(VideoPlayerCore core, VideoRenderer renderer) {
        this.core = Objects.requireNonNull(core);
        this.renderer = Objects.requireNonNull(renderer);
    }

    public void update() {
        core.rethrowIfFailed();

        VideoFrame frame = core.pollFrame();
        if (frame != null) {
            if (renderer.supports(frame)) {
                renderer.upload(frame);
            }
            core.recycle(frame); // OBLIGATOIRE
        }
    }

    public void render(SpriteBatch batch, float x, float y, float w, float h) {
        renderer.render(batch, x, y, w, h);
    }

    @Override
    public <T extends VideoEvent> VideoEventBus.Subscription on(Class<T> type, Consumer<T> consumer) {
        return core.on(type, consumer);
    }

    @Override
    public void open(String path) {
        core.open(path);
    }

    @Override
    public void play() {
        core.play();
    }

    @Override
    public void pause() {
        core.pause();
    }

    @Override
    public void stop() {
        core.stop();
    }

    @Override
    public void close() {
        core.close();
    }

    @Override
    public void seekMs(long ms) {
        core.seekMs(ms);
    }

    @Override
    public double getSpeed() {
        return core.getSpeed();
    }

    @Override
    public void setSpeed(double speed) {
        core.setSpeed(speed);
    }

    @Override
    public double getVolume() {
        return core.getVolume();
    }

    @Override
    public void setVolume(double volume) {
        core.setVolume(volume);
    }

    @Override
    public void setMuted(boolean muted) {
        core.setMuted(muted);
    }

    @Override
    public VideoFrame pollFrame() {
        return core.pollFrame();
    }

    @Override
    public void recycle(VideoFrame frame) {
        core.recycle(frame);
    }

    @Override
    public long getPositionMs() {
        return core.getPositionMs();
    }

    @Override
    public long getDurationMs() {
        return core.getDurationMs();
    }

    @Override
    public boolean isPlaying() {
        return core.isPlaying();
    }
}

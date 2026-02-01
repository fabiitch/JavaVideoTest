package com.nz.media;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.nz.media.player.VideoPlayer;
import com.nz.media.player.gdx.GdxVideoPlayer;
import org.junit.jupiter.api.Disabled;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Disabled
public final class VideoTestScreen extends ScreenAdapter {

    private final SpriteBatch batch;
    private final GdxVideoPlayer videoPlayer;
    private final File videoFile;

    public VideoTestScreen(File videoFile, GdxVideoPlayer videoPlayer) {
        System.out.println("VideoFile=" + videoFile.getAbsolutePath());
        this.videoFile = videoFile;
        this.videoPlayer = videoPlayer;
        this.batch = new SpriteBatch();
    }

    @Override
    public void show() {
        videoPlayer.open(videoFile.getAbsolutePath());
        videoPlayer.setSpeed(1);
        videoPlayer.seekMs(TimeUnit.SECONDS.toMillis(60));
        videoPlayer.play();
    }

    float acc = 0;

    @Override
    public void render(float delta) {
        if (acc <= 10) {
            acc += delta;
        }
        int FPS = Gdx.graphics.getFramesPerSecond();
        long heapMb = Gdx.app.getJavaHeap() / (1024L * 1024L);
        Gdx.graphics.setTitle("FPS=" + FPS
            + ", heapMb=" + heapMb
        );

        // clear
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // update player (poll frame + upload + recycle)
        videoPlayer.update();

        // draw
        batch.begin();
        videoPlayer.render(
            batch,
            0,
            0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );
        batch.end();
    }

    @Override
    public void dispose() {
//        if (player != null) player.dispose();
        if (videoPlayer != null) videoPlayer.close();
        if (batch != null) batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
    }
}

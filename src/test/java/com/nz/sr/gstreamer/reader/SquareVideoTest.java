//package com.nz.sr.gstreamer.reader;
//
//import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.ScreenAdapter;
//import com.badlogic.gdx.graphics.Color;
//import com.badlogic.gdx.graphics.OrthographicCamera;
//import com.badlogic.gdx.graphics.g2d.BitmapFont;
//import com.badlogic.gdx.graphics.g2d.SpriteBatch;
//import com.badlogic.gdx.utils.viewport.ScreenViewport;
//import com.badlogic.gdx.utils.viewport.Viewport;
//import com.nz.sr.RessourceUtils;
//import com.nz.sr.gstreamer.GstTestUtils;
//
//import java.io.File;
//import java.util.function.Function;
//
//public class SquareVideoTest extends ScreenAdapter {
//    private final SpriteBatch batch = new SpriteBatch();
//
//    IOldVideoReader r1, r2, r3, r4;
//
//    OrthographicCamera camera = new OrthographicCamera();
//    Viewport viewport = new ScreenViewport(camera);
//
//    BitmapFont font;
//
//    public SquareVideoTest(File folderVideos, Function<File, IOldVideoReader> videoReaderFactory) {
//        GstTestUtils.initGStreamer();
//
//        File video1 = RessourceUtils.pickRandomFile(folderVideos);
//        File video2 = RessourceUtils.pickRandomFile(folderVideos);
//        File video3 = RessourceUtils.pickRandomFile(folderVideos);
//        File video4 = RessourceUtils.pickRandomFile(folderVideos);
//
//        // 2) PlayBin + AppSink RGBA
//        r1 = videoReaderFactory.apply(video1);
//        r2 = videoReaderFactory.apply(video2);
//        r3 = videoReaderFactory.apply(video3);
//        r4 = videoReaderFactory.apply(video4);
//
//        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
//        font = new BitmapFont();
//        font.setColor(Color.RED);
//    }
//
//    @Override
//    public void render(float delta) {
//        Gdx.graphics.setTitle("FPS=" + Gdx.graphics.getFramesPerSecond());
//
//        r1.update(delta);
//        r2.update(delta);
//        r3.update(delta);
//        r4.update(delta);
//
//        // rendu
//        Gdx.gl.glClearColor(0, 0, 0, 1);
//        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);
//
//        float w = viewport.getWorldWidth();
//        float h = viewport.getWorldHeight();
//        float halfW = w / 2f, halfH = h / 2f;
//
//        camera.update();
//        batch.setProjectionMatrix(camera.combined);
//        batch.begin();
//        r1.render(batch, 0, 0, halfW, halfH);
//        r2.render(batch, halfW, 0, halfW, halfH);
//        r3.render(batch, 0, halfH, halfW, halfH);
//        r4.render(batch, halfW, halfH, halfW, halfH);
//
//        // position texte
//        float pad = 8f;
//        // en haut à gauche de chaque quadrant
//        font.draw(batch, String.valueOf(r1.getFps()), pad, halfH - pad);          // top-left of bottom-left video
//        font.draw(batch, String.valueOf(r2.getFps()), halfW + pad, halfH - pad);  // top-left of bottom-right video
//        font.draw(batch, String.valueOf(r3.getFps()), pad, h - pad);              // top-left of top-left video
//        font.draw(batch, String.valueOf(r4.getFps()), halfW + pad, h - pad);      // top-left of top-right video
//
//        batch.end();
//    }
//
//    @Override
//    public void resize(int width, int height) {
//        super.resize(width, height);
//        viewport.update(width, height, true);
//    }
//
//    @Override
//    public void dispose() {
//        r1.dispose();
//        r2.dispose();
//        r3.dispose();
//        r4.dispose();
//        batch.dispose();
//        font.dispose();
//    }
//}

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
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.Function;
//
//public class GridVideoTest extends ScreenAdapter {
//    private final SpriteBatch batch = new SpriteBatch();
//    private final OrthographicCamera camera = new OrthographicCamera();
//    private final Viewport viewport = new ScreenViewport(camera);
//
//    private final List<IOldVideoReader> readers = new ArrayList<>();
//    private int cols, rows;
//    private final BitmapFont font;
//
//    // options d’affichage
//    private boolean keepSquareCells = true; // true = zone carrée centrée, false = remplir la cellule
//
//    /**
//     * @param count              nombre de players à créer
//     * @param videoReaderFactory factory (File -> IVideoReader)
//     * @param videoFolder        video folder
//     */
//    public GridVideoTest(int count, Function<File, IOldVideoReader> videoReaderFactory, File videoFolder) {
//        GstTestUtils.initGStreamer();
//        for (int i = 0; i < count; i++) {
//            readers.add(videoReaderFactory.apply(RessourceUtils.pickRandomFile(videoFolder)));
//        }
//        recomputeGrid();
//        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
//        font = new BitmapFont();
//        font.setColor(Color.RED);
//    }
//
//    private void recomputeGrid() {
//        int n = Math.max(1, readers.size());
//        cols = (int) Math.ceil(Math.sqrt(n));
//        rows = (int) Math.ceil(n / (float) cols);
//    }
//
//    @Override
//    public void render(float delta) {
//        Gdx.graphics.setTitle("N=" + readers.size() + "  FPS=" + Gdx.graphics.getFramesPerSecond());
//
//        float W = viewport.getWorldWidth();
//        float H = viewport.getWorldHeight();
//        float cellW = W / cols;
//        float cellH = H / rows;
//        // update des players
//        for (IOldVideoReader r : readers) r.update(delta);
//
//        // clear
//        Gdx.gl.glClearColor(0, 0, 0, 1);
//        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);
//
//        camera.update();
//        batch.setProjectionMatrix(camera.combined);
//        batch.begin();
//
//        for (int i = 0; i < readers.size(); i++) {
//            IOldVideoReader r = readers.get(i);
//            int cx = i % cols;
//            int cy = i / cols;
//
//            float x = cx * cellW;
//            float y = H - (cy + 1) * cellH; // origine en bas-gauche (LibGDX), on place rangée par rangée
//
//            float drawW = cellW;
//            float drawH = cellH;
//
//            if (keepSquareCells) {
//                float s = Math.min(cellW, cellH);
//                x += (cellW - s) / 2f;
//                y += (cellH - s) / 2f;
//                drawW = s;
//                drawH = s;
//            }
//
//            r.render(batch, x, y, drawW, drawH);
//        }
//
//        float PAD = 8f;
//        // --- Dessin des FPS par lecteur : en haut-gauche de chaque tuile ---
//        for (int i = 0; i < readers.size(); i++) {
//            int col = i % cols;
//            int row = i / cols;
//
//            // coin bas-gauche de la cellule
//            float baseX = col * cellW;
//            float baseY = row * cellH;
//
//            // zone vidéo effective dans la cellule (centrée si keepSquareCells)
//            float vx = baseX, vy = baseY, vw = cellW, vh = cellH;
//            if (keepSquareCells) {
//                float s = Math.min(cellW, cellH);
//                vx = baseX + (cellW - s) * 0.5f;
//                vy = baseY + (cellH - s) * 0.5f;
//                vw = vh = s;
//            }
//
//            // position du label : haut-gauche de la zone vidéo
//            float labelX = vx + PAD;
//            float labelY = vy + vh - PAD;
//
//            // texte
//            String fpsText = String.valueOf(readers.get(i).getFps());
//            font.draw(batch, fpsText, labelX, labelY);
//        }
//        batch.end();
//    }
//
//    @Override
//    public void resize(int width, int height) {
//        viewport.update(width, height, true);
//        // pas besoin de recomputeGrid() ici (uniquement si le nombre change)
//    }
//
//    @Override
//    public void dispose() {
//        for (IOldVideoReader r : readers) {
//            try {
//                r.dispose();
//            } catch (Exception ignored) {
//            }
//        }
//        batch.dispose();
//        font.dispose();
//    }
//
//    // setters pratiques
//    public GridVideoTest keepSquareCells(boolean keep) {
//        this.keepSquareCells = keep;
//        return this;
//    }
//}

package com.nz.recorder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class ScreenRecordTestUi extends ScreenAdapter {

    private final ScreenRecordTestService recorderService;
    private Stage stage;
    private Skin skin;

    private TextButton startBtn;
    private TextButton stopBtn;

    public ScreenRecordTestUi(ScreenRecordTestService recorderService) {
        this.recorderService = recorderService;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = loadSkinOrFallback();

        startBtn = new TextButton("START", skin);
        stopBtn = new TextButton("STOP", skin);
        stopBtn.setDisabled(true);

        startBtn.addListener(e -> {
            if (!startBtn.isPressed()) {
                return false;
            }
            recorderService.start();
            return true;
        });

        stopBtn.addListener(e -> {
            if (!stopBtn.isPressed()) {
                return false;
            }
            recorderService.stop();
            return true;
        });

        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(10).width(240).height(70);

        root.add(startBtn).row();
        root.add(stopBtn).row();

        stage.addActor(root);
    }

    @Override
    public void render(float delta) {
        boolean recording = recorderService.isRecording();
        if (recording) {
            ScreenUtils.clear(0.75f, 0f, 0f, 1f);
        } else {
            ScreenUtils.clear(0f, 0f, 0f, 1f);
        }

        startBtn.setDisabled(recording);
        stopBtn.setDisabled(!recording);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        recorderService.dispose();
        if (stage != null) {
            stage.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
    }

    private Skin loadSkinOrFallback() {
        FileHandle skinFile = Gdx.files.internal("ui-skin/uiskin.json");
        if (skinFile.exists()) {
            return new Skin(skinFile);
        }
        throw new IllegalStateException("Missing assets/uiskin.json (Scene2D UI skin). Add it or load your own skin.");
    }
}

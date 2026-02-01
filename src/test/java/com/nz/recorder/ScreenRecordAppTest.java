package com.nz.recorder;


import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.nz.lol.shared.debug.utils.TestDataUtils;

import java.io.File;
import java.nio.file.Path;

public class ScreenRecordAppTest extends Game {

    static File recordFolder = TestDataUtils.getRecordDir();
    static File recordFile = recordFolder.toPath().resolve("test.mp4").toFile();

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("FireCall Recorder Test");
        cfg.setWindowedMode(520, 320);
        cfg.useVsync(true);

        new Lwjgl3Application(new ScreenRecordAppTest(), cfg);
    }
    @Override
    public void create() {
        Path out = recordFile.toPath();
        ScreenRecordTestService recorderService = new ScreenRecordTestService(out);
        setScreen(new ScreenRecordTestUi(recorderService));
    }
}

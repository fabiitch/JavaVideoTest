package com.nz.recorder;

import com.nz.lol.shared.debug.utils.TestDataUtils;
import com.nz.recorder.api.core.RecorderSettings;
import com.nz.recorder.api.output.OutputSettings;
import com.nz.recorder.api.output.QualityPreset;
import com.nz.recorder.api.save.FileSave;
import com.nz.recorder.api.save.SaveStrategy;
import com.nz.recorder.api.targets.CaptureTarget;
import com.nz.recorder.api.targets.ScreenTarget;
import com.nz.recorder.backend.simplegst.SimpleGstRecorder;
import com.nz.recorder.backend.simplegst.SimpleGstRecorderBuilder;
import com.nz.sr.gstreamer.GstTestUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

public class ScreenRecorderTest {

    static File recordFolder = TestDataUtils.getRecordDir();
    static File recordFile = recordFolder.toPath().resolve("test.mp4").toFile();

    @Test
    public void recordScreen() throws InterruptedException {
        GstTestUtils.initGStreamer();

        CaptureTarget target = new ScreenTarget("0");
        SaveStrategy saveStrategy = new FileSave(new OutputSettings(recordFile.toPath(), true));
        RecorderSettings settings = new RecorderSettings(target,
            QualityPreset.MID,
            60,
            saveStrategy);
        SimpleGstRecorder gstRecorder = SimpleGstRecorderBuilder.windowsDesktopToMp4(recordFile.toPath());
        gstRecorder.init();

        System.out.println("start record");
        gstRecorder.start();
        Thread.sleep(10000);
        gstRecorder.stop();

        System.out.println("End record to file = " + recordFile.getAbsolutePath());
    }
}

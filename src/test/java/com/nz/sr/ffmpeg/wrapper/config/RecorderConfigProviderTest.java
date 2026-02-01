package com.nz.sr.ffmpeg.wrapper.config;

import com.nz.sr.ffmpeg.wrapper.FFmpegWrapper;
import org.junit.jupiter.api.Test;

public class RecorderConfigProviderTest {


    private final static String path = "C:\\Users\\fabocc\\Desktop\\dossier_perso\\sdk\\ffmpeg\\bin\\ffmpeg.exe";

    @Test
    public void test() {
        FFmpegWrapper wrapper = new FFmpegWrapper(path);
        FFmpegWrapperConfig configProvider = new FFmpegWrapperConfig(wrapper);



    }
}

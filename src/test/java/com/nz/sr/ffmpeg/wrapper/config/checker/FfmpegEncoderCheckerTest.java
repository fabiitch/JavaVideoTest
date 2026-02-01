package com.nz.sr.ffmpeg.wrapper.config.checker;

import com.nz.sr.ffmpeg.wrapper.FFmpegWrapper;
import com.nz.sr.ffmpeg.wrapper.config.FFmpegWrapperConfig;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegEncoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class FfmpegEncoderCheckerTest {

    private final static String path = "C:\\Users\\fabocc\\Desktop\\dossier_perso\\sdk\\ffmpeg\\bin\\ffmpeg.exe";
    FFmpegWrapper wrapper = new FFmpegWrapper(path);
    FFmpegWrapperConfig config = new FFmpegWrapperConfig(wrapper);

    @Test
    public void testNvecVideo() {
        FFmpegEncoder h264Nvenc = config.getEncoder("h264_nvenc");

        FFmpegEncoderChecker encoderChecker = new FFmpegEncoderChecker(wrapper);
        boolean b = encoderChecker.checkEncoder(h264Nvenc);
        Assertions.assertTrue(b);
    }

    @Test
    public void testAudio() {
        FFmpegEncoderChecker encoderChecker = new FFmpegEncoderChecker(wrapper);
        {
            FFmpegEncoder libfdk_aac = config.getEncoder("aac");
            boolean b = encoderChecker.checkEncoder(libfdk_aac);
            Assertions.assertTrue(b);
        }{
            FFmpegEncoder libfdk_aac = config.getEncoder("libopus");
            boolean b = encoderChecker.checkEncoder(libfdk_aac);
            Assertions.assertTrue(b);
        }
    }
}

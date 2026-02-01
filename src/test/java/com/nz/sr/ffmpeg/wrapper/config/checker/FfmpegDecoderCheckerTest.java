package com.nz.sr.ffmpeg.wrapper.config.checker;

import com.nz.sr.ffmpeg.wrapper.FFmpegWrapper;
import com.nz.sr.ffmpeg.wrapper.config.FFmpegWrapperConfig;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegEncoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class FfmpegDecoderCheckerTest {

    private final static String path = "C:\\Users\\fabocc\\Desktop\\dossier_perso\\sdk\\ffmpeg\\bin\\ffmpeg.exe";

    @Test
    public void testNvec() {
        FFmpegWrapper wrapper = new FFmpegWrapper(path);

        FFmpegWrapperConfig config = new FFmpegWrapperConfig(wrapper);
        FFmpegEncoder h264Nvenc = config.getDecoder("h264");

        FFmpegDecoderChecker decoderChecker = new FFmpegDecoderChecker(wrapper);
        boolean b = decoderChecker.checkDecoder(h264Nvenc);
        Assertions.assertTrue(b);
    }

    @Test
    public void testAmf() {
        FFmpegWrapper wrapper = new FFmpegWrapper(path);
        FFmpegWrapperConfig config = new FFmpegWrapperConfig(wrapper);
        FFmpegEncoder hevc_amf = config.getDecoder("h264_amf");

        FFmpegDecoderChecker decoderChecker = new FFmpegDecoderChecker(wrapper);
        boolean b = decoderChecker.checkDecoder(hevc_amf);
        Assertions.assertFalse(b);
    }


    @Test
    public void testAudio() {
        FFmpegWrapper wrapper = new FFmpegWrapper(path);
        FFmpegWrapperConfig config = new FFmpegWrapperConfig(wrapper);
        FFmpegEncoder hevc_amf = config.getDecoder("aac");

        FFmpegDecoderChecker decoderChecker = new FFmpegDecoderChecker(wrapper);
        boolean b = decoderChecker.checkDecoder(hevc_amf);
        Assertions.assertTrue(b);
    }
}

package com.nz.sr.ffmpeg.wrapper.config.checker;

import com.nz.sr.ffmpeg.wrapper.FFmpegResult;
import com.nz.sr.ffmpeg.wrapper.FFmpegTask;
import com.nz.sr.ffmpeg.wrapper.FFmpegWrapper;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegEncoder;
import com.nz.sr.ffmpeg.wrapper.config.dto.MediaType;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FFmpegEncoderChecker {

    private final FFmpegWrapper wrapper;

    public boolean checkEncoder(FFmpegEncoder encoder) {
        if (encoder.getMediaType() == MediaType.VIDEO) {
            return checkVideo(encoder);
        }
        if (encoder.getMediaType() == MediaType.AUDIO) {
            return checkAudio(encoder);
        }
        return false;
    }

    private boolean checkAudio(FFmpegEncoder encoder) {
        FFmpegTask task = new FFmpegTask();
        task.add("-hide_banner")
            .add("-v", "error")
            .add("-f", "lavfi")
            .add("-i", "sine=frequency=1000:sample_rate=44100:duration=2")
            .add("-c:a", encoder.getName())
            .add("-f", "null")
            .add("-");
        FFmpegResult run = wrapper.run(task);
        return run.ok();
    }

    //    $ ./ffmpeg.exe -hide_banner -v error   -f lavfi -i testsrc2=size=320x180:rate=10   -vf format=yuv420p   -t 0.5   -c:v h264_nvenc   -rc constqp -qp 23   -f null -
    private boolean checkVideo(FFmpegEncoder encoder) {
        FFmpegTask task = new FFmpegTask();
        task.add("-hide_banner")
            .add("-v", "error")
            .add("-f", "lavfi")
            .add("-i", "testsrc2=size=320x180:rate=10")
            .add("-vf", "format=yuv420p")
            .add("-t", "0.5")
            .add("-c:v", encoder.getName())
            .add("-rc", "constqp")
            .add("-qp", "23")
            .add("-f", "null")
            .add("-");
        FFmpegResult run = wrapper.run(task);
        return run.ok();
    }

}

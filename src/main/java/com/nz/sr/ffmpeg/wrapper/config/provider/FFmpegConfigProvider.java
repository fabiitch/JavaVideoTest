package com.nz.sr.ffmpeg.wrapper.config.provider;

import com.nz.sr.ffmpeg.wrapper.FFmpegResult;
import com.nz.sr.ffmpeg.wrapper.FFmpegTask;
import com.nz.sr.ffmpeg.wrapper.FFmpegWrapper;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegCodec;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegEncoder;
import com.nz.sr.ffmpeg.wrapper.config.parser.FFmpegCodecResultParser;
import com.nz.sr.ffmpeg.wrapper.config.parser.FFmpegEncodersResultParser;

import java.util.List;

public class FFmpegConfigProvider {


    public List<FFmpegCodec> getCodecs(FFmpegWrapper wrapper) {
        FFmpegTask task = new FFmpegTask();
        task.add("-hide_banner");
        task.add("-codecs");

        FFmpegResult run = wrapper.run(task);
        return FFmpegCodecResultParser.parse(run.getStdout());
    }

    public List<FFmpegEncoder> getEncoders(FFmpegWrapper wrapper) {
        FFmpegTask task = new FFmpegTask();
        task.add("-hide_banner");
        task.add("-encoders");

        FFmpegResult run = wrapper.run(task);
        return FFmpegEncodersResultParser.parse(run.getStdout());
    }

    public List<FFmpegEncoder> getDecoders(FFmpegWrapper wrapper) {
        FFmpegTask task = new FFmpegTask();
        task.add("-hide_banner");
        task.add("-decoders");

        FFmpegResult run = wrapper.run(task);
        return FFmpegEncodersResultParser.parse(run.getStdout());
    }
}

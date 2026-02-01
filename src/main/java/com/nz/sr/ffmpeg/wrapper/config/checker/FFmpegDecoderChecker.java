package com.nz.sr.ffmpeg.wrapper.config.checker;

import com.nz.sr.ffmpeg.wrapper.FFmpegResult;
import com.nz.sr.ffmpeg.wrapper.FFmpegTask;
import com.nz.sr.ffmpeg.wrapper.FFmpegWrapper;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegEncoder;
import com.nz.sr.ffmpeg.wrapper.config.dto.MediaType;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@AllArgsConstructor
public class FFmpegDecoderChecker {
    private final FFmpegWrapper wrapper;

    public boolean checkDecoder(FFmpegEncoder decoder) {
        if (decoder.getMediaType() == MediaType.VIDEO) {
            return checkVideoDecoder(decoder);
        }
        if (decoder.getMediaType() == MediaType.AUDIO) {
            return checkAudio(decoder);
        }
        return false;
    }

    private boolean checkAudio(FFmpegEncoder decoder) {
        String sampleName = "sample_" + decoder.getCodec().getName();
        String extension = ".m4a";
        File sampleCpyFile = null;
        try (InputStream in = getClass().getResourceAsStream("/samples/audio/" + sampleName + extension)) {
            sampleCpyFile = File.createTempFile(sampleName, extension);
            FileUtils.copyInputStreamToFile(in, sampleCpyFile);

//            ffmpeg -hide_banner -v error -c:a <decoder_name> -i sample.aac -f null -

            FFmpegTask task = new FFmpegTask();
            task.hideBanner()
                .add("-v", "error")
                .add("-i", sampleCpyFile.getAbsolutePath())
                .add("-c:a", decoder.getName())
                .add("-f", "null")
                .add("-");

            FFmpegResult run = wrapper.run(task);
            return run.ok();
        } catch (IOException e) {
            return false;
        } finally {
            if (sampleCpyFile != null && sampleCpyFile.exists()) {
                sampleCpyFile.delete();
            }
        }
    }

    private boolean checkVideoDecoder(FFmpegEncoder decoder) {
        String sampleName = "sample_" + decoder.getCodec().getName();
        String extension = ".mp4";
        File sampleCpyFile = null;
        try (InputStream in = getClass().getResourceAsStream("/samples/video/" + sampleName + extension)) {
            sampleCpyFile = File.createTempFile(sampleName, extension);
            FileUtils.copyInputStreamToFile(in, sampleCpyFile);

//            ffmpeg -i sample.mp4 -c:v h264_amf -f null -
            FFmpegTask task = new FFmpegTask();
            task.hideBanner()
                .add("-v", "error")
                .add("-i", sampleCpyFile.getAbsolutePath())
                .add("-c:v", decoder.getName())
                .add("-f", "null")
                .add("-");

            FFmpegResult run = wrapper.run(task);
            return run.ok();
        } catch (IOException e) {
            return false;
        } finally {
            if (sampleCpyFile != null && sampleCpyFile.exists()) {
                sampleCpyFile.delete();
            }
        }
    }
}

package com.nz.sr.dto.config.video.presets;

import com.nz.sr.dto.config.video.HevcAmfVideoRecordConfig;
import com.nz.sr.dto.config.video.HevcNvencVideoRecordConfig;
import com.nz.sr.dto.config.video.HevcQsvVideoRecordConfig;
import com.nz.sr.dto.config.video.X265VideoRecordConfig;
import com.nz.sr.dto.video.*;
import com.nz.sr.dto.video.preset.X265Preset;

public final class HevcRecordPresets {

    private HevcRecordPresets() {}

    // ---------- NVENC ----------

    public static HevcNvencVideoRecordConfig.HevcNvencVideoRecordConfigBuilder nvenc1080p60LowLatency() {
        return HevcNvencVideoRecordConfig.builder()
            .width(1920)
            .height(1080)
            .fps(60.0)
            .pixelFormat(PixelFormat.YUV420P)
            .colorRange(ColorRange.Limited)
            .container(Container.MP4)
            .targetUsage(TargetUsage.LowLatency)
            .rcMode(RateControl.CBR)
            .bitrateKbps(8000)
            .gop(120)        // 2s @60fps
            .bFrames(0)
            .zeroLatency(true);
    }

    public static HevcNvencVideoRecordConfig.HevcNvencVideoRecordConfigBuilder nvenc1440p60Quality() {
        return HevcNvencVideoRecordConfig.builder()
            .width(2560)
            .height(1440)
            .fps(60.0)
            .pixelFormat(PixelFormat.YUV420P)
            .colorRange(ColorRange.Limited)
            .container(Container.MP4)
            .targetUsage(TargetUsage.Quality)
            .rcMode(RateControl.VBR)
            .bitrateKbps(15000)
            .maxrateKbps(20000)
            .gop(120)
            .bFrames(2)
            .main10(true);
    }

    // ---------- x265 ----------

    public static X265VideoRecordConfig.X265VideoRecordConfigBuilder x2651080p60Quality() {
        return X265VideoRecordConfig.builder()
            .width(1920)
            .height(1080)
            .fps(60.0)
            .pixelFormat(PixelFormat.YUV420P)
            .colorRange(ColorRange.Limited)
            .container(Container.MKV)
            .rcMode(RateControl.CRF)
            .crf(22)
            .preset(X265Preset.Slower)
            .tune(Tune.ZeryLatency)
            .gop(120)
            .bFrames(2);
    }

    // ---------- QSV ----------

    public static HevcQsvVideoRecordConfig.HevcQsvVideoRecordConfigBuilder qsv1080p30Balanced() {
        return HevcQsvVideoRecordConfig.builder()
            .width(1920)
            .height(1080)
            .fps(30.0)
            .pixelFormat(PixelFormat.YUV420P)
            .colorRange(ColorRange.Limited)
            .container(Container.MP4)
            .targetUsage(TargetUsage.Medium)
            .rcMode(RateControl.CBR)
            .bitrateKbps(5000)
            .gop(60) // 2s @30fps
            .lowPower(true);
    }

    // ---------- AMF ----------

    public static HevcAmfVideoRecordConfig.HevcAmfVideoRecordConfigBuilder amf1080p60LowLatency() {
        return HevcAmfVideoRecordConfig.builder()
            .width(1920)
            .height(1080)
            .fps(60.0)
            .pixelFormat(PixelFormat.YUV420P)
            .colorRange(ColorRange.Limited)
            .container(Container.MP4)
            .targetUsage(TargetUsage.LowLatency)
            .rcMode(RateControl.CBR)
            .bitrateKbps(8000)
            .gop(120)
            .bFrames(0)
            .main10(false);
    }
}

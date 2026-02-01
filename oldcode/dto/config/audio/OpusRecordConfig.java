package com.nz.sr.dto.config.audio;

import com.nz.sr.record.dto.RecordAudioCodec;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OpusRecordConfig extends BaseAudioRecordConfig {
    // libopus options
    private final Boolean vbr;               // -vbr on/off
    private final Integer complexity;        // -compression_level (0..10)
    private final Integer frameDurationMs;   // -frame_duration (ex: 20)
    private final Boolean music;             // -application audio|voip (true = audio)

    @Override
    public List<String> toFfmpegArgs() {
        List<String> args = commonArgs();
        args.add("-c:a"); args.add("libopus");
        if (bitrate != null) { args.add("-b:a"); args.add(bitrate + ""); }
        if (vbr != null) { args.add("-vbr"); args.add(vbr ? "on" : "off"); }
        if (complexity != null) { args.add("-compression_level"); args.add(complexity.toString()); }
        if (frameDurationMs != null) { args.add("-frame_duration"); args.add(frameDurationMs.toString()); }
        if (music != null) { args.add("-application"); args.add(music ? "audio" : "voip"); }
        return args;
    }

    public static OpusRecordConfig high() {
        return OpusRecordConfig.builder()
            .codec(RecordAudioCodec.OPUS).sampleRate(48_000).channels(2)
            .sampleFormat("fltp").bitrate(128_000)
            .vbr(true).complexity(10).frameDurationMs(20).music(true)
            .build();
    }
    public static OpusRecordConfig medium() {
        return OpusRecordConfig.builder()
            .codec(RecordAudioCodec.OPUS).sampleRate(48_000).channels(2)
            .sampleFormat("fltp").bitrate(96_000)
            .vbr(true).complexity(8).frameDurationMs(20).music(true)
            .build();
    }
    public static OpusRecordConfig low() {
        return OpusRecordConfig.builder()
            .codec(RecordAudioCodec.OPUS).sampleRate(48_000).channels(1)
            .sampleFormat("fltp").bitrate(64_000)
            .vbr(true).complexity(6).frameDurationMs(20).music(false)
            .build();
    }
}

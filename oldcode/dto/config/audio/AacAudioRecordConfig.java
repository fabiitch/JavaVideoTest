package com.nz.sr.dto.config.audio;

import com.nz.sr.record.dto.RecordAudioCodec;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AacAudioRecordConfig extends BaseAudioRecordConfig{
    // ex: aac_low, aac_he, etc. (optionnel)
    private final String profile; // null = défaut
    @Override
    public List<String> toFfmpegArgs() {
        List<String> args = commonArgs();
        args.add("-c:a");
        args.add("aac");
        if (bitrate != null) {
            args.add("-b:a");
            args.add(bitrate + "");
        }
        if (profile != null) {
            args.add("-profile:a");
            args.add(profile);
        }
        // AAC attend souvent fltp; laisse sampleFormat choisir, sinon passe null et FFmpeg adaptera.
        return args;
    }

    // Presets statiques
    public static AacAudioRecordConfig high() {
        return AacAudioRecordConfig.builder()
            .codec(RecordAudioCodec.AAC).sampleRate(48_000).channels(2)
            .sampleFormat("fltp").bitrate(192_000).profile(null)
            .build();
    }

    public static AacAudioRecordConfig medium() {
        return AacAudioRecordConfig.builder()
            .codec(RecordAudioCodec.AAC).sampleRate(44_100).channels(2)
            .sampleFormat("fltp").bitrate(128_000).profile(null)
            .build();
    }

    public static AacAudioRecordConfig low() {
        return AacAudioRecordConfig.builder()
            .codec(RecordAudioCodec.AAC).sampleRate(22_050).channels(1)
            .sampleFormat("fltp").bitrate(64_000).profile(null)
            .build();
    }
}

package com.nz.sr.dto.config.audio;

import com.nz.sr.record.dto.RecordAudioCodec;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PcmRecordConfig extends BaseAudioRecordConfig {
    // Choisis selon conteneur: s16le/s24le/f32le
    private final String pcmCodec; // ex: "pcm_s16le" (si null → s16le par défaut)

    @Override
    public List<String> toFfmpegArgs() {
        List<String> args = commonArgs();
        args.add("-c:a"); args.add(pcmCodec != null ? pcmCodec : "pcm_s16le");
        // bitrate ignoré (lossless)
        return args;
    }

    public static PcmRecordConfig high() {
        return PcmRecordConfig.builder()
            .codec(RecordAudioCodec.PCM).sampleRate(48_000).channels(2)
            .sampleFormat("s16").pcmCodec("pcm_s16le").bitrate(null)
            .build();
    }
    public static PcmRecordConfig medium() {
        return PcmRecordConfig.builder()
            .codec(RecordAudioCodec.PCM).sampleRate(44_100).channels(2)
            .sampleFormat("s16").pcmCodec("pcm_s16le").bitrate(null)
            .build();
    }
    public static PcmRecordConfig low() {
        return PcmRecordConfig.builder()
            .codec(RecordAudioCodec.PCM).sampleRate(22_050).channels(1)
            .sampleFormat("s16").pcmCodec("pcm_s16le").bitrate(null)
            .build();
    }
}

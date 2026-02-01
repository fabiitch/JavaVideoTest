package com.nz.sr.dto.config.audio;

import com.nz.sr.record.dto.RecordAudioCodec;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@EqualsAndHashCode
public abstract class BaseAudioRecordConfig {
    protected final RecordAudioCodec codec;
    protected final int sampleRate;      // -ar
    protected final int channels;        // -ac
    protected final String sampleFormat; // -sample_fmt (ex: fltp, s16, f32) — optionnel mais utile
    protected final Integer bitrate;     // -b:a (peut être ignoré pour PCM)


    /** Options FFmpeg côté sortie (sans les inputs). */
    public abstract List<String> toFfmpegArgs();

    /** Tronc commun utile. */
    protected List<String> commonArgs() {
        List<String> args = new ArrayList<>();
        args.add("-ar"); args.add(String.valueOf(sampleRate));
        args.add("-ac"); args.add(String.valueOf(channels));
        if (sampleFormat != null) {
            args.add("-sample_fmt"); args.add(sampleFormat);
        }
        return args;
    }
}

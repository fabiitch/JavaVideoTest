package com.nz.sr.ffmpeg.wrapper.config.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FFmpegCodec {

    private final String name;
    private final String description;
    private final List<MediaType> type;
    private final boolean canEncode;
    private final boolean canDecode;
    private final String rawFlags;

    private final boolean intraFramOnly;
    private final boolean lossyCompression;
    private final boolean losslessCompression;
    private final List<FFmpegEncoder> encoders = new ArrayList<>();
    private final List<FFmpegEncoder> decoders = new ArrayList<>();

    public FFmpegCodec(String name, String description, List<MediaType> type, boolean canEncode, boolean canDecode, String rawFlags, boolean intraFramOnly, boolean lossyCompression, boolean losslessCompression) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.canEncode = canEncode;
        this.canDecode = canDecode;
        this.rawFlags = rawFlags;
        this.intraFramOnly = intraFramOnly;
        this.lossyCompression = lossyCompression;
        this.losslessCompression = losslessCompression;
    }
}

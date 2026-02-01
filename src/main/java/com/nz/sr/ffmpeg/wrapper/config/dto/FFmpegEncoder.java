package com.nz.sr.ffmpeg.wrapper.config.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class FFmpegEncoder {

    private String name;
    private String desc;
    private String codecName;
    @Setter
    private FFmpegCodec codec;
    private final MediaType mediaType;
    private final boolean frameLevelMultithreading;
    private final boolean sliceLevelMultithreading;
    private final boolean experimental;
    private final boolean supportDrawHorizBand;
    private final boolean supportDirectRenderingMethod1;

    private final String rawFlags;

    public FFmpegEncoder(String name, String desc, String codecName,
                         MediaType mediaType, boolean frameLevelMultithreading,
                         boolean sliceLevelMultithreading, boolean experimental,
                         boolean supportDrawHorizBand, boolean supportDirectRenderingMethod1,
                         String rawFlags) {
        this.name = name;
        this.desc = desc;
        this.codecName = codecName;
        this.mediaType = mediaType;
        this.frameLevelMultithreading = frameLevelMultithreading;
        this.sliceLevelMultithreading = sliceLevelMultithreading;
        this.experimental = experimental;
        this.supportDrawHorizBand = supportDrawHorizBand;
        this.supportDirectRenderingMethod1 = supportDirectRenderingMethod1;
        this.rawFlags = rawFlags;
    }
}

package com.nz.sr.ffmpeg.wrapper.config.parser;

import com.nz.sr.ffmpeg.wrapper.config.dto.MediaType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FFmpegParserUtils {
    public static MediaType getMediaType(char c) {
        if (c == 'V') return MediaType.VIDEO;
        if (c == 'A') return MediaType.AUDIO;
        if (c == 'S') return MediaType.SUBTITLE;
        if (c == 'D') return MediaType.DATA;
        if (c == 'T') return MediaType.Attachment;
        if (c == '.') return null;
        return MediaType.Unknown;
    }
}

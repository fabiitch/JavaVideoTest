package com.nz.sr.ffmpeg.wrapper.config.parser;


import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegCodec;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegEncoder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FFmpegEncoderCodecMapper {


    public static Map<String, FFmpegCodec> merge(List<FFmpegCodec> codecs, Collection<FFmpegEncoder> encoders, Collection<FFmpegEncoder> decoders) {
        Map<String, FFmpegCodec> codecMap = codecs.stream().collect(Collectors.toMap(FFmpegCodec::getName, c -> c));

        mapEncoders(encoders, codecMap, true);
        mapEncoders(decoders, codecMap, false);
        return codecMap;
    }

    private static void mapEncoders(Collection<FFmpegEncoder> encoders, Map<String, FFmpegCodec> codecMap, boolean isEncoder) {
        Pattern CODEC_PATTERN = Pattern.compile("\\(codec\\s+([^)]+)\\)");
        for (FFmpegEncoder encoder : encoders) {
            String desc = encoder.getDesc();
            Matcher m = CODEC_PATTERN.matcher(desc);
            if (m.find()) {
                String codecname = m.group(1).trim(); // exemple: "h264"
                FFmpegCodec fFmpegCodec = codecMap.get(codecname);
                if (fFmpegCodec != null) {
                    if (isEncoder) {
                        fFmpegCodec.getEncoders().add(encoder);
                    } else {
                        fFmpegCodec.getDecoders().add(encoder);
                    }
                    encoder.setCodec(fFmpegCodec);
                }
            } else {
                FFmpegCodec fFmpegCodec = codecMap.get(encoder.getName());
                if (fFmpegCodec != null) {
                    if (isEncoder) {
                        fFmpegCodec.getEncoders().add(encoder);
                    } else {
                        fFmpegCodec.getDecoders().add(encoder);
                    }
                    encoder.setCodec(fFmpegCodec);
                } else {
                    System.out.println("unknown codec: " + encoder.getName());
                }
            }
        }
    }
}

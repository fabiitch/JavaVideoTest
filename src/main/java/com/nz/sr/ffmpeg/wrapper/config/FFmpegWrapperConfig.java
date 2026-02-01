package com.nz.sr.ffmpeg.wrapper.config;

import com.nz.sr.ffmpeg.wrapper.FFmpegWrapper;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegCodec;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegEncoder;
import com.nz.sr.ffmpeg.wrapper.config.parser.FFmpegEncoderCodecMapper;
import com.nz.sr.ffmpeg.wrapper.config.provider.FFmpegConfigProvider;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
public class FFmpegWrapperConfig {
    private final FFmpegWrapper wrapper;

    private final List<FFmpegCodec> codecs;
    private final Map<String, FFmpegCodec> codecsMap;

    private final Map<String, FFmpegEncoder> encodersMap;
    private final Map<String, FFmpegEncoder> decodersMap;

    public FFmpegWrapperConfig(FFmpegWrapper wrapper) {
        this.wrapper = wrapper;

        FFmpegConfigProvider configProvider = new FFmpegConfigProvider();
        this.codecs = configProvider.getCodecs(wrapper);
        this.encodersMap = configProvider.getEncoders(wrapper).stream().collect(Collectors.toMap(FFmpegEncoder::getName, c -> c));
        this.decodersMap = configProvider.getDecoders(wrapper).stream().collect(Collectors.toMap(FFmpegEncoder::getName, c -> c));
        this.codecsMap = FFmpegEncoderCodecMapper.merge(codecs, encodersMap.values(), decodersMap.values());
    }


    public FFmpegCodec getFfmpegCodec(String codecName) {
        return codecsMap.get(codecName);
    }

    public FFmpegEncoder getEncoder(String encoder) {
        return encodersMap.get(encoder);
    }

    public FFmpegEncoder getEncoder(String codec, String encoder) {
        FFmpegCodec fFmpegCodec = codecsMap.get(codec);
        if (fFmpegCodec == null) {
            return null;
        }
        return fFmpegCodec.getEncoders().stream().filter(e -> e.getName().equals(encoder)).findFirst().orElse(null);
    }

    public FFmpegEncoder getDecoder(String h264) {
      return decodersMap.get(h264);
    }
}

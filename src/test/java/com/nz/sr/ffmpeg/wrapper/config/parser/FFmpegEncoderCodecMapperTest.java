package com.nz.sr.ffmpeg.wrapper.config.parser;

import com.nz.sr.RessourceUtils;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegCodec;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegEncoder;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FFmpegEncoderCodecMapperTest {

    @Test
    public void shouldAttachEncodersAndDecodersToCodecs() {
        String codecOutput = readOutput("codecs-output.txt");
        List<FFmpegCodec> codecs = FFmpegCodecResultParser.parse(codecOutput);

        String encodersOutput = readOutput("encoders-output.txt");
        List<FFmpegEncoder> encoders = FFmpegEncodersResultParser.parse(encodersOutput);

        String decoderOutput = readOutput("decoders-output.txt");
        List<FFmpegEncoder> decoders = FFmpegEncodersResultParser.parse(decoderOutput);

        FFmpegEncoderCodecMapper.merge(codecs, encoders, decoders);

    }

    private String readOutput(String fileName) {
        String resourcePath = "ffmpeg/output/" + fileName;
        return RessourceUtils.readRessourceAsString(resourcePath);
    }

}

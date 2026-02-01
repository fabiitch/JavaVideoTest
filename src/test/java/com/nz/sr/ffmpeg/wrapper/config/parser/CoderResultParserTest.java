package com.nz.sr.ffmpeg.wrapper.config.parser;

import com.nz.sr.RessourceUtils;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegCodec;
import com.nz.sr.ffmpeg.wrapper.config.dto.MediaType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CoderResultParserTest {

    @Test
    void shouldParseFfmpegCodecsOutput() throws IOException {
        String output = readOutput("codecs-output.txt");
        List<FFmpegCodec> codecs = FFmpegCodecResultParser.parse(output);

        assertFalse(codecs.isEmpty(), "Expected codecs to be parsed");

        FFmpegCodec first = codecs.get(0);
        assertEquals("012v", first.getName());
        assertEquals("Uncompressed 4:2:2 10-bit", first.getDescription());
        assertTrue(first.isCanDecode());
        assertFalse(first.isCanEncode());
        assertEquals("D.VI.S", first.getRawFlags());
        assertTrue(first.getType().contains(MediaType.VIDEO));
        assertTrue(first.getType().contains(MediaType.SUBTITLE));

        FFmpegCodec a64Multi = codecs.stream()
            .filter(codec -> codec.getName().equals("a64_multi"))
            .findFirst()
            .orElseThrow(()-> new RuntimeException("test fail"));

        assertTrue(a64Multi.isCanEncode());
        assertFalse(a64Multi.isCanDecode());
        assertTrue(a64Multi.getType().contains(MediaType.Unknown));
        assertTrue(a64Multi.getType().contains(MediaType.VIDEO));
    }

    private String readOutput(String fileName) {
        String resourcePath = "ffmpeg/output/" + fileName;
        return RessourceUtils.readRessourceAsString(resourcePath);
    }
}

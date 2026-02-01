package com.nz.sr.ffmpeg.wrapper.config.parser;

import com.nz.sr.RessourceUtils;
import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegEncoder;
import com.nz.sr.ffmpeg.wrapper.config.dto.MediaType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FFmpegEncodersResultParserTest {

    @Test
    void shouldParseFfmpegEncodersOutput() throws IOException {
        String output = readOutput("encoders-output.txt");

        List<FFmpegEncoder> encoders = FFmpegEncodersResultParser.parse(output);

        assertFalse(encoders.isEmpty(), "Expected encoders to be parsed");

        FFmpegEncoder first = encoders.get(0);
        assertEquals("a64multi", first.getName());
        assertEquals(MediaType.VIDEO, first.getMediaType());
        assertEquals("V....D", first.getRawFlags());
        assertFalse(first.isFrameLevelMultithreading());
        assertFalse(first.isSliceLevelMultithreading());
        assertFalse(first.isExperimental());
        assertFalse(first.isSupportDrawHorizBand());
        assertTrue(first.isSupportDirectRenderingMethod1());
    }

    @Test
    void shouldParseFfmpegDecodersOutput() throws Throwable {
        String output = readOutput("decoders-output.txt");

        List<FFmpegEncoder> decoders = FFmpegEncodersResultParser.parse(output);

        assertFalse(decoders.isEmpty(), "Expected decoders to be parsed");

        FFmpegEncoder firstDecoder = decoders.get(0);
        assertEquals("012v", firstDecoder.getName());
        assertEquals(MediaType.VIDEO, firstDecoder.getMediaType());
        assertTrue(firstDecoder.isSupportDirectRenderingMethod1());

        FFmpegEncoder threadedDecoder = decoders.stream()
            .filter(decoder -> decoder.getName().equals("aic"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("test fail"));

        assertTrue(threadedDecoder.isFrameLevelMultithreading());
        assertFalse(threadedDecoder.isSliceLevelMultithreading());
        assertEquals("VF...D", threadedDecoder.getRawFlags());
    }

    private String readOutput(String fileName) {
        String resourcePath = "ffmpeg/output/" + fileName;
        return RessourceUtils.readRessourceAsString(resourcePath);
    }
}

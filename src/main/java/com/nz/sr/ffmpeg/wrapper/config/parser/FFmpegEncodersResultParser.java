package com.nz.sr.ffmpeg.wrapper.config.parser;


import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegEncoder;
import com.nz.sr.ffmpeg.wrapper.config.dto.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegEncodersResultParser {

    private final static Pattern CODEC_PATTERN = Pattern.compile("\\(codec\\s+([^)]+)\\)");

    /**
     * Encoders:
     * V..... = Video
     * A..... = Audio
     * S..... = Subtitle
     * .F.... = Frame-level multithreading
     * ..S... = Slice-level multithreading
     * ...X.. = Codec is experimental
     * ....B. = Supports draw_horiz_band
     * .....D = Supports direct rendering method 1
     * ------
     *
     * @param ffmpegOutput
     * @return
     */
    public static List<FFmpegEncoder> parse(String ffmpegOutput) {
        List<FFmpegEncoder> result = new ArrayList<>();
        boolean separatorFound = false;
        for (String line : ffmpegOutput.split("\\R")) { // \R = regex pour n'importe quel séparateur de ligne
            if (!separatorFound) {
                if (line.contains("------")) {
                    separatorFound = true;
                }
            } else {
                FFmpegEncoder encoder = parseLine(line);
                result.add(encoder);
            }

        }
        return result;
    }

    //         * Encoders:
//        *  V..... = Video
//     *  A..... = Audio
//     *  S..... = Subtitle
//     *  .F.... = Frame-level multithreading
//     *  ..S... = Slice-level multithreading
//     *  ...X.. = Codec is experimental
//     *  ....B. = Supports draw_horiz_band
//     *  .....D = Supports direct rendering method 1
    public static FFmpegEncoder parseLine(String line) {
        String rawFlags = line.substring(0, 7).trim();
        String[] parts = line.trim().split("\\s+", 3);
        String name = parts[1];
        String description = parts.length > 2 ? parts[2] : "";

        MediaType mediaType = FFmpegParserUtils.getMediaType(rawFlags.charAt(0));

        boolean frameLevelMultithreading = rawFlags.charAt(1) == 'F';
        boolean sliceLevelMultithreading = rawFlags.charAt(2) == 'S';
        boolean experimental = rawFlags.charAt(3) == 'X';
        boolean supportDrawHorizBand = rawFlags.charAt(4) == 'B';
        boolean supportDirectRenderingMethod1 = rawFlags.charAt(5) == 'D';

        String codecName = name;
        Matcher m = CODEC_PATTERN.matcher(description);
        if (m.find()) {
            codecName = m.group(1).trim(); // exemple: "h264"
        }

        return new FFmpegEncoder(
            name,
            description,
            codecName,
            mediaType,
            frameLevelMultithreading,
            sliceLevelMultithreading,
            experimental,
            supportDrawHorizBand,
            supportDirectRenderingMethod1,
            rawFlags
        );
    }




}

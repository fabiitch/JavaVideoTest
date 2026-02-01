package com.nz.sr.ffmpeg.wrapper.config.parser;


import com.nz.sr.ffmpeg.wrapper.config.dto.FFmpegCodec;
import com.nz.sr.ffmpeg.wrapper.config.dto.MediaType;

import java.util.ArrayList;
import java.util.List;

public class FFmpegCodecResultParser {

    /**
     * D..... = Decoding supported
     * .E.... = Encoding supported
     * ..V... = Video codec
     * ..A... = Audio codec
     * ..S... = Subtitle codec
     * ..D... = Data codec
     * ..T... = Attachment codec
     * ...I.. = Intra frame-only codec
     * ....L. = Lossy compression
     * .....S = Lossless compression
     */
    public static List<FFmpegCodec> parse(String ffmpegOutput) {
        List<FFmpegCodec> result = new ArrayList<>();
        boolean separatorFound = false;
        for (String line : ffmpegOutput.split("\\R")) { // \R = regex pour n'importe quel séparateur de ligne
            if (!separatorFound) {
                if (line.contains("-------")) {
                    separatorFound = true;
                }
            } else {
                FFmpegCodec codec = parseLine(line);
                result.add(codec);
            }

        }
        return result;
    }

    public static FFmpegCodec parseLine(String line) {
        String rawFlags = line.substring(0, 7).trim();
        String[] parts = line.trim().split("\\s+", 3);
        String name = parts[1];
        String description = parts.length > 2 ? parts[2] : "";

        boolean canDecode = rawFlags.charAt(0) == 'D';
        boolean canEncode = rawFlags.charAt(1) == 'E';


        List<MediaType> mediaTypes = parseMedia(rawFlags);

        boolean intraFrameOnly = rawFlags.length() > 3 && rawFlags.charAt(3) == 'I';
        boolean lossyCompression = rawFlags.length() > 4 && rawFlags.charAt(4) == 'L';
        boolean losslessCompression = rawFlags.length() > 5 && rawFlags.charAt(5) == 'S';

        return new FFmpegCodec(
            name,
            description,
            mediaTypes,
            canEncode,
            canDecode,
            rawFlags,
            intraFrameOnly,
            lossyCompression,
            losslessCompression
        );
    }

    public static List<MediaType> parseMedia(String rawFlags) {
        List<MediaType> types = new ArrayList<>();
        for (int i = 0; i < rawFlags.length(); i++) {
            MediaType type = FFmpegParserUtils.getMediaType(rawFlags.charAt(i));
            if (type != null) {
                types.add(type);
            }
        }
        return types;
    }


}

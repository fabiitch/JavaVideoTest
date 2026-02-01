package com.nz.sr.dto.config.video;

import com.nz.sr.dto.video.*;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder(toBuilder = true)
/**
 * Base immuable pour un enregistrement vidéo.
 * - Utilise un builder "protected" pour la base
 * - Spécifique encodeur géré par sous-classes
 * - Génère les args FFmpeg via toFfmpegArgs()
 */
public abstract class BaseVideoRecordConfig {

    // commun à tous
    protected final int width;
    protected final int height;
    protected final double fps;
    protected final Container container;
    protected final PixelFormat pixelFormat;
    protected final ColorRange colorRange;

    // GOP / B-frames & co
    protected final Integer gop;         // keyint
    protected final Integer bFrames;     // nb B
    protected final Integer maxrateKbps; // pour CBR/VBR caps
    protected final Integer bufsizeKbps; // VBV buffer

    // RC générique
    protected final RateControl rcMode;
    protected final Integer crf;     // pour CRF (x264)
    protected final Integer bitrateKbps; // pour CBR/VBR
    protected final Integer qp;      // pour CQP (NVENC/AMF)
    protected final TargetUsage targetUsage;

    protected final Profile profile;
    protected final Tune tune;

    public abstract String videoCodecName();        // ex "libx264", "h264_nvenc", "h264_amf"

    protected abstract List<String> encoderSpecificArgs(); // args propres à l’encodeur (preset, rc flags…)

    /**
     * Construit les arguments FFmpeg pour la partie vidéo (sans -i input / -f etc.).
     */
    public final List<String> toFfmpegArgs(String outputPath) {
        List<String> args = new ArrayList<>(List.of(
            "-r", String.valueOf(fps),
            "-s", width + "x" + height,
            "-pix_fmt", pixelFormat.getFf(),
            "-color_range", colorRange.getFf(),
            "-c:v", videoCodecName()
        ));
        // GOP / B
        if (gop != null) args.addAll(List.of("-g", String.valueOf(gop)));
        if (bFrames != null) args.addAll(List.of("-bf", String.valueOf(bFrames)));

        // RC générique
        List<String> rcArgs = rcMode == null ? List.of() : switch (rcMode) {
            case CRF -> List.of("-crf", String.valueOf(crf));
            case CQP -> List.of(); // géré par l’encodeur (QP flag)
            case CBR -> List.of("-b:v", bitrateKbps + "k", "-minrate", bitrateKbps + "k", "-maxrate", (maxrateKbps != null ? maxrateKbps : bitrateKbps) + "k");
            case VBR -> List.of("-b:v", bitrateKbps + "k"); // cap optionnels
        };
        args.addAll(rcArgs);

        if (bufsizeKbps != null) args.addAll(List.of("-bufsize", bufsizeKbps + "k"));

        // Profile/tune (si l’encodeur les supporte, nombre les mappe encoderSpecificArgs)
        args.addAll(encoderSpecificArgs());

        // container
        List<String> containerArgs = container == null ? List.of() : switch (container) {
            case MP4 -> List.of("-f", "mp4");
            case MKV -> List.of("-f", "matroska");
            case MOV -> List.of("-f", "mov");
            case FLV -> List.of("-f", "flv");
        };
        args.addAll(containerArgs);
        return args;
    }
}

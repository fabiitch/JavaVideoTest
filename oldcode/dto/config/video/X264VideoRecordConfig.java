package com.nz.sr.dto.config.video;

import com.nz.sr.dto.video.Profile;
import com.nz.sr.dto.video.Tune;
import com.nz.sr.dto.video.preset.X264Preset;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Getter
@SuperBuilder(toBuilder = true)
public class X264VideoRecordConfig extends BaseVideoRecordConfig {


    @Override
    public String videoCodecName() {
        return "libx264";
    }

    @Override
    protected List<String> encoderSpecificArgs() {
        List<String> a = new ArrayList<>();

        // preset depuis TargetUsage (rapide pour le screen record)
        if (targetUsage != null) {
            X264Preset preset = X264Preset.from(targetUsage);
            a.addAll(preset.toFfmpegArgs());
        }

        // profile (baseline/main/high…)
        if (profile != null) {
            a.addAll(Arrays.asList("-profile:v", mapX264Profile(profile)));
        }

        // tune (zerolatency très courant en capture écran)
        if (tune != null) {
            a.addAll(Arrays.asList("-tune", mapX264Tune(tune)));
        }

        // RC : CRF déjà géré par la base via -crf. CBR/VBR via -b:v déjà posé.
        // CQP non pertinent pour x264 => rien à faire.

        return a;
    }

    private String mapX264Profile(Profile p) {
        switch (p) {
            case BASELINE:
                return "baseline";
            case MAIN:
                return "main";
            case HIGH:
                return "high";
            default:
                return "high";
        }
    }

    private String mapX264Tune(Tune t) {
        switch (t) {
            case ZeryLatency:
                return "zerolatency";
            case Film:
                return "film";
            case Animation:
                return "animation";
            case Grain:
                return "grain";
            case StillImage:
                return "stillimage";
            case FastDecode:
                return "fastdecode";
            default:
                return "zerolatency";
        }
    }
}

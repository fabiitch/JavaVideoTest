package com.nz.sr.dto.config.video;
import com.nz.sr.dto.video.Profile;
import com.nz.sr.dto.video.Tune;
import com.nz.sr.dto.video.preset.X265Preset;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@SuperBuilder(toBuilder = true)
public class X265VideoRecordConfig extends BaseVideoRecordConfig {

    // optionnel : contrôle fin
    private final X265Preset preset; // si null → map depuis TargetUsage
    private final Boolean main10;    // yuv420p10le + profile main10 si true

    @Override
    public String videoCodecName() { return "libx265"; }

    @Override
    protected List<String> encoderSpecificArgs() {
        List<String> a = new ArrayList<>();

        // preset
        X265Preset effPreset = (preset != null) ? preset : X265Preset.from(targetUsage);
        if (effPreset != null) a.addAll(effPreset.toFfmpegArgs());

        // profile main / main10
        if (Boolean.TRUE.equals(main10)) {
            a.addAll(Arrays.asList("-profile:v", "main10"));
            // (si besoin: forcer -pix_fmt yuv420p10le dans la base / builder)
        } else if (profile != null) {
            a.addAll(Arrays.asList("-profile:v", mapHevcProfile(profile)));
        }

        // tune (x265 supporte zerolatency, grain, etc.)
        if (tune != null) {
            String t = mapX265Tune(tune);
            if (t != null) a.addAll(Arrays.asList("-tune", t));
        }

        // RC: CRF déjà géré par la base (-crf). CBR/VBR posés par la base (-b:v etc.)
        // CQP pas pertinent pour x265.

        return a;
    }

    private String mapHevcProfile(Profile p) {
        // pour le screen record, on garde simple
        switch (p) {
            case MAIN:
            case HIGH:
            default:       return "main";
        }
    }

    private String mapX265Tune(Tune t) {
        switch (t) {
            case ZeryLatency: return "zerolatency";
            case Grain:       return "grain";
            case FastDecode:  return "fastdecode";
            case Film:        return "film";
            default:          return null;
        }
    }
}

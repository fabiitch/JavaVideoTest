package com.nz.sr.dto.config.video;

import com.nz.sr.dto.video.Tune;
import com.nz.sr.dto.video.preset.AmfUsage;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@SuperBuilder(toBuilder = true)
public class HevcAmfVideoRecordConfig extends BaseVideoRecordConfig {

    private final AmfUsage usage;   // si null → map depuis TargetUsage
    private final Boolean main10;   // profile main10

    @Override
    public String videoCodecName() { return "hevc_amf"; }

    @Override
    protected List<String> encoderSpecificArgs() {
        List<String> a = new ArrayList<>();

        // usage
        AmfUsage eff = (usage != null) ? usage : AmfUsage.from(targetUsage);
        if (eff != null) a.addAll(eff.toFfmpegArgs()); // -> -usage X

        // profil
        if (Boolean.TRUE.equals(main10)) {
            a.addAll(Arrays.asList("-profile:v", "main10"));
        } else if (profile != null) {
            a.addAll(Arrays.asList("-profile:v", "main"));
        }

        // RC
        List<String> rcArgs = rcMode == null ? List.of() : switch (rcMode) {
            case CBR -> List.of("-rc", "cbr");
            case VBR -> List.of("-rc", "vbr");
            case CQP -> {
                List<String> qpArgs = new ArrayList<>(List.of("-rc", "cqp"));
                if (qp != null) qpArgs.addAll(List.of("-qp_i", String.valueOf(qp), "-qp_p", String.valueOf(qp)));
                if (bFrames != null && bFrames > 0 && qp != null) qpArgs.addAll(List.of("-qp_b", String.valueOf(qp)));
                yield List.copyOf(qpArgs);
            }
            case CRF -> List.of("-rc", "vbr"); // CRF “pur” pas dispo → VBR par défaut
        };
        a.addAll(rcArgs);

        // tune low-latency
        if (tune == Tune.ZeryLatency) {
            a.addAll(Arrays.asList("-preanalysis", "0"));
        }

        return a;
    }
}

package com.nz.sr.dto.config.video;

import com.nz.sr.dto.video.Tune;
import com.nz.sr.dto.video.preset.NvencPreset;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@SuperBuilder(toBuilder = true)
public class HevcNvencVideoRecordConfig extends BaseVideoRecordConfig {

    private final NvencPreset preset; // si null → map depuis TargetUsage
    private final Boolean main10;     // profile main10
    private final Boolean zeroLatency;
    private final Integer lookahead;  // -rc-lookahead N

    @Override
    public String videoCodecName() { return "hevc_nvenc"; }

    @Override
    protected List<String> encoderSpecificArgs() {
        List<String> a = new ArrayList<>();

        // preset p1..p7
        NvencPreset eff = (preset != null) ? preset : NvencPreset.from(targetUsage);
        if (eff != null) a.addAll(eff.toFfmpegArgs());

        // profil
        if (Boolean.TRUE.equals(main10)) {
            a.addAll(Arrays.asList("-profile:v", "main10"));
        } else if (profile != null) {
            a.addAll(Arrays.asList("-profile:v", "main"));
        }

        // tune ll/hq (low latency vs quality)
        if (tune == Tune.ZeryLatency || Boolean.TRUE.equals(zeroLatency)) {
            a.addAll(Arrays.asList("-tune", "ll"));
        } else if (tune != null) {
            a.addAll(Arrays.asList("-tune", "hq"));
        }

        // RC mode
        List<String> rcArgs = rcMode == null ? List.of() : switch (rcMode) {
            case CBR -> List.of("-rc:v", "cbr");
            case VBR -> List.of("-rc:v", "vbr");
            case CQP -> {
                List<String> qpArgs = new ArrayList<>(List.of("-rc:v", "constqp"));
                if (qp != null) qpArgs.addAll(List.of("-qp", String.valueOf(qp)));
                yield List.copyOf(qpArgs);
            }
            case CRF -> {
                List<String> crfArgs = new ArrayList<>(List.of("-rc:v", "vbr"));
                if (crf != null) crfArgs.addAll(List.of("-cq", String.valueOf(Math.max(0, Math.min(51, crf)))));
                yield List.copyOf(crfArgs);
            }
        };
        a.addAll(rcArgs);

        if (lookahead != null) a.addAll(Arrays.asList("-rc-lookahead", String.valueOf(lookahead)));
        if (Boolean.TRUE.equals(zeroLatency)) a.addAll(Arrays.asList("-no-scenecut", "1"));

        return a;
    }
}

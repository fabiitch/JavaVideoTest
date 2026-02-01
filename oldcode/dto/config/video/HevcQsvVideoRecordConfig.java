package com.nz.sr.dto.config.video;
import com.nz.sr.dto.video.preset.QsvPreset;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@SuperBuilder(toBuilder = true)
public class HevcQsvVideoRecordConfig extends BaseVideoRecordConfig {

    private final QsvPreset preset;  // si null → map depuis TargetUsage
    private final Boolean main10;    // profile main10
    private final Boolean lowPower;  // -low_power 1

    @Override
    public String videoCodecName() { return "hevc_qsv"; }

    @Override
    protected List<String> encoderSpecificArgs() {
        List<String> a = new ArrayList<>();

        if (Boolean.TRUE.equals(lowPower)) a.addAll(Arrays.asList("-low_power", "1"));

        QsvPreset eff = (preset != null) ? preset : QsvPreset.from(targetUsage);
        if (eff != null) a.addAll(eff.toFfmpegArgs()); // -> -preset X

        if (Boolean.TRUE.equals(main10)) {
            a.addAll(Arrays.asList("-profile:v", "main10"));
        } else if (profile != null) {
            a.addAll(Arrays.asList("-profile:v", "main"));
        }

        // RC
        List<String> rcArgs = rcMode == null ? List.of() : switch (rcMode) {
            case CBR, VBR -> List.of(); // -b:v/-maxrate/-bufsize posés par la base
            case CQP -> qp != null ? List.of("-qp", String.valueOf(qp)) : List.of();
            case CRF -> List.of(); // hevc_qsv supporte ICQ/LA-ICQ via -global_quality, mais c’est variable
        };
        a.addAll(rcArgs);

        return a;
    }
}

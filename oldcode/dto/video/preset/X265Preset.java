package com.nz.sr.dto.video.preset;

import com.nz.sr.dto.video.TargetUsage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum X265Preset {
    UltraFast("ultrafast"),
    SuperFast("superfast"),
    VeryFast("veryfast"),
    Faster("faster"),
    Fast("fast"),
    Medium("medium"),
    Slow("slow"),
    Slower("slower"),
    VerySlow("veryslow"),
    Placebo("placebo");

    private final String ff;

    public static X265Preset from(TargetUsage usage) {
        if (usage == null) return Medium;
        switch (usage) {
            case UltraFast:
                return UltraFast;
            case VeryFast:
                return VeryFast;
            case Fast:
                return Fast;
            case Medium:
                return Medium;
            case Slow:
                return Slow;
            case Quality:
                return Slower;     // favorise qualité
            case LowLatency:
                return SuperFast;  // favorise latence
            default:
                return Medium;
        }
    }

    public java.util.List<String> toFfmpegArgs() {
        return java.util.Arrays.asList("-preset", ff);
    }

}

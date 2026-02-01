package com.nz.sr.dto.video.preset;

import com.nz.sr.dto.video.TargetUsage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum X264Preset {
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

    public static X264Preset from(TargetUsage usage) {
        if (usage == null) return Medium; // default safe
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
                return Slower; // qualité max → map vers Slower/VerySlow
            case LowLatency:
                return SuperFast; // low-latency → preset rapide
            default:
                return Medium;
        }
    }
    public java.util.List<String> toFfmpegArgs() {
        return java.util.Arrays.asList("-preset", ff);
    }
}

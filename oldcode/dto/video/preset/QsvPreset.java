package com.nz.sr.dto.video.preset;

import com.nz.sr.dto.video.TargetUsage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum QsvPreset {
    VeryFast("veryfast"),
    Fast("fast"),
    Medium("medium"),
    Slow("slow"),
    VerySlow("veryslow");

    private final String ff;

    public static QsvPreset from(TargetUsage usage) {
        if (usage == null) return Medium;
        switch (usage) {
            case UltraFast:
            case VeryFast:
                return VeryFast;
            case Fast:
                return Fast;
            case Medium:
                return Medium;
            case Slow:
                return Slow;
            case Quality:
                return VerySlow;
            case LowLatency:
                return VeryFast;
            default:
                return Medium;
        }
    }

    public java.util.List<String> toFfmpegArgs() {
        return java.util.Arrays.asList("-preset", ff);
    }

}

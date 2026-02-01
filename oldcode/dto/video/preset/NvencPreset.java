package com.nz.sr.dto.video.preset;

import com.nz.sr.dto.video.TargetUsage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum NvencPreset {
    P1("p1"), P2("p2"), P3("p3"), P4("p4"), P5("p5"), P6("p6"), P7("p7");
    private final String ff;

    public static NvencPreset from(TargetUsage usage) {
        if (usage == null) return P5;
        switch (usage) {
            case UltraFast:
                return P7;
            case VeryFast:
                return P6;
            case Fast:
                return P5;
            case Medium:
                return P4;
            case Slow:
                return P3;
            case Quality:
                return P1; // meilleure qualité
            case LowLatency:
                return P6; // plutôt rapide
            default:
                return P5;
        }
    }

    public java.util.List<String> toFfmpegArgs() {
        return java.util.Arrays.asList("-preset", ff);
    }
}

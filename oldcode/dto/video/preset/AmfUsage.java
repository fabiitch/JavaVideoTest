package com.nz.sr.dto.video.preset;

import com.nz.sr.dto.video.TargetUsage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AmfUsage {
    Transcoding("transcoding"),
    Quality("quality"),
    LowLatency("lowlatency"),
    UltraLowLatency("ultralowlatency");

    private final String ff;

    public static AmfUsage from(TargetUsage usage) {
        if (usage == null) return Quality;
        switch (usage) {
            case UltraFast:
            case VeryFast:
            case Fast:
                return UltraLowLatency;
            case Medium:
                return Transcoding;
            case Slow:
            case Quality:
                return Quality;
            case LowLatency:
                return LowLatency;
            default:
                return Quality;
        }
    }


    public java.util.List<String> toFfmpegArgs() {
        return java.util.Arrays.asList("-usage", ff);
    }
}

package com.nz.recorder.api.targets;

/** Capture the entire screen/monitor (id is backend-defined but stable during session). */
public record ScreenTarget(String screenId) implements CaptureTarget {
    public ScreenTarget {
        if (screenId == null || screenId.isBlank()) throw new IllegalArgumentException("screenId");
    }

    @Override
    public CaptureTargetType type() {
        return CaptureTargetType.SCREEN;
    }
}

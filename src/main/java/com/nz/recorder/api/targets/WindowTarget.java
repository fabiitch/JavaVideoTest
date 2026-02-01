package com.nz.recorder.api.targets;

/** Capture a specific window (id/handle is opaque to keep API agnostic). */
public record WindowTarget(String windowId) implements CaptureTarget {
    public WindowTarget {
        if (windowId == null || windowId.isBlank()) throw new IllegalArgumentException("windowId");
    }

    @Override
    public CaptureTargetType type() {
        return CaptureTargetType.WINDOW;
    }
}

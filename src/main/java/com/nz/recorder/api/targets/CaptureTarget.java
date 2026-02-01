package com.nz.recorder.api.targets;

/** Where to capture. */
public sealed interface CaptureTarget permits ScreenTarget, WindowTarget {
    CaptureTargetType type();
}

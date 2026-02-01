package com.nz.media.event;

public sealed interface VideoEvent
    permits BackendOpened, PositionChanged, BackendError, PlaybackStateChanged {
    long atNanos();
}

// -------- Events --------

// -------- Enum --------


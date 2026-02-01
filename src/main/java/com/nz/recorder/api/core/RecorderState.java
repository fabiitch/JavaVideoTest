package com.nz.recorder.api.core;

/**
 * Simple session state machine.
 */
public enum RecorderState {
    New,
    Idle,
    Starting,
    Recording,
    Paused,
    Stopping,
    Stopped,
    Closed,
    Error;

    public boolean in(RecorderState... states) {
        for (RecorderState state : states) {
            if (state == this) return true;
        }
        return false;
    }

    public boolean notIn(RecorderState... states) {
        return !in(states);
    }
}

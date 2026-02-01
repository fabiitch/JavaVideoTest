package com.nz.recorder.api.core;

import com.nz.recorder.api.subscriptions.RecorderSubscriptions;

/** Main control surface for a recording session. */
public interface Recorder extends AutoCloseable {
    RecorderState getState();

    void init();
    void start();
    void pause();
    void resume();

    /**
     * Stops the session (capture + encode) and releases runtime resources.
     * Depending on SaveStrategy, this may finalize an output or do nothing.
     */
    RecordingResult stop();

    RecorderSubscriptions events();

    @Override
    void close();
}

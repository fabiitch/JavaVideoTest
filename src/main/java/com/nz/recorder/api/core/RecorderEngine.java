package com.nz.recorder.api.core;

/** Entry point / factory for recorder instances (backend selection is internal). */
public interface RecorderEngine {
    Recorder create(RecorderSettings settings);

    static RecorderEngine systemDefault() {
        throw new UnsupportedOperationException("Provided by implementation module");
    }
}

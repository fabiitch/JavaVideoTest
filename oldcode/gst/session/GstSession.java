package com.nz.recorder.backend.gst.session;

import java.nio.file.Path;
import java.util.Optional;

public interface GstSession {
    void start();
    void pause();
    void resume();

    Optional<Path> stopAndFinalize();

    GstSessionMetrics metrics();
}

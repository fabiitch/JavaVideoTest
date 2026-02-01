package com.nz.recorder.api.targets;

/** Streaming target placeholder (agnostic). */
public record StreamTarget(String uri) {
    public StreamTarget {
        if (uri == null || uri.isBlank()) throw new IllegalArgumentException("uri");
    }
}

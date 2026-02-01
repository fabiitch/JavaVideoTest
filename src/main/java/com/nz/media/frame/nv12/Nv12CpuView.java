package com.nz.media.frame.nv12;

import java.nio.ByteBuffer;

/**
 * CPU-accessible NV12 planes.
 * Y plane full-res, UV plane half-res interleaved.
 */
public interface Nv12CpuView {
    ByteBuffer y();

    ByteBuffer uv();

    int yStrideBytes();

    int uvStrideBytes();
}


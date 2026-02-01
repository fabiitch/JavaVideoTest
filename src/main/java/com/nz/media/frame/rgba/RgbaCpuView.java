package com.nz.media.frame.rgba;

import java.nio.ByteBuffer;

public interface RgbaCpuView {

    ByteBuffer rgba();

    int strideBytes();
}

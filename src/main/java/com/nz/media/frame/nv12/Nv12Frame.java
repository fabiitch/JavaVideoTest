package com.nz.media.frame.nv12;

import com.nz.media.frame.VideoFrame;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

public final class Nv12Frame implements VideoFrame, Nv12CpuView {

    @Getter
    @Setter
    private int width, height;

    @Getter
    @Setter
    private long ptsNs;

    private ByteBuffer yBuf;
    private ByteBuffer uvBuf;

    @Getter
    @Setter
    private int yStride, uvStride;

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public long ptsNs() {
        return ptsNs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T view(Class<T> type) {
        if (type == Nv12CpuView.class) return (T) this;
        return null;
    }

    @Override
    public ByteBuffer y() {
        return yBuf;
    }

    @Override
    public ByteBuffer uv() {
        return uvBuf;
    }

    @Override
    public int yStrideBytes() {
        return yStride;
    }

    @Override
    public int uvStrideBytes() {
        return uvStride;
    }

    /**
     * Ensures buffers for a tightly packed NV12 frame (stride = width).
     */
    public void ensureCapacity(int w, int h) {
        int ySize = w * h;
        int uvSize = w * (h / 2);

        if (yBuf == null || yBuf.capacity() < ySize) {
            yBuf = ByteBuffer.allocateDirect(ySize);
        }
        if (uvBuf == null || uvBuf.capacity() < uvSize) {
            uvBuf = ByteBuffer.allocateDirect(uvSize);
        }
    }

    /**
     * Resets metadata for recycling (keeps buffers allocated).
     */
    @Override
    public void reset() {
        width = 0;
        height = 0;
        ptsNs = 0;
        yStride = 0;
        uvStride = 0;

        // optional: don't clear buffers; not needed for correctness
        // if (yBuf != null) yBuf.clear();
        // if (uvBuf != null) uvBuf.clear();
    }
}

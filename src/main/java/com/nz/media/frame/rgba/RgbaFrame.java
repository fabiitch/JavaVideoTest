package com.nz.media.frame.rgba;

import com.nz.media.frame.VideoFrame;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

public final class RgbaFrame implements VideoFrame, RgbaCpuView {

    @Getter
    @Setter
    private int width, height;

    @Getter
    @Setter
    private long ptsNs;

    private ByteBuffer rgba;

    @Getter
    @Setter
    private int strideBytes;

    @Getter
    private boolean allowGrow = true;

    @Getter
    private long reallocCount = 0L;

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
        if (type == RgbaCpuView.class) return (T) this;
        return null;
    }

    @Override
    public ByteBuffer rgba() {
        return rgba;
    }

    @Override
    public int strideBytes() {
        return strideBytes;
    }

    public void setAllowGrow(boolean allowGrow) {
        this.allowGrow = allowGrow;
    }

    public void clearBufferForWrite() {
        if (rgba != null) {
            rgba.clear();
        }
    }

    public void prepareForRead() {
        if (rgba != null) {
            rgba.flip();
        }
    }

    public void ensureCapacity(int w, int h) {
        int size = w * h * 4;
        if (!allowGrow) {
            return;
        }
        if (rgba == null || rgba.capacity() < size) {
            rgba = ByteBuffer.allocateDirect(size);
            reallocCount++;
        }
    }

    public void copyTo(RgbaFrame target) {
        if (target == null || rgba == null) return;
        target.ensureCapacity(width, height);
        ByteBuffer src = rgba.duplicate();
        src.rewind();
        target.clearBufferForWrite();
        target.rgba().put(src);
        target.prepareForRead();
        target.setWidth(width);
        target.setHeight(height);
        target.setPtsNs(ptsNs);
        target.setStrideBytes(strideBytes);
    }

    @Override
    public void reset() {
        width = 0;
        height = 0;
        ptsNs = 0L;
        strideBytes = 0;
    }
}

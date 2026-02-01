package com.nz.media.frame.external;


import com.nz.media.frame.VideoFrame;

public final class GlTextureFrame implements VideoFrame {

    private int textureId;
    private int width;
    private int height;
    private long ptsNs;

    public void set(int textureId, int width, int height, long ptsNs) {
        this.textureId = textureId;
        this.width = width;
        this.height = height;
        this.ptsNs = ptsNs;
    }

    public int textureId() { return textureId; }
    public int width() { return width; }
    public int height() { return height; }
    public long ptsNs() { return ptsNs; }

    @Override
    public <T> T view(Class<T> type) {
        return null;
    }

    public void reset() {
        textureId = 0;
        width = 0;
        height = 0;
        ptsNs = 0;
    }
}

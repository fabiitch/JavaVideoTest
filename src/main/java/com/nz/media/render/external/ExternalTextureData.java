package com.nz.media.render.external;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;

public final class ExternalTextureData implements TextureData {
    private int width;
    private int height;
    private Pixmap.Format format;

    public ExternalTextureData(int width, int height, Pixmap.Format format) {
        this.width = width;
        this.height = height;
        this.format = format;
    }

    public void update(int width, int height, Pixmap.Format format) {
        this.width = width;
        this.height = height;
        this.format = format;
    }

    @Override public TextureDataType getType() { return TextureDataType.Custom; }
    @Override public boolean isPrepared() { return true; }
    @Override public void prepare() { /* no-op */ }

    @Override public Pixmap consumePixmap() {
        throw new UnsupportedOperationException("External texture: no pixmap");
    }
    @Override public boolean disposePixmap() { return false; }

    @Override public void consumeCustomData(int target) {
        // NO-OP: we do NOT upload anything.
        // The GL texture already exists (owned by GStreamer).
    }

    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }
    @Override public Pixmap.Format getFormat() { return format; }
    @Override public boolean useMipMaps() { return false; }
    @Override public boolean isManaged() { return false; }
}

package com.nz.media.render.external;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public final class ExternalTexture extends Texture {

    private final ExternalTextureData data;

    public ExternalTexture(int texId, int width, int height) {
        super(GL20.GL_TEXTURE_2D, texId, new ExternalTextureData(width, height, Pixmap.Format.RGBA8888));
        this.data = (ExternalTextureData) getTextureData();
    }

    public void update(int texId, int width, int height) {
        // switch handle
        this.glHandle = texId;

        // keep TextureData metadata consistent (SpriteBatch uses width/height)
        data.update(width, height, Pixmap.Format.RGBA8888);
    }

    @Override
    public void dispose() {
        // IMPORTANT: do NOT delete GL texture (GStreamer owns it)
        // so dispose is intentionally a no-op.
    }
}

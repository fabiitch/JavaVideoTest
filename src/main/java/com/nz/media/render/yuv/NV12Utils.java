package com.nz.media.render.yuv;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;
import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;

@UtilityClass
public class NV12Utils {
    public static Texture getUvTex(int w, int h) {
        if (Gdx.gl30 != null) {
            return new Texture(new GLOnlyTextureData(
                w / 2, h / 2,
                0,
                GL30.GL_RG8,
                GL30.GL_RG,
                GL20.GL_UNSIGNED_BYTE
            ));
        }
        return new Texture(new GLOnlyTextureData(
            w / 2, h / 2,
            0,
            GL20.GL_LUMINANCE_ALPHA,
            GL20.GL_LUMINANCE_ALPHA,
            GL20.GL_UNSIGNED_BYTE
        ));
    }

    public static ByteBuffer ensureCapacity(ByteBuffer buf, int capacity) {
        if (buf == null || buf.capacity() < capacity) {
            return ByteBuffer.allocateDirect(capacity);
        }
        buf.clear();
        return buf;
    }

    /**
     * Copie ligne à ligne en “tightly packed” (rowBytes octets par ligne).
     */
    public static void packPlane(ByteBuffer src, int srcStrideBytes, int rowBytes, int height, ByteBuffer dst) {
        dst.clear();
        for (int y = 0; y < height; y++) {
            int srcPos = y * srcStrideBytes;
            src.limit(srcPos + rowBytes);
            src.position(srcPos);
            dst.put(src);
        }
        dst.flip();
    }


    public static Texture getYTex(int w, int h) {
        if (Gdx.gl30 != null) {
            // OpenGL 3+ : formats modernes (unpacked, rapides)
            return new Texture(new GLOnlyTextureData(
                w, h,
                0,                // mipMapLevel
                GL30.GL_R8,       // internalFormat
                GL30.GL_RED,      // format
                GL20.GL_UNSIGNED_BYTE // type
            ));
        }
        return new Texture(new GLOnlyTextureData(
            w, h,
            0,
            GL20.GL_LUMINANCE,
            GL20.GL_LUMINANCE,
            GL20.GL_UNSIGNED_BYTE
        ));
    }
}

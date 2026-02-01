package com.nz.media;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.github.fabiitch.gdxunit.utils.TestUtils;
import com.nz.lol.shared.debug.utils.TestDataUtils;
import com.nz.media.backend.gst.GstGLInteropBackend;
import com.nz.media.backend.gst.GstNv12Backend;
import com.nz.media.backend.gst.GstRgbaBackend;
import com.nz.media.player.VideoPlayerCore;
import com.nz.media.player.gdx.GdxVideoPlayer;
import com.nz.media.render.VideoRenderer;
import com.nz.media.render.external.GstExternalTextureGdxRenderer;
import com.nz.media.render.gdx.Nv12GdxRenderer;
import com.nz.media.render.gdx.RgbaGdxRenderer;
import com.nz.sr.RessourceUtils;
import com.nz.sr.gstreamer.GstTestUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.function.Supplier;

@Disabled
public class MediaPlayerTest {
    static File videoFile = TestDataUtils.getRandomVideo();

    @Test
    public void gstNv12() {
        GstTestUtils.initGStreamer();
        new Lwjgl3Application(new Game() {
            @Override
            public void create() {
                GstNv12Backend backend = new GstNv12Backend(4);
                VideoPlayerCore core = new VideoPlayerCore(backend);
                VideoRenderer renderer = new Nv12GdxRenderer();
                GdxVideoPlayer player = new GdxVideoPlayer(core, renderer);
                setScreen(new VideoTestScreen(videoFile, player));
            }
        });
    }

    @Test
    public void gstRGBA() {
        GstTestUtils.initGStreamer();
        new Lwjgl3Application(new Game() {
            @Override
            public void create() {
                GstRgbaBackend backend = new GstRgbaBackend(4);
                VideoPlayerCore core = new VideoPlayerCore(backend);
                VideoRenderer renderer = new RgbaGdxRenderer();
                GdxVideoPlayer player = new GdxVideoPlayer(core, renderer);
                setScreen(new VideoTestScreen(videoFile, player));
            }
        });
    }
    @Test
    public void gstOpenGL() {
        GstTestUtils.initGStreamer();
        new Lwjgl3Application(new Game() {
            @Override
            public void create() {
                GstGLInteropBackend backend = new GstGLInteropBackend();
                VideoPlayerCore core = new VideoPlayerCore(backend);
                VideoRenderer renderer = new GstExternalTextureGdxRenderer();
                GdxVideoPlayer player = new GdxVideoPlayer(core, renderer);
                setScreen(new VideoTestScreen(videoFile, player));
            }
        });
    }
}

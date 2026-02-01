package com.nz.recorder.backend.gst.source.video;

import com.nz.share.gst.recorder.GstRecorderLog;
import com.nz.recorder.backend.gst.source.GstVideoSource;
import lombok.Getter;
import org.freedesktop.gstreamer.*;

@Getter
public class GstScreenCapture implements GstVideoSource {
    public enum API {D3D11, DXGI, X11, WAYLAND}

    private final API api;
    private final int x, y, width, height;
    private final int fps;

    public GstScreenCapture(API api, int x, int y, int width, int height, int fps) {
        this.api = api;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    @Override
    public Bin build() {
        // Windows: d3d11screencapturesrc is generally best on modern GPUs.
        String elementName = null; //TODO gstream
        switch (api){
            case D3D11:
                elementName = "d3d11screencapturesrc";
                break;
            case DXGI:
                elementName ="dxgiscreencapsrc";
                break;
            case X11:
                elementName = "ximagesrc";
                break;
            case WAYLAND:
                elementName = "waylandsink";
                break;
        }

        Element src = ElementFactory.make(elementName, "vsrc");
        if (src == null) throw new IllegalStateException("Missing element: " + elementName);

        // Try common properties; not all are available on all elements.
        try {
            src.set("left", x);
        } catch (Exception ignored) {
        }
        try {
            src.set("top", y);
        } catch (Exception ignored) {
        }
        try {
            src.set("right", x + width);
        } catch (Exception ignored) {
        }
        try {
            src.set("bottom", y + height);
        } catch (Exception ignored) {
        }
        try {
            src.set("framerate", new Fraction(fps, 1));
        } catch (Exception ignored) {
        }

        Element queue = ElementFactory.make("queue", "vq_src");
        Element cap = ElementFactory.make("capsfilter", "vcap");

        String memoryCaps = api == API.D3D11
            ? String.format("video/x-raw(memory:D3D11Memory),format=(string){BGRA,RGBA,NV12},width=%d,height=%d,framerate=%d/1", width, height, fps)
            : String.format("video/x-raw,format=(string){BGRA,RGBA,NV12},width=%d,height=%d,framerate=%d/1", width, height, fps);
        Caps rawCaps = Caps.fromString(memoryCaps);
        cap.set("caps", rawCaps);

        Bin bin = new Bin("video_source");
        bin.addMany(src, queue, cap);
        Element.linkMany(src, queue, cap);
        GstRecorderLog.info("Video source caps: " + memoryCaps);

        GhostPad pad = new GhostPad("src", cap.getStaticPad("src"));
        bin.addPad(pad);
        return bin;
    }
}

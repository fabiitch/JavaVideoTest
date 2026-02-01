package com.nz.recorder.backend.gst.builder;

import com.nz.recorder.api.targets.ScreenTarget;
import com.nz.recorder.backend.gst.support.GstSupport;
import lombok.experimental.UtilityClass;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

@UtilityClass
public final class GstTargetsBuilder {

    public static Rectangle resolveScreenBounds(ScreenTarget target) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();

        String id = target.screenId();
        if (id != null) {
            try {
                int idx = Integer.parseInt(id.trim());
                if (idx >= 0 && idx < devices.length) {
                    return scaleBounds(devices[idx].getDefaultConfiguration().getBounds(),
                        devices[idx].getDefaultConfiguration().getDefaultTransform());
                }
            } catch (Exception ignored) {
            }

            for (GraphicsDevice d : devices) {
                if (d.getIDstring() != null && d.getIDstring().contains(id)) {
                    return scaleBounds(d.getDefaultConfiguration().getBounds(),
                        d.getDefaultConfiguration().getDefaultTransform());
                }
            }
        }

        return scaleBounds(ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds(),
            ge.getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform());
    }

    private static Rectangle scaleBounds(Rectangle bounds, AffineTransform transform) {
        double scaleX = transform.getScaleX();
        double scaleY = transform.getScaleY();
        if (scaleX != 1.0 || scaleY != 1.0) {
            GstSupport.logWarn("DPI scaling detected (scaleX=" + scaleX + ", scaleY=" + scaleY
                + "). Capture bounds are adjusted to device pixels. TODO: verify DPI awareness on Windows.");
        }
        int x = (int) Math.round(bounds.x * scaleX);
        int y = (int) Math.round(bounds.y * scaleY);
        int w = (int) Math.round(bounds.width * scaleX);
        int h = (int) Math.round(bounds.height * scaleY);
        return new Rectangle(x, y, w, h);

    }
}

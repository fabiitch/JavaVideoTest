package com.nz.sr.gstreamer;

import com.nz.sr.RessourceUtils;
import com.nz.share.gst.exception.GStreamInitException;
import com.nz.share.gst.init.GStreamInit;

public class GstTestUtils {

    public static void initGStreamer() {
        try {
            GStreamInit.init("Test",
                RessourceUtils.getFileFromResource("libs/gstreamer-1.26.6"));
        } catch (GStreamInitException e) {
            throw new RuntimeException(e);
        }
    }
}

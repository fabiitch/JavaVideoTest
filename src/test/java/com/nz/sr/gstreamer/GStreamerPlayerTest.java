package com.nz.sr.gstreamer;

import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class GStreamerPlayerTest {

    @Test
    public void GStreamerPlayerTest() {
        {
            GstTestUtils.initGStreamer();

            // ✅ pipeline équivalente à "videotestsrc ! d3d11videosink"
            Pipeline pipe = new Pipeline("test-pipeline");

            Element src = ElementFactory.make("videotestsrc", "source");
            Element sink = ElementFactory.make("d3d11videosink", "sink");

            if (src == null || sink == null) {
                System.err.println("❌ Missing required elements (videotestsrc or d3d11videosink).");
                System.exit(1);
            }

            // optionnel : changer le pattern (0 = défaut)
            src.set("pattern", 0);

            // ajoute et linke les éléments
            pipe.addMany(src, sink);
            if (!Element.linkMany(src, sink)) {
                System.err.println("❌ Could not link elements!");
                System.exit(2);
            }

            // ✅ play
            pipe.play();
            System.out.println("▶️  Playing test video... (Ctrl+C to quit)");

            // petite boucle pour garder le process vivant
            Gst.main();

            // cleanup (jamais atteint si Ctrl+C)
            pipe.stop();
            Gst.deinit();
        }
    }
}

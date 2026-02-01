//package com.nz.sr.gstreamer.recorder;
//
//import org.junit.jupiter.api.Test;
//
//import java.util.Set;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//class GstVideoPipelinePlannerTest {
//
//    @Test
//    void hardwarePathKeepsD3d11Memory() {
//        GstElementProbe probe = new FakeProbe(Set.of("d3d11convert", "videoconvert"));
//        GstVideoPipelinePlanner.Plan plan = GstVideoPipelinePlanner.planD3d11Capture(
//                GstVideoPipelinePlanner.EncoderTarget.H264_HARDWARE,
//                1920,
//                1080,
//                60,
//                probe
//        );
//
//        assertEquals(GstVideoPipelinePlanner.MemoryPath.D3D11, plan.memoryPath());
//        assertTrue(plan.useD3d11Convert());
//        assertFalse(plan.useD3d11Download());
//        assertTrue(plan.encoderCaps().contains("NV12"));
//    }
//
//    @Test
//    void softwarePathDownloadsToSystemMemory() {
//        GstElementProbe probe = new FakeProbe(Set.of("d3d11convert", "d3d11download", "videoconvert"));
//        GstVideoPipelinePlanner.Plan plan = GstVideoPipelinePlanner.planD3d11Capture(
//                GstVideoPipelinePlanner.EncoderTarget.H264_SOFTWARE,
//                1280,
//                720,
//                30,
//                probe
//        );
//
//        assertEquals(GstVideoPipelinePlanner.MemoryPath.SYSTEM, plan.memoryPath());
//        assertTrue(plan.useD3d11Download());
//        assertTrue(plan.useVideoConvert());
//        assertTrue(plan.encoderCaps().contains("I420"));
//    }
//
//    private static final class FakeProbe implements GstElementProbe {
//        private final Set<String> available;
//
//        private FakeProbe(Set<String> available) {
//            this.available = available;
//        }
//
//        @Override
//        public boolean isElementAvailable(String factory) {
//            return available.contains(factory);
//        }
//
//        @Override
//        public boolean hasProperty(org.freedesktop.gstreamer.Element element, String property) {
//            return true;
//        }
//    }
//}

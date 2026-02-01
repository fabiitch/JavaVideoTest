//package com.nz.recorder.backend.gst.core;
//
//import com.nz.recorder.api.core.Recorder;
//import com.nz.recorder.api.core.RecorderEngine;
//import com.nz.recorder.api.core.RecorderSettings;
//import com.nz.recorder.backend.gst.support.GstSupport;
//
//public final class GstRecorderEngine implements RecorderEngine {
//
//    @Override
//    public Recorder create(RecorderSettings settings) {
//        GstSupport.ensureGstInitOnce();
//        return new GstBackendRecorder(settings);
//    }
//}

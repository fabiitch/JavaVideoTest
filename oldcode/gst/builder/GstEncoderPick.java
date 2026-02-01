package com.nz.recorder.backend.gst.builder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.freedesktop.gstreamer.Element;

@AllArgsConstructor
@Getter
public class GstEncoderPick {
    private final Element encoder;
    private final boolean hardware;
}

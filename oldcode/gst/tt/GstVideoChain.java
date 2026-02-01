package com.nz.recorder.backend.gst.tt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.freedesktop.gstreamer.Element;

@AllArgsConstructor
@Getter
public class GstVideoChain {
    final Element bin;
    final String debug;
}

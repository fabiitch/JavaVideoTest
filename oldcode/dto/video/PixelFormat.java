package com.nz.sr.dto.video;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PixelFormat {
    // 8-bit
    YUV420P("yuv420p"),     // standard streaming
    YUV422P("yuv422p"),     // meilleur chroma
    YUV444P("yuv444p"),     // no subsampling
    NV12("nv12"),           // 4:2:0 semi-planar (très utilisé par NVENC/QSV/AMF)
    RGB24("rgb24"),         // RGB 8-bit
    RGBA("rgba"),           // RGB + alpha

    // 10-bit
    YUV420P10LE("yuv420p10le"), // HEVC Main10
    YUV422P10LE("yuv422p10le"),
    YUV444P10LE("yuv444p10le");

    private final String ff;

}

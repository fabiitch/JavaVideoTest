package com.nz.sr.dto.video;

public enum VideoCodec {

    //NVIDIA NVENC
    av1_nvenc, hevc_nvenc, h264_nvenc,
    // Intel QuickSync (QSV)
    h264_qsv, hevc_qsv,
    // AMD AMF
    h264_amf, hevc_amf,

    //Cpu
    libx264,
    libx265,
    libvpx_vp9
}

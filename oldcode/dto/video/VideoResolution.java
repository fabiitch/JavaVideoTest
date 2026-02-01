package com.nz.sr.dto.video;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoResolution {

    Native(-1, -1),
    HD_720(1280, 720),
    FULLHD_1080(1920, 1440),
    QHD_1440(2560, 1440),
    UHD_2160(3840, 2160);


    private final int width, height;
}

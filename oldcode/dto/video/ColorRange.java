package com.nz.sr.dto.video;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ColorRange {
    Limited("mpeg"),
    Full("jpeg");
    private final String ff;
}

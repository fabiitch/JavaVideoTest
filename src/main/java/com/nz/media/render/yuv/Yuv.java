package com.nz.media.render.yuv;


import com.nz.lol.shared.all.file.FileResolver;

import java.io.File;

public interface Yuv {

    String VERT = "shaders/yuv/yuv.vert";
    String FRAG = "shaders/yuv/yuv_nv12.frag";

    static File getVert(FileResolver fileResolver) {
        return fileResolver.getFile(VERT);
    }

    static File getFrag(FileResolver fileResolver) {
        return fileResolver.getFile(FRAG);
    }
}

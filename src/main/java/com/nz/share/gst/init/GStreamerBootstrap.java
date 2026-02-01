package com.nz.share.gst.init;


import java.nio.file.Path;

public final class GStreamerBootstrap {

    private static boolean done = false;

    public static synchronized void init(Path gstRoot) {
        if (done) return;

        Path bin = gstRoot.resolve("bin");
        Path lib = gstRoot.resolve("lib");

        // 1) Fix DLL search path AVANT TOUT
        WinDllDirs.enableDefaultSearch();
        WinDllDirs.add(bin);
        WinDllDirs.add(lib);

        // 2) Charger les DLL CORE à la main
        System.load(bin.resolve("gstreamer-1.0-0.dll").toString());
        System.load(bin.resolve("gstgl-1.0-0.dll").toString());

        done = true;
    }

    private GStreamerBootstrap() {}
}

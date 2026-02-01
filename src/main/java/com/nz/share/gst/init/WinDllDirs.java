package com.nz.share.gst.init;

import com.sun.jna.Pointer;

import java.nio.file.Path;

public final class WinDllDirs {

    // Win8+; on Win10/11 c'est OK
    private static final int LOAD_LIBRARY_SEARCH_DEFAULT_DIRS = 0x00001000;

    public static void enableDefaultSearch() {
        int ok = Kernel32Dirs.INSTANCE.SetDefaultDllDirectories(LOAD_LIBRARY_SEARCH_DEFAULT_DIRS);
        if (ok == 0) throw new RuntimeException("SetDefaultDllDirectories failed err=" + Kernel32Dirs.INSTANCE.GetLastError());
    }

    public static void add(Path dir) {
        Pointer h = Kernel32Dirs.INSTANCE.AddDllDirectory(dir.toAbsolutePath().toString());
        if (h == null) throw new RuntimeException("AddDllDirectory failed for " + dir + " err=" + Kernel32Dirs.INSTANCE.GetLastError());
    }

    private WinDllDirs() {}
}

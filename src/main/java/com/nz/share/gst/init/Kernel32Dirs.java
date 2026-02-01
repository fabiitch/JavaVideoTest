package com.nz.share.gst.init;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32Dirs extends Library {
    Kernel32Dirs INSTANCE = Native.load("kernel32", Kernel32Dirs.class, W32APIOptions.UNICODE_OPTIONS);

    int SetDefaultDllDirectories(int flags);
    Pointer AddDllDirectory(String newDirectory);
    int GetLastError();
}

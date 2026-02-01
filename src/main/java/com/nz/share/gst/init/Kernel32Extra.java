package com.nz.share.gst.init;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32Extra extends Library {
    Kernel32Extra INSTANCE = Native.load("kernel32", Kernel32Extra.class, W32APIOptions.UNICODE_OPTIONS);

    boolean SetDllDirectory(String lpPathName);
    int GetLastError();

    Pointer LoadLibrary(String lpLibFileName);
    boolean FreeLibrary(Pointer hModule);
}

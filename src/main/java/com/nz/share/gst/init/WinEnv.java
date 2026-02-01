package com.nz.share.gst.init;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.ptr.IntByReference;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WinEnv {

    public static void set(String name, String value) {
        Kernel32.INSTANCE.SetEnvironmentVariable(name, value);
    }

    public static String get(String name) {
        // Lire depuis l'environnement du process (pas System.getenv)
        char[] buf = new char[32_768];
        IntByReference len = new IntByReference();
        int n = Kernel32.INSTANCE.GetEnvironmentVariable(name, buf, buf.length);
        if (n == 0) return null; // non défini
        return new String(buf, 0, n);
    }
}

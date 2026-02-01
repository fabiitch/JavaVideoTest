package com.nz.media.backend.gst.nativeinterop;

import com.sun.jna.Pointer;
import org.freedesktop.gstreamer.GstObject;
import org.freedesktop.gstreamer.lowlevel.GstObjectPtr;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class GstAddr {
    private static final Field HANDLE_FIELD;
    private static final Method GET_POINTER;

    static {
        try {
            HANDLE_FIELD = GstObject.class.getDeclaredField("handle");
            HANDLE_FIELD.setAccessible(true);

            Class<?> handleClass = Class.forName("org.freedesktop.gstreamer.GstObject$Handle");
            GET_POINTER = handleClass.getDeclaredMethod("getPointer");
            GET_POINTER.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Cannot init GstAddr (gst1-java-core internals changed?)", e);
        }
    }

    public static long addr(GstObject obj) {
        try {
            Object handle = HANDLE_FIELD.get(obj);
            GstObjectPtr ptr = (GstObjectPtr) GET_POINTER.invoke(handle);
            return Pointer.nativeValue(ptr.getPointer());
        } catch (Throwable t) {
            throw new RuntimeException("Failed to read native address for " + obj.getClass().getName(), t);
        }
    }

    private GstAddr() {}
}

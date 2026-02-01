package com.nz.media.backend.gst.nativeinterop;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.foreign.ValueLayout.*;

/**
 * Panama downcalls for a few GStreamer / gst-gl symbols.
 * <p>
 * Works best if GStreamer DLLs are already loaded by your bootstrap (GStreamInit),
 * thanks to SymbolLookup.loaderLookup().
 * <p>
 * Fallback: if not loaded, you can set -Dgst.bin=... pointing to your GStreamer "bin" folder
 * and we'll libraryLookup() the needed dlls by absolute path.
 * <p>
 * Required JVM arg (Java 25):
 * --enable-native-access=ALL-UNNAMED
 */
public final class GstNativePanama {
    public static int GST_GL_API_OPENGL = 1;
    public static int GST_GL_PLATFORM_WGL = 4;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final Arena ARENA = Arena.ofShared();

    private static volatile SymbolLookup LOOKUP; // lazy, avoids ExceptionInInitializerError
    private static final AtomicBoolean INIT = new AtomicBoolean(false);

    // Handles (lazy)
    private static MethodHandle MH_gst_buffer_peek_memory;
    private static MethodHandle MH_gst_is_gl_memory;
    private static MethodHandle MH_gst_gl_memory_get_texture_id;
    private static MethodHandle MH_gst_gl_display_new;
    private static MethodHandle MH_gst_gl_context_new_wrapped;
    private static MethodHandle MH_gst_gl_context_get_type;

    private static MethodHandle MH_gst_context_new;
    private static MethodHandle MH_gst_context_writable_structure;
    private static MethodHandle MH_gst_structure_set; // variadic
    private static MethodHandle MH_gst_element_set_context;
    private static MethodHandle MH_gst_context_unref;

    private static MethodHandle MH_gst_structure_set_ctx; // spécialisé "context"


    private GstNativePanama() {
    }

    /**
     * Call once after your GStreamer bootstrap if you want. Safe to call multiple times.
     */
    public static void ensureLoaded() {
        initIfNeeded();
    }


    public static MemorySegment ptr(long address) {
        // pointeur NU (taille 0) : OK pour passer au natif
        return MemorySegment.ofAddress(address);
    }

    public static MemorySegment gstBufferStruct(long address) {
        // vue sized pour lire pts/dts/etc.
        long size = com.nz.media.gst.panama._GstBuffer.layout().byteSize();
        return MemorySegment.ofAddress(address)
            .reinterpret(size, Arena.global(), _ms -> {});
    }

    // GstMemory* gst_buffer_peek_memory(GstBuffer *buffer, guint idx);
    public static MemorySegment bufferPeekMemory(MemorySegment gstBuffer, int idx) {
        initIfNeeded();
        try {
            return (MemorySegment) MH_gst_buffer_peek_memory.invoke(gstBuffer, idx);
        } catch (Throwable t) {
            throw new RuntimeException("gst_buffer_peek_memory failed", t);
        }
    }

    // gboolean gst_is_gl_memory(GstMemory *mem);
    public static boolean isGlMemory(MemorySegment gstMemory) {
        initIfNeeded();
        try {
            return ((int) MH_gst_is_gl_memory.invoke(gstMemory)) != 0;
        } catch (Throwable t) {
            throw new RuntimeException("gst_is_gl_memory failed", t);
        }
    }

    // guint gst_gl_memory_get_texture_id(GstGLMemory *mem);
    public static int glMemoryGetTextureId(MemorySegment gstMemory) {
        initIfNeeded();
        try {
            return (int) MH_gst_gl_memory_get_texture_id.invoke(gstMemory);
        } catch (Throwable t) {
            throw new RuntimeException("gst_gl_memory_get_texture_id failed", t);
        }
    }
    public static MemorySegment gstGlDisplayNew() {
        initIfNeeded();
        try {
            return (MemorySegment) MH_gst_gl_display_new.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static MemorySegment gstGlContextNewWrapped(MemorySegment display, long handle, int platform, int api) {
        initIfNeeded();
        try {
            return (MemorySegment) MH_gst_gl_context_new_wrapped.invokeExact(display, handle, platform, api);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static long gstGlContextGType() {
        initIfNeeded();
        try {
            return (long) MH_gst_gl_context_get_type.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static MemorySegment gstContextNew(String type, boolean persistent, Arena arena) {
        initIfNeeded();
        MemorySegment cType = arena.allocateFrom(type);
        int pers = persistent ? 1 : 0;
        try {
            return (MemorySegment) MH_gst_context_new.invokeExact(cType, pers);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static MemorySegment gstContextWritableStructure(MemorySegment ctx) {
        initIfNeeded();
        try {
            return (MemorySegment) MH_gst_context_writable_structure.invokeExact(ctx);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void gstElementSetContext(MemorySegment element, MemorySegment ctx) {
        initIfNeeded();
        try {
            MH_gst_element_set_context.invokeExact(element, ctx);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static MemorySegment makeGlAppContext(MemorySegment glContext, Arena arena) {
        initIfNeeded();

        MemorySegment ctx = gstContextNew("gst.gl.app_context", true, arena);
        if (ctx == null || ctx.equals(MemorySegment.NULL)) {
            throw new IllegalStateException("gst_context_new returned NULL");
        }

        MemorySegment s = gstContextWritableStructure(ctx);
        if (s == null || s.equals(MemorySegment.NULL)) {
            throw new IllegalStateException("gst_context_writable_structure returned NULL");
        }

        long gtypeGlContext = gstGlContextGType();
        MemorySegment fieldName = arena.allocateFrom("context");

        try {
            // gst_structure_set(s, "context", GType, gpointer, NULL)
            MH_gst_structure_set_ctx.invokeExact(
                s,
                fieldName,
                gtypeGlContext,
                glContext,
                MemorySegment.NULL
            );
        } catch (Throwable t) {
            throw new RuntimeException("gst_structure_set(context) failed", t);
        }

        return ctx;
    }


    // -------------------------
    // Internal init
    // -------------------------

    private static void initIfNeeded() {
        if (INIT.get()) return;
        synchronized (GstNativePanama.class) {
            if (INIT.get()) return;

            // 1) Best path: resolve symbols from libraries already loaded (your GStreamInit / gst-one)
            SymbolLookup lookup = SymbolLookup.loaderLookup();

            // 2) If not found, fallback to explicit dll load by absolute path from -Dgst.bin
            // (useful when calling Panama before gst-one bootstrap)
            if (!hasSymbol(lookup, "gst_buffer_peek_memory") || !hasSymbol(lookup, "gst_is_gl_memory")) {
                lookup = buildLookupFromGstBin();
            }

            LOOKUP = lookup;

            MH_gst_buffer_peek_memory = downcall("gst_buffer_peek_memory",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));

            MH_gst_is_gl_memory = downcall("gst_is_gl_memory",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

            MH_gst_gl_memory_get_texture_id = downcall("gst_gl_memory_get_texture_id",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

            MH_gst_gl_display_new = downcall("gst_gl_display_new",
                FunctionDescriptor.of(ADDRESS));

            MH_gst_gl_context_new_wrapped = downcall("gst_gl_context_new_wrapped",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT));
// display, handle(uintptr_t), platform(enum int), api(enum int)

            MH_gst_gl_context_get_type = downcall("gst_gl_context_get_type",
                FunctionDescriptor.of(JAVA_LONG));
// retourne un GType (unsigned long)

            MH_gst_context_new = downcall("gst_context_new",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
// (const gchar* type, gboolean persistent)

            MH_gst_context_writable_structure = downcall("gst_context_writable_structure",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
// (GstContext*) -> GstStructure*

// gst_structure_set(structure, fieldname, ...)
// Variadic start index = 2 (structure, fieldname, <variadic...>)
            MH_gst_structure_set = downcallVariadic("gst_structure_set",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS),
                2);

            MH_gst_element_set_context = downcall("gst_element_set_context",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
// (GstElement*, GstContext*)

            MH_gst_context_unref = downcall("gst_context_unref",
                FunctionDescriptor.ofVoid(ADDRESS));

            MH_gst_structure_set_ctx = downcallVariadic(
                "gst_structure_set",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS),
                2
            );

            INIT.set(true);
        }
    }

    private static boolean hasSymbol(SymbolLookup lookup, String name) {
        try {
            return lookup.find(name).isPresent();
        } catch (Throwable t) {
            return false;
        }
    }

    public static long bufferGetPts(MemorySegment gstBuffer) {
        long pts = com.nz.media.gst.panama._GstBuffer.pts(gstBuffer);

        // optionnel mais propre
        if (pts == Long.MAX_VALUE) { // GST_CLOCK_TIME_NONE
            return -1;
        }
        return pts;
    }

    private static MethodHandle downcall(String symbol, FunctionDescriptor fd) {
        var opt = LOOKUP.find(symbol);
        if (opt.isEmpty()) {
            throw new IllegalStateException("Panama: symbol not found: " + symbol +
                " (did you load GStreamer DLLs before calling GstNativePanama? " +
                "If not, set -Dgst.bin=... to your GStreamer bin folder)");
        }
        return LINKER.downcallHandle(opt.get(), fd);
    }

    private static SymbolLookup buildLookupFromGstBin() {
        String gstBin = System.getProperty("gst.bin");
        if (gstBin == null || gstBin.isBlank()) {
            // no fallback available; keep loaderLookup (and error will be explicit)
            return SymbolLookup.loaderLookup();
        }

        File bin = new File(gstBin);
        if (!bin.isDirectory()) {
            throw new IllegalArgumentException("-Dgst.bin is not a directory: " + gstBin);
        }

        // Your bundle uses "no lib-" naming, so on Windows we expect:
        // gstreamer-1.0-0.dll and gstgl-1.0-0.dll
        File gstDll = new File(bin, "gstreamer-1.0-0.dll");
        File glDll = new File(bin, "gstgl-1.0-0.dll");

        // Some bundles name them slightly differently; if missing, try a few common fallbacks.
        if (!gstDll.exists()) gstDll = firstExisting(bin,
            "gstreamer-1.0.dll", "gstreamer-1.0-0.dll", "libgstreamer-1.0-0.dll");
        if (!glDll.exists()) glDll = firstExisting(bin,
            "gstgl-1.0.dll", "gstgl-1.0-0.dll", "libgstgl-1.0-0.dll");

        if (gstDll == null || glDll == null) {
            throw new IllegalStateException("Cannot locate required DLLs in " + bin.getAbsolutePath() +
                " (need gstreamer + gstgl). Set correct -Dgst.bin or rely on GStreamInit to load them first.");
        }

        // libraryLookup by absolute path
        SymbolLookup gst = SymbolLookup.libraryLookup(gstDll.getAbsolutePath(), ARENA);
        SymbolLookup gstgl = SymbolLookup.libraryLookup(glDll.getAbsolutePath(), ARENA);

        // Combine lookups
        return name -> {
            var a = gst.find(name);
            if (a.isPresent()) return a;
            return gstgl.find(name);
        };
    }

    private static MethodHandle downcallVariadic(String name, FunctionDescriptor fd, int firstVariadicIndex) {
        var sym = LOOKUP.find(name).orElseThrow(() -> new UnsatisfiedLinkError("Missing symbol: " + name));
        return LINKER.downcallHandle(sym, fd, Linker.Option.firstVariadicArg(firstVariadicIndex));
    }

    private static File firstExisting(File bin, String... names) {
        for (String n : names) {
            File f = new File(bin, n);
            if (f.exists()) return f;
        }
        return null;
    }
}

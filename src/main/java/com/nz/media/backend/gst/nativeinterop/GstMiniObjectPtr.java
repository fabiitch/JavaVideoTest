package com.nz.media.backend.gst.nativeinterop;

import org.freedesktop.gstreamer.MiniObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class GstMiniObjectPtr {

    private static volatile Accessor accessor; // lazy init (évite ExceptionInInitializerError)

    public static long address(MiniObject obj) {
        if (obj == null) return 0L;
        Accessor a = accessor;
        if (a == null) {
            accessor = a = buildAccessor();
            // Debug utile une fois
            // System.out.println("[GstMiniObjectPtr] Using: " + a);
        }
        return a.get(obj);
    }

    // -------------------------

    private interface Accessor {
        long get(MiniObject obj);
    }

    private static Accessor buildAccessor() {
        // 1) Try fields (class hierarchy)
        Field f = findPointerFieldInHierarchy(MiniObject.class);
        if (f != null) {
            f.setAccessible(true);
            return new FieldAccessor(f);
        }

        // 2) Try methods (class hierarchy)
        Method m = findPointerMethodInHierarchy(MiniObject.class);
        if (m != null) {
            m.setAccessible(true);
            return new MethodAccessor(m);
        }

        // 3) Last resort: dump for you (so we can target it precisely)
        dumpMiniObjectIntrospection();

        throw new RuntimeException(
            "Cannot find native pointer on MiniObject via fields or methods. " +
                "See debug dump above and we’ll target the right member."
        );
    }

    private static Field findPointerFieldInHierarchy(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                String n = f.getName().toLowerCase();
                Class<?> t = f.getType();
                if (t == long.class && (n.contains("handle") || n.contains("peer") || n.contains("ptr") || n.contains("address") || n.contains("native"))) {
                    return f;
                }
                if (t.getName().toLowerCase().contains("pointer")) {
                    return f;
                }
            }
        }
        return null;
    }

    private static Method findPointerMethodInHierarchy(Class<?> type) {
        // We search for methods returning long or something pointer-like
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                String n = m.getName().toLowerCase();
                if (m.getParameterCount() != 0) continue;

                Class<?> rt = m.getReturnType();
                boolean returnsLong = (rt == long.class) || (rt == int.class);
                boolean returnsPointerLike = rt.getName().toLowerCase().contains("pointer");

                if (!(returnsLong || returnsPointerLike)) continue;

                // Typical names in bindings
                if (n.contains("pointer") || n.contains("native") || n.contains("handle") || n.contains("address") || n.equals("getptr") || n.equals("ptr")) {
                    return m;
                }
            }
        }

        // If no good name matched, still try: any zero-arg long-returning method (risky but better than nothing)
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == long.class) return m;
            }
        }
        return null;
    }

    private static void dumpMiniObjectIntrospection() {
        System.err.println("---- MiniObject Introspection Dump ----");
        Class<?> c = MiniObject.class;
        while (c != null && c != Object.class) {
            System.err.println("Class: " + c.getName());
            System.err.println("  Fields: " + Arrays.toString(c.getDeclaredFields()));
            System.err.println("  Methods:");
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() == 0) {
                    System.err.println("    " + m.getReturnType().getName() + " " + m.getName() + "()");
                }
            }
            c = c.getSuperclass();
        }
        System.err.println("--------------------------------------");
    }

    // -------------------------

    private static final class FieldAccessor implements Accessor {
        private final Field field;
        FieldAccessor(Field field) { this.field = field; }

        @Override public long get(MiniObject obj) {
            try {
                Object v = field.get(obj);
                if (v instanceof Long l) return l;
                if (field.getType() == long.class) return (long) v;

                // pointer wrapper (ex: com.sun.jna.Pointer with "peer")
                if (v != null) {
                    try {
                        Field peer = v.getClass().getDeclaredField("peer");
                        peer.setAccessible(true);
                        Object p = peer.get(v);
                        if (p instanceof Long l2) return l2;
                    } catch (NoSuchFieldException ignored) {}
                }
                return 0L;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override public String toString() { return "FieldAccessor(" + field + ")"; }
    }

    private static final class MethodAccessor implements Accessor {
        private final Method method;
        MethodAccessor(Method method) { this.method = method; }

        @Override public long get(MiniObject obj) {
            try {
                Object v = method.invoke(obj);
                if (v == null) return 0L;
                if (v instanceof Long l) return l;
                if (v instanceof Integer i) return (long) i;

                // pointer wrapper (ex: com.sun.jna.Pointer)
                if (v.getClass().getName().toLowerCase().contains("pointer")) {
                    try {
                        Field peer = v.getClass().getDeclaredField("peer");
                        peer.setAccessible(true);
                        Object p = peer.get(v);
                        if (p instanceof Long l2) return l2;
                    } catch (NoSuchFieldException ignored) {}
                }
                return 0L;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override public String toString() { return "MethodAccessor(" + method + ")"; }
    }

    private GstMiniObjectPtr() {}
}

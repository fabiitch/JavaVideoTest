package com.nz.share.gst.init;

import com.nz.lol.shared.all.log.LogTag;
import com.nz.share.gst.exception.GStreamInitException;
import com.sun.jna.platform.win32.Kernel32;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Registry;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class GStreamInit {

    private boolean initialized = false;

    private static GStreamInit instance;

    public static synchronized void init(String progName, File rootGstPath) throws GStreamInitException {
        init(progName, rootGstPath, LogTag.Record.log());
    }

    public static synchronized void dispose() {
        if (instance != null) {
            instance.doDispose();
        }
    }

    public synchronized void doDispose() {
        if (initialized) {
            Gst.deinit();
            initialized = false;
        }
    }

    public static synchronized void init(String progName, File rootGstPath, Logger logger) throws GStreamInitException {
        try {
            if (instance == null) {
                instance = new GStreamInit();
            }
            instance.doInit(progName, rootGstPath, logger);
        } catch (Exception e) {
            logger.error("Failed to init GStreamer", e);
            throw new GStreamInitException("Failed to init GStreamer", e);
        }
    }

    private void doInit(String progName, File gstRoot, Logger logger) throws GStreamInitException {
        if (initialized) return;

        if (!gstRoot.exists())
            throw new GStreamInitException("GStreamer libraries not found at " + gstRoot.getAbsolutePath());

        logger.debug("GstRoot={}", gstRoot.getAbsolutePath());

        String binPath = new File(gstRoot, "bin").getAbsolutePath();
        String libPath = new File(gstRoot, "lib").getAbsolutePath();
        String plugins = new File(gstRoot, "lib\\gstreamer-1.0").getAbsolutePath();
        String scanner = new File(gstRoot, "libexec\\gstreamer-1.0\\gst-plugin-scanner.exe").getAbsolutePath();

        {
            Path root = gstRoot.toPath();
            Path bin  = root.resolve("bin");
            Path lib  = root.resolve("lib");

            WinDllDirs.enableDefaultSearch();
            WinDllDirs.add(bin);
            WinDllDirs.add(lib);
            var h = Kernel32Extra.INSTANCE.LoadLibrary(bin.resolve("gstreamer-1.0-0.dll").toString());
            System.out.println(h != null ? "LoadLibrary OK" : "FAIL err=" + Kernel32Extra.INSTANCE.GetLastError());


// Maintenant le loader Windows trouve deps dans bin + lib
            System.load(bin.resolve("gstreamer-1.0-0.dll").toString());
            System.load(bin.resolve("gstgl-1.0-0.dll").toString());
        }


        // 1) Prépare le cache
        File cacheFile = new File("cache");
        cacheFile.mkdirs();
        File registryFile = new File(cacheFile, "gst-registry.bin");
        registryFile.delete();
        logger.debug("CacheFile={}", cacheFile.getAbsolutePath());
        logger.debug("CacheFile={}", cacheFile.getAbsolutePath());

        System.setProperty("GST_REGISTRY", registryFile.getAbsolutePath());
        System.setProperty("GST_REGISTRY_1_0", registryFile.getAbsolutePath());
        System.setProperty("gstreamer.path", binPath);
        System.setProperty("GST_PLUGIN_PATH", plugins);
        System.setProperty("GST_PLUGIN_SYSTEM_PATH", plugins);
        System.setProperty("GST_PLUGIN_SCANNER", scanner);

        // 2) Renseigne les chemins GStreamer
        Kernel32 kernel32 = Kernel32.INSTANCE;
        WinEnv.set("PATH", binPath + ";" + libPath + ";" + System.getenv("PATH"));
        WinEnv.set("GST_PLUGIN_PATH_1_0", plugins);
        WinEnv.set("GST_PLUGIN_SYSTEM_PATH_1_0", plugins);
        WinEnv.set("GST_PLUGIN_SCANNER", scanner);
        WinEnv.set("GST_REGISTRY", registryFile.getAbsolutePath());
        WinEnv.set("GST_REGISTRY_1_0", registryFile.getAbsolutePath());

        logger.debug("GST_PLUGIN_PATH_1_0={}", WinEnv.get("GST_PLUGIN_PATH_1_0"));
        logger.debug("GST_PLUGIN_SYSTEM_PATH_1_0={}", WinEnv.get("GST_PLUGIN_SYSTEM_PATH_1_0"));
        logger.debug("GST_PLUGIN_SCANNER={}", WinEnv.get("GST_PLUGIN_SCANNER"));
        logger.debug("GST_REGISTRY={}", WinEnv.get("GST_REGISTRY"));
        logger.debug("GST_REGISTRY_1_0={}", WinEnv.get("GST_REGISTRY_1_0"));

        kernel32.SetEnvironmentVariable
            ("PATH", binPath + ";" + WinEnv.get("PATH"));

        // Debug chargeur du registre (verbeux mais parfait pour voir pourquoi gstd3d11.dll ne se charge pas)
//        WinEnv.set("GST_DEBUG", "GST_REGISTRY:6,DEFAULT:1");

        // 3) Rendez le dossier bin visible aux loaders Windows & JNA
        System.setProperty("jna.library.path", binPath);
        // 4) (Option de debug)
//        System.setProperty("jna.debug_load", "true");

        logger.debug("PATH= {}", WinEnv.get("PATH"));
        logger.debug("gstreamer.path=" + System.getProperty("gstreamer.path"));
        Gst.init(progName);

        logger.info("✅ GStreamer version={} init from {} : ", Gst.getVersion(), gstRoot);

        // explicitly rescan your portable plugin dir
        boolean scanned = Registry.get().scanPath(plugins);
        logger.debug("[GST] scanPath(" + plugins + ") = " + scanned);

        try {
            GStreamInstallCheck.checkAll(gstRoot, true);
        } catch (
            GStreamInitException e) {
            LogTag.Alert.log().error("GStream init error: {}", e.getMessage());
        }

        initialized = true;
    }
}

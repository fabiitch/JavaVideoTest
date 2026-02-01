package com.nz.sr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class RessourceUtils {
    public static File pickRandomFile(File folder, String... extensions) {
        if (folder == null || !folder.isDirectory())
            throw new IllegalArgumentException("Folder invalide: " + folder);

        File[] files = folder.listFiles((dir, name) -> {
            if (extensions == null || extensions.length == 0) return true;
            String lower = name.toLowerCase();
            for (String ext : extensions) {
                if (lower.endsWith(ext.toLowerCase())) return true;
            }
            return false;
        });


       if (files == null || files.length == 0)
            throw new IllegalStateException("Aucun fichier trouvé dans " + folder);

        return files[new Random().nextInt(files.length)];
    }

    public static File getFileFromResource(String path) {
        try {
            URL url = ClassLoader.getSystemResource(path);
            return new File(url.toURI());
        } catch (Exception e) {
            throw new RuntimeException("Cant find file " + path, e);
        }
    }
    public static String readRessourceAsString(String resourcePath) {
        try (InputStream stream = RessourceUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Missing resource: " + resourcePath);
            }
            return new String(org.apache.commons.io.IOUtils.toByteArray(stream), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

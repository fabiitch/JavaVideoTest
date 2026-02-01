package com.nz.share.gst.init;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

public class GStreamerFinder {

    public static File getGStreamerFolder(File parentDir, String version) {
        if (parentDir == null || !parentDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid parent dir: " + parentDir);
        }
        return new File(parentDir, version);
    }

    public static File getLatestGStreamerFolder(File parentDir) {
        if (parentDir == null || !parentDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid parent dir: " + parentDir);
        }
        Optional<File> latest = Arrays.stream(parentDir.listFiles(File::isDirectory))
            .filter(f -> f.getName().matches("\\d+\\.\\d+\\.\\d+")) // ex: 1.26.7
            .max((f1, f2) -> compareVersions(parseVersion(f1), parseVersion(f2)));

        return latest.orElseThrow(() ->
            new IllegalStateException("No GStreamer version folders found in " + parentDir.getAbsolutePath()));
    }

    private static int[] parseVersion(File f) {
        String[] parts = f.getName().split("\\.");
        int[] nums = new int[3];
        for (int i = 0; i < parts.length && i < 3; i++) {
            try {
                nums[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                nums[i] = 0;
            }
        }
        return nums;
    }

    private static int compareVersions(int[] v1, int[] v2) {
        for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
            if (v1[i] != v2[i]) {
                return Integer.compare(v1[i], v2[i]);
            }
        }
        return 0; // égales
    }
}

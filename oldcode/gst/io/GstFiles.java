package com.nz.recorder.backend.gst.io;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GstFiles {

    public static String extension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase() : ".mkv";
    }

    public static Path createTempDirectory(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp dir", e);
        }
    }

    public static Path exportDirectoryFor(Path requestedFile) {
        String name = requestedFile.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        Path parent = requestedFile.getParent() != null ? requestedFile.getParent() : Path.of(".");
        return parent.resolve(base + "_segments");
    }

    public static List<Path> listSegments(Path ringDir) throws IOException {
        if (ringDir == null) return List.of();
        try (Stream<Path> s = Files.list(ringDir)) {
            return s
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toList());
        }
    }

    private GstFiles() {}
}

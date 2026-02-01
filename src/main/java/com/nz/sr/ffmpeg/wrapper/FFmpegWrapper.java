package com.nz.sr.ffmpeg.wrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class FFmpegWrapper {

    private String path;

    private final ExecutorService executor = Executors.newCachedThreadPool();



    public FFmpegWrapper(String path) {
        this.path = path;
    }

    /**
     * Async
     */
    public CompletableFuture<FFmpegResult> runAsync(FFmpegTask task) {
        return CompletableFuture.supplyAsync(() -> run(task));
    }

    /**
     * Sync
     */
    public FFmpegResult run(FFmpegTask task) {
        List<String> cmd = task.build(path);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (task.isMergeErrorToOut()) {
            pb.redirectErrorStream(true);
        }

        try {
            Process p = pb.start();
            Instant start = Instant.now();

            // Lecture ligne à ligne (option: live consumers), sinon on bufferise.
            Future<String> outF = executor.submit(() -> slurp(p.getInputStream(), task.getLiveStderr()));
            Future<String> errF = task.isMergeErrorToOut()
                    ? CompletableFuture.completedFuture("") // déjà merged
                    : executor.submit(() -> slurp(p.getErrorStream(), task.getLiveStderr()));

            boolean finished = p.waitFor(task.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                p.destroyForcibly();
                return FFmpegResult.failure(new TimeoutException("ffmpeg timeout"), cmd);
            }

            int code = p.exitValue();
            String out = getQuiet(outF);
            String err = task.isMergeErrorToOut() ? "" : getQuiet(errF);
            return new FFmpegResult(code, out, err, Duration.between(start, Instant.now()), null, cmd);

        } catch (Throwable t) {
            return FFmpegResult.failure(t, cmd);
        }
    }

    private static String slurp(InputStream in, Consumer<String> live) throws IOException {
        if (live == null) {
            // buffer complet
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder(4096);
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } else {
            // streaming live + buffer retour
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder(4096);
                String line;
                while ((line = br.readLine()) != null) {
                    live.accept(line);
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        }
    }

    private static String getQuiet(Future<String> f) {
        try {
            return f.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }


}

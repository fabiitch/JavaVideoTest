package com.nz.sr.ffmpeg.wrapper;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.util.List;

@AllArgsConstructor
@Getter
public class FFmpegResult {
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final Duration duration;
    private final Throwable error; // null si OK
    private final List<String> command;

    public static FFmpegResult failure(Throwable t, List<String> cmd) {
        return new FFmpegResult(-1, "", t == null ? "" : t.toString(), Duration.ZERO, t, cmd);
    }

    public boolean ok() {
        return exitCode == 0 && error == null;
    }
}

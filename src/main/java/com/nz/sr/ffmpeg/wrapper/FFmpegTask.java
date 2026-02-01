package com.nz.sr.ffmpeg.wrapper;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Getter
@Setter
public class FFmpegTask {
    private final List<String> args = new ArrayList<>();
    private Duration timeout = Duration.ofMinutes(30);
    private boolean mergeErrorToOut = false;
    private Consumer<String> liveStdout; // optionnel: consommation ligne à ligne
    private Consumer<String> liveStderr; // optionnel

    public FFmpegTask add(String... a) {
        args.addAll(Arrays.asList(a));
        return this;
    }

    public FFmpegTask hideBanner(){
        return add("-hide_banner");
    }

    public FFmpegTask add(Collection<String> a) {
        args.addAll(a);
        return this;
    }

    List<String> build(String ffmpegPath) {
        List<String> cmd = new ArrayList<>(1 + args.size());
        cmd.add(ffmpegPath);
        cmd.addAll(args);
        return cmd;
    }
}

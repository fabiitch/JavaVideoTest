package com.nz.media.metrics;

import java.util.List;
import java.util.Optional;

public record VideoMetricsSnapshot(
        Optional<String> backendName,
        Optional<String> decoderName,
        Optional<String> rendererName,
        Optional<Integer> width,
        Optional<Integer> height,
        Optional<String> pixelFormat,
        Optional<Boolean> playing,
        Optional<Long> positionMs,
        Optional<Long> durationMs,
        Optional<Double> speed,
        Optional<Double> volume,
        Optional<Double> renderFps,
        Optional<Double> videoFpsNominal,
        Optional<Double> videoFpsOutput,
        Optional<Long> droppedDecoderFrames,
        Optional<Long> droppedRendererFrames,
        Optional<Long> lateFrames,
        Optional<Double> avSyncMs,
        Optional<Long> bufferedVideoMs,
        Optional<Long> bufferedAudioMs,
        Optional<Integer> decodedQueueSize,
        Optional<Double> decodeMsAvg,
        Optional<Double> decodeMsMax,
        Optional<Double> uploadMsAvg,
        Optional<Double> uploadMsMax,
        Optional<Double> renderMsAvg,
        Optional<Double> renderMsMax,
        List<String> notes
) {
    public VideoMetricsSnapshot {
        backendName = nullToEmpty(backendName);
        decoderName = nullToEmpty(decoderName);
        rendererName = nullToEmpty(rendererName);
        width = nullToEmpty(width);
        height = nullToEmpty(height);
        pixelFormat = nullToEmpty(pixelFormat);
        playing = nullToEmpty(playing);
        positionMs = nullToEmpty(positionMs);
        durationMs = nullToEmpty(durationMs);
        speed = nullToEmpty(speed);
        volume = nullToEmpty(volume);
        renderFps = nullToEmpty(renderFps);
        videoFpsNominal = nullToEmpty(videoFpsNominal);
        videoFpsOutput = nullToEmpty(videoFpsOutput);
        droppedDecoderFrames = nullToEmpty(droppedDecoderFrames);
        droppedRendererFrames = nullToEmpty(droppedRendererFrames);
        lateFrames = nullToEmpty(lateFrames);
        avSyncMs = nullToEmpty(avSyncMs);
        bufferedVideoMs = nullToEmpty(bufferedVideoMs);
        bufferedAudioMs = nullToEmpty(bufferedAudioMs);
        decodedQueueSize = nullToEmpty(decodedQueueSize);
        decodeMsAvg = nullToEmpty(decodeMsAvg);
        decodeMsMax = nullToEmpty(decodeMsMax);
        uploadMsAvg = nullToEmpty(uploadMsAvg);
        uploadMsMax = nullToEmpty(uploadMsMax);
        renderMsAvg = nullToEmpty(renderMsAvg);
        renderMsMax = nullToEmpty(renderMsMax);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    public static VideoMetricsSnapshot empty() {
        return new VideoMetricsSnapshot(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of()
        );
    }

    private static <T> Optional<T> nullToEmpty(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }
}

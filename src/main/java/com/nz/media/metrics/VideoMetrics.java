package com.nz.media.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

public final class VideoMetrics {
    private VideoMetrics() {
    }

    public static VideoMetricsSnapshot merge(VideoMetricsSnapshot left, VideoMetricsSnapshot right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");

        List<String> mergedNotes = new ArrayList<>();
        if (left.notes() != null) {
            mergedNotes.addAll(left.notes());
        }
        if (right.notes() != null) {
            mergedNotes.addAll(right.notes());
        }

        return new VideoMetricsSnapshot(
                or(left.backendName(), right.backendName()),
                or(left.decoderName(), right.decoderName()),
                or(left.rendererName(), right.rendererName()),
                or(left.width(), right.width()),
                or(left.height(), right.height()),
                or(left.pixelFormat(), right.pixelFormat()),
                or(left.playing(), right.playing()),
                or(left.positionMs(), right.positionMs()),
                or(left.durationMs(), right.durationMs()),
                or(left.speed(), right.speed()),
                or(left.volume(), right.volume()),
                or(left.renderFps(), right.renderFps()),
                or(left.videoFpsNominal(), right.videoFpsNominal()),
                or(left.videoFpsOutput(), right.videoFpsOutput()),
                or(left.droppedDecoderFrames(), right.droppedDecoderFrames()),
                or(left.droppedRendererFrames(), right.droppedRendererFrames()),
                or(left.lateFrames(), right.lateFrames()),
                or(left.avSyncMs(), right.avSyncMs()),
                or(left.bufferedVideoMs(), right.bufferedVideoMs()),
                or(left.bufferedAudioMs(), right.bufferedAudioMs()),
                or(left.decodedQueueSize(), right.decodedQueueSize()),
                or(left.decodeMsAvg(), right.decodeMsAvg()),
                or(left.decodeMsMax(), right.decodeMsMax()),
                or(left.uploadMsAvg(), right.uploadMsAvg()),
                or(left.uploadMsMax(), right.uploadMsMax()),
                or(left.renderMsAvg(), right.renderMsAvg()),
                or(left.renderMsMax(), right.renderMsMax()),
                List.copyOf(mergedNotes)
        );
    }

    static <T> Optional<T> or(Optional<T> a, Optional<T> b) {
        if (a != null && a.isPresent()) {
            return a;
        }
        return b == null ? Optional.empty() : b;
    }
}

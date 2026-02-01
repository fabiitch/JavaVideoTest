package com.nz.recorder.api.utils;

import com.nz.recorder.api.core.RecorderSettings;
import com.nz.recorder.api.output.QualityPreset;
import com.nz.recorder.api.save.SaveStrategy;
import com.nz.recorder.api.targets.CaptureTarget;

/**
 * Convenience settings to quickly build a RecorderSettings with sensible defaults + fast presets.
 * <p>
 * Defaults:
 * - runMode: CONTINUOUS
 * - fps: 60
 * - quality: LOW
 * - ringBuffer/frameAccess: empty
 */
public final class FastRecorderSettings {

    private static final int DEFAULT_FPS = 60;
    private static final QualityPreset DEFAULT_QUALITY = QualityPreset.LOW;

    private final CaptureTarget target;
    private final SaveStrategy saveStrategy;

    private final QualityPreset quality;
    private final int fps;

    private FastRecorderSettings(
        CaptureTarget target,
        SaveStrategy saveStrategy,
        QualityPreset quality,
        int fps) {
        if (target == null) throw new IllegalArgumentException("target");
        if (saveStrategy == null) throw new IllegalArgumentException("saveStrategy");
        if (quality == null) throw new IllegalArgumentException("quality");
        if (fps <= 0) throw new IllegalArgumentException("fps must be > 0");

        this.target = target;
        this.saveStrategy = saveStrategy;
        this.quality = quality;
        this.fps = fps;
    }

    // ------------------------------------------------------------------------
    // ENTRY POINTS
    // ------------------------------------------------------------------------

    /**
     * Minimal entry-point: give target + save strategy, get defaults (LOW/60/CONTINUOUS).
     */
    public static FastRecorderSettings of(CaptureTarget target, SaveStrategy saveStrategy) {
        return new FastRecorderSettings(
            target,
            saveStrategy,
            DEFAULT_QUALITY,
            DEFAULT_FPS
        );
    }

    /**
     * Shortcut: continuous recording defaults.
     */
    public static FastRecorderSettings continuous(CaptureTarget target, SaveStrategy saveStrategy) {
        return of(target, saveStrategy);
    }

    /**
     * Shortcut: ring-buffer recording with provided ring buffer settings.
     */
    public static FastRecorderSettings ringBuffer(CaptureTarget target,
                                                  SaveStrategy saveStrategy) {
        return new FastRecorderSettings(
            target,
            saveStrategy,
            DEFAULT_QUALITY,
            DEFAULT_FPS
        );
    }

    // ------------------------------------------------------------------------
    // FAST PRESETS (ONE LINERS)
    // ------------------------------------------------------------------------

    // ---- LOW ----
    public static FastRecorderSettings low60(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.LOW, 60);
    }

    public static FastRecorderSettings low30(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.LOW, 30);
    }

    public static FastRecorderSettings low120(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.LOW, 120);
    }

    // ---- MID ----
    public static FastRecorderSettings mid60(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.MID, 60);
    }

    public static FastRecorderSettings mid30(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.MID, 30);
    }

    public static FastRecorderSettings mid120(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.MID, 120);
    }

    // ---- HIGH ----
    public static FastRecorderSettings high60(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.HIGH, 60);
    }

    public static FastRecorderSettings high30(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.HIGH, 30);
    }

    public static FastRecorderSettings high120(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.HIGH, 120);
    }

    // ---- ULTRA ----
    public static FastRecorderSettings ultra60(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.ULTRA, 60);
    }

    public static FastRecorderSettings ultra30(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.ULTRA, 30);
    }

    public static FastRecorderSettings ultra120(CaptureTarget target, SaveStrategy saveStrategy) {
        return preset(target, saveStrategy, QualityPreset.ULTRA, 120);
    }

    /**
     * Generic preset helper.
     */
    public static FastRecorderSettings preset(CaptureTarget target,
                                              SaveStrategy saveStrategy,
                                              QualityPreset quality,
                                              int fps) {
        return FastRecorderSettings.of(target, saveStrategy)
            .quality(quality)
            .fps(fps);
    }

    // ------------------------------------------------------------------------
    // MUTATORS (IMMUTABLE STYLE)
    // ------------------------------------------------------------------------

    public FastRecorderSettings quality(QualityPreset quality) {
        return new FastRecorderSettings(target, saveStrategy, quality, fps);
    }

    public FastRecorderSettings fps(int fps) {
        return new FastRecorderSettings(target, saveStrategy, quality, fps);
    }


    // ------------------------------------------------------------------------
    // CONVERSION
    // ------------------------------------------------------------------------

    /**
     * Final conversion to the immutable API record.
     */
    public RecorderSettings toRecorderSettings() {
        return new RecorderSettings(
            target,
            quality,
            fps,
            saveStrategy
        );
    }

    // Optional getters (handy for debug / logging)
    public CaptureTarget target() {
        return target;
    }

    public SaveStrategy saveStrategy() {
        return saveStrategy;
    }

    public QualityPreset quality() {
        return quality;
    }

    public int fps() {
        return fps;
    }
}

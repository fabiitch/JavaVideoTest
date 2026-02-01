package com.nz.media.backend;

import java.util.EnumSet;

public class BackendStateUtils {

    private static final EnumSet<BackendState> OPENED_STATES =
        EnumSet.complementOf(EnumSet.of(BackendState.CLOSED, BackendState.NEW));
    private static final EnumSet<BackendState> PLAY_STATES =
        EnumSet.of(BackendState.READY, BackendState.PAUSED, BackendState.PLAYING, BackendState.ENDED);
    private static final EnumSet<BackendState> PAUSE_STATES =
        EnumSet.of(BackendState.PLAYING, BackendState.READY, BackendState.PAUSED);
    private static final EnumSet<BackendState> SEEKABLE_STATES = PLAY_STATES;
    private static final EnumSet<BackendState> SPEED_STATES = PLAY_STATES;
    private static final EnumSet<BackendState> VOLUME_STATES =
        EnumSet.complementOf(EnumSet.of(BackendState.ERROR));



    private BackendStateUtils() {
    }

    public static boolean isOpened(BackendState state) {
        return OPENED_STATES.contains(state);
    }

    public static boolean isPlayable(BackendState state) {
        return PLAY_STATES.contains(state);
    }

    public static boolean isPausable(BackendState state) {
        return PAUSE_STATES.contains(state);
    }

    public static boolean isSeekable(BackendState state) {
        return SEEKABLE_STATES.contains(state);
    }

    public static boolean isSpeedChangeAllowed(BackendState state) {
        return SPEED_STATES.contains(state);
    }

    public static boolean isVolumeChangeAllowed(BackendState state) {
        return VOLUME_STATES.contains(state);
    }
}

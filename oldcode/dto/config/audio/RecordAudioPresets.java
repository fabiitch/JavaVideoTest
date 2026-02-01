package com.nz.sr.dto.config.audio;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RecordAudioPresets {
    public static BaseAudioRecordConfig ultraHighAac() {
        return AacAudioRecordConfig.high();
    } // 4K vidéo → garde High audio

    public static BaseAudioRecordConfig highAac() {
        return AacAudioRecordConfig.high();
    }

    public static BaseAudioRecordConfig mediumOpus() {
        return OpusRecordConfig.medium();
    }

    public static BaseAudioRecordConfig lowOpus() {
        return OpusRecordConfig.low();
    }

    public static BaseAudioRecordConfig rawPcm() {
        return PcmRecordConfig.high();
    }
}

package com.nz.recorder.api.subscriptions;

import com.nz.recorder.api.core.RecorderState;
import com.nz.recorder.api.core.VideoStats;
import java.util.function.Consumer;

/** Event hooks (optional, but super useful). */
public interface RecorderSubscriptions {
    Subscription onStateChanged(Consumer<RecorderState> listener);
    Subscription onStats(Consumer<VideoStats> listener);
    Subscription onError(Consumer<Throwable> listener);
}

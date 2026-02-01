package com.nz.recorder.backend.gst.events;

import com.nz.recorder.api.core.RecorderState;
import com.nz.recorder.api.core.VideoStats;
import com.nz.recorder.api.subscriptions.RecorderSubscriptions;
import com.nz.recorder.api.subscriptions.Subscription;
import com.nz.recorder.backend.gst.support.GstSupport;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class GstEventHub implements RecorderSubscriptions {

    private final List<Consumer<RecorderState>> stateListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<VideoStats>> statsListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Throwable>> errorListeners = new CopyOnWriteArrayList<>();

    public void emitWarning(String msg) {
        GstSupport.logWarn(msg);
    }

    public void emitState(RecorderState s) {
        for (var l : stateListeners) {
            l.accept(s);
        }
    }

    public void emitStats(VideoStats st) {
        for (var l : statsListeners) {
            l.accept(st);
        }
    }

    public void emitError(Throwable t) {
        for (var l : errorListeners) {
            l.accept(t);
        }
    }

    @Override
    public Subscription onStateChanged(Consumer<RecorderState> listener) {
        stateListeners.add(listener);
        return () -> stateListeners.remove(listener);
    }

    @Override
    public Subscription onStats(Consumer<VideoStats> listener) {
        statsListeners.add(listener);
        return () -> statsListeners.remove(listener);
    }

    @Override
    public Subscription onError(Consumer<Throwable> listener) {
        errorListeners.add(listener);
        return () -> errorListeners.remove(listener);
    }
}

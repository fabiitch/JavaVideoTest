package com.nz.recorder.api.subscriptions;

public interface Subscription extends AutoCloseable {
    @Override
    void close();
}

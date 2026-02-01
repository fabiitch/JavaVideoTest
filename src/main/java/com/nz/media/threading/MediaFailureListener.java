package com.nz.media.threading;

@FunctionalInterface
public interface MediaFailureListener {
    void onFailure(Throwable error);
}

package com.nz.media.event;

public record PlaybackStateChanged(long atNanos, PlaybackState state) implements VideoEvent {}

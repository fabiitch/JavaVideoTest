package com.nz.media.event;

public record PositionChanged(long atNanos, long positionMs) implements VideoEvent {}

package com.nz.media.event;

public record BackendError(long atNanos, Throwable error) implements VideoEvent {}

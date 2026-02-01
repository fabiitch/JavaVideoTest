package com.nz.media.event;

public record BackendOpened(long atNanos, String source) implements VideoEvent {}

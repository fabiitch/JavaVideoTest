package com.nz.media.event;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class VideoEventBus implements AutoCloseable {

    public interface Subscription extends AutoCloseable {}

    private final BlockingQueue<VideoEvent> queue = new LinkedBlockingQueue<>();
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final Thread dispatcher;

    public VideoEventBus() {
        dispatcher = Thread.ofVirtual()
            .name("video-event-bus")
            .start(this::loop);
    }

    public void publish(VideoEvent event) {
        if (running.get()) queue.offer(event);
    }

    public <T extends VideoEvent> Subscription subscribe(
        Class<T> type,
        Consumer<T> consumer
    ) {
        listeners.computeIfAbsent(type, _ -> new CopyOnWriteArrayList<>())
            .add(consumer);
        return () -> listeners.getOrDefault(type, new CopyOnWriteArrayList<>())
            .remove(consumer);
    }

    private void loop() {
        while (running.get()) {
            try {
                dispatch(queue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatch(VideoEvent event) {
        var exact = listeners.get(event.getClass());
        if (exact != null)
            exact.forEach(l -> ((Consumer<VideoEvent>) l).accept(event));

        var wildcard = listeners.get(VideoEvent.class);
        if (wildcard != null)
            wildcard.forEach(l -> ((Consumer<VideoEvent>) l).accept(event));
    }

    @Override
    public void close() {
        running.set(false);
        dispatcher.interrupt();
    }
}

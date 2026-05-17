package me.englishhugging.core;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class WordScheduler implements AutoCloseable {
    public interface Listener {
        void onWord(WordEntry wordEntry);
    }

    private final List<WordEntry> words;
    private final Listener listener;
    private final AtomicInteger index = new AtomicInteger(0);
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private int intervalSeconds;

    public WordScheduler(List<WordEntry> words, int intervalSeconds, Listener listener) {
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("words must not be empty");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.words = words;
        this.listener = listener;
        this.intervalSeconds = Math.max(2, intervalSeconds);
    }

    public synchronized void start() {
        stop();
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "word-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        emitNext();
        future = executor.scheduleAtFixedRate(this::emitNext, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public synchronized void updateIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = Math.max(2, intervalSeconds);
        if (executor != null) {
            start();
        }
    }

    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void emitNext() {
        int position = Math.floorMod(index.getAndIncrement(), words.size());
        listener.onWord(words.get(position));
    }

    @Override
    public void close() {
        stop();
    }
}

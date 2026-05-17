package me.englishhugging.core;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class WordScheduler implements AutoCloseable {
    public interface Listener {
        void onWord(WordEntry wordEntry);
    }

    public interface ProgressListener {
        void onProgress(int nextWordIndex, String shuffleOrder, int shufflePosition, int randomPlayedCount);
    }

    private final List<WordEntry> words;
    private final Listener listener;
    private final ProgressListener progressListener;
    private final Random random = new Random();
    private PlaybackMode playbackMode;
    private int nextWordIndex;
    private List<Integer> shuffleOrder;
    private int shufflePosition;
    private int randomPlayedCount;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private int intervalSeconds;

    public WordScheduler(List<WordEntry> words, int intervalSeconds, Listener listener) {
        this(
                words,
                intervalSeconds,
                PlaybackMode.SEQUENTIAL,
                0,
                "",
                0,
                0,
                listener,
                null
        );
    }

    public WordScheduler(
            List<WordEntry> words,
            int intervalSeconds,
            PlaybackMode playbackMode,
            int nextWordIndex,
            String shuffleOrder,
            int shufflePosition,
            int randomPlayedCount,
            Listener listener,
            ProgressListener progressListener
    ) {
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("words must not be empty");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.words = words;
        this.listener = listener;
        this.progressListener = progressListener;
        this.playbackMode = playbackMode == null ? PlaybackMode.SEQUENTIAL : playbackMode;
        this.nextWordIndex = Math.floorMod(nextWordIndex, words.size());
        this.shuffleOrder = parseShuffleOrder(shuffleOrder, words.size());
        this.shufflePosition = Math.min(Math.max(0, shufflePosition), words.size());
        this.randomPlayedCount = Math.max(0, randomPlayedCount);
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
        int position = nextPosition();
        listener.onWord(words.get(position));
        publishProgress();
    }

    private synchronized int nextPosition() {
        if (playbackMode == PlaybackMode.RANDOM) {
            randomPlayedCount++;
            return random.nextInt(words.size());
        }

        if (playbackMode == PlaybackMode.SHUFFLE_NO_REPEAT) {
            if (shuffleOrder.size() != words.size() || shufflePosition >= shuffleOrder.size()) {
                shuffleOrder = newShuffleOrder(words.size());
                shufflePosition = 0;
            }
            return shuffleOrder.get(shufflePosition++);
        }

        int position = Math.floorMod(nextWordIndex, words.size());
        nextWordIndex = Math.floorMod(position + 1, words.size());
        return position;
    }

    private void publishProgress() {
        if (progressListener != null) {
            progressListener.onProgress(nextWordIndex, serializeShuffleOrder(shuffleOrder), shufflePosition, randomPlayedCount);
        }
    }

    private List<Integer> parseShuffleOrder(String value, int wordCount) {
        if (value == null || value.trim().length() == 0) {
            return newShuffleOrder(wordCount);
        }

        String[] parts = value.split(",");
        if (parts.length != wordCount) {
            return newShuffleOrder(wordCount);
        }

        List<Integer> parsed = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        try {
            for (String part : parts) {
                int index = Integer.parseInt(part.trim());
                if (index < 0 || index >= wordCount || !seen.add(index)) {
                    return newShuffleOrder(wordCount);
                }
                parsed.add(index);
            }
        } catch (RuntimeException ignored) {
            return newShuffleOrder(wordCount);
        }
        return parsed;
    }

    private List<Integer> newShuffleOrder(int wordCount) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < wordCount; i++) {
            order.add(i);
        }
        Collections.shuffle(order, random);
        return order;
    }

    private String serializeShuffleOrder(List<Integer> order) {
        StringBuilder builder = new StringBuilder();
        for (Integer index : order) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(index);
        }
        return builder.toString();
    }

    @Override
    public void close() {
        stop();
    }
}

package me.englishhugging.core;

import me.englishhugging.core.display.FillBlankGenerator;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.PlaybackMode;

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
        void onFillBlankWord(String displayWord, WordEntry originalEntry, boolean hidePhrases, boolean hideTranslation);
        void onPlaybackFinished();
    }

    public interface ProgressListener {
        void onProgress(int nextWordIndex, String shuffleOrder, int shufflePosition, int randomPlayedCount);
    }

    private final List<WordEntry> words;
    private final Listener listener;
    private final ProgressListener progressListener;
    private final Random random = new Random();
    private final FillBlankGenerator fillBlankGenerator = new FillBlankGenerator();
    private PlaybackMode playbackMode;
    private int nextWordIndex;
    private List<Integer> shuffleOrder;
    private int shufflePosition;
    private int randomPlayedCount;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private int intervalSeconds;
    private boolean paused;
    private boolean loopPlayback;
    private int sessionPlayedCount = 0;

    // Fill-blank state
    private boolean fillBlankMode;
    private int fillBlankIntervalSeconds;
    private boolean fillBlankHidePhrases;
    private boolean fillBlankShowTranslation;
    private boolean inFillBlankPhase = false;
    private boolean initialBlankShown = false;
    private WordEntry fillBlankOriginalEntry;
    private String fillBlankCurrentWord;
    private List<Integer> fillBlankRemainingBlanks;

    public WordScheduler(
            List<WordEntry> words,
            int intervalSeconds,
            PlaybackMode playbackMode,
            int nextWordIndex,
            String shuffleOrder,
            int shufflePosition,
            int randomPlayedCount,
            String startingPrefix,
            boolean loopPlayback,
            Listener listener,
            ProgressListener progressListener
    ) {
        this(words, intervalSeconds, playbackMode, nextWordIndex, shuffleOrder,
                shufflePosition, randomPlayedCount, startingPrefix, loopPlayback,
                false, 3, true, true,
                listener, progressListener);
    }

    public WordScheduler(
            List<WordEntry> words,
            int intervalSeconds,
            PlaybackMode playbackMode,
            int nextWordIndex,
            String shuffleOrder,
            int shufflePosition,
            int randomPlayedCount,
            String startingPrefix,
            boolean loopPlayback,
            boolean fillBlankMode,
            int fillBlankIntervalSeconds,
            boolean fillBlankHidePhrases,
            boolean fillBlankShowTranslation,
            Listener listener,
            ProgressListener progressListener
    ) {
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("words must not be empty");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (startingPrefix != null && !startingPrefix.isEmpty()) {
            List<WordEntry> filtered = new ArrayList<>();
            for (WordEntry w : words) {
                if (w.getWord().toLowerCase().startsWith(startingPrefix.toLowerCase())) {
                    filtered.add(w);
                }
            }
            this.words = filtered.isEmpty() ? words : filtered;
        } else {
            this.words = words;
        }

        this.listener = listener;
        this.progressListener = progressListener;
        this.playbackMode = playbackMode == null ? PlaybackMode.SEQUENTIAL : playbackMode;

        if (nextWordIndex < 0 || nextWordIndex > this.words.size()) {
            this.nextWordIndex = 0;
        } else {
            this.nextWordIndex = nextWordIndex;
            if (loopPlayback && this.nextWordIndex == this.words.size()) {
                this.nextWordIndex = 0;
            }
        }

        this.shuffleOrder = parseShuffleOrder(shuffleOrder, this.words.size());
        this.shufflePosition = Math.min(Math.max(0, shufflePosition), this.words.size());
        this.randomPlayedCount = Math.max(0, randomPlayedCount);
        this.intervalSeconds = Math.max(2, intervalSeconds);

        boolean hasPrefix = startingPrefix != null && !startingPrefix.trim().isEmpty();
        this.loopPlayback = hasPrefix ? loopPlayback : true;

        this.fillBlankMode = fillBlankMode;
        this.fillBlankIntervalSeconds = Math.max(1, fillBlankIntervalSeconds);
        this.fillBlankHidePhrases = fillBlankHidePhrases;
        this.fillBlankShowTranslation = fillBlankShowTranslation;
    }

    public synchronized void start() {
        stop();
        paused = false;
        inFillBlankPhase = false;
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "word-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        sessionPlayedCount = 0;
        emitNext(); // This handles both the emission and scheduling the next tick
    }

    public synchronized void pause() {
        if (future != null) { future.cancel(false); future = null; }
        paused = true;
    }

    public synchronized void resume() {
        if (!paused || executor == null) return;
        paused = false;
        emitNext();
    }

    public synchronized boolean isPaused() { return paused; }

    public synchronized void updateIntervalSeconds(int intervalSeconds) {
        int newInterval = Math.max(2, intervalSeconds);
        if (this.intervalSeconds == newInterval) return;
        this.intervalSeconds = newInterval;
        // The new interval will take effect on the next normal word tick naturally
    }

    public synchronized void updateFillBlankSettings(boolean enabled, int interval, boolean hidePhrases, boolean showTranslation) {
        this.fillBlankMode = enabled;
        this.fillBlankIntervalSeconds = Math.max(1, interval);
        this.fillBlankHidePhrases = hidePhrases;
        this.fillBlankShowTranslation = showTranslation;

        if (!enabled && inFillBlankPhase) {
            inFillBlankPhase = false;
            fillBlankOriginalEntry = null;
            fillBlankCurrentWord = null;
            fillBlankRemainingBlanks = null;
            
            // Advance to next normal word immediately
            if (future != null) future.cancel(false);
            scheduleNext(0);
        }
    }

    public synchronized void stop() {
        paused = false;
        inFillBlankPhase = false;
        if (future != null) {
            future.cancel(true);
            future = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private synchronized void scheduleNext(long delaySeconds) {
        if (paused || executor == null) return;
        if (future != null) future.cancel(false);
        future = executor.schedule(this::emitNext, delaySeconds, TimeUnit.SECONDS);
    }

    private void emitNext() {
        synchronized (this) {
            if (paused || executor == null) return;

            if (inFillBlankPhase) {
                if (!initialBlankShown) {
                    initialBlankShown = true;
                    boolean hideTranslation = !fillBlankShowTranslation;
                    listener.onFillBlankWord(fillBlankCurrentWord, fillBlankOriginalEntry, fillBlankHidePhrases, hideTranslation);
                    scheduleNext(fillBlankIntervalSeconds);
                    return;
                }

                if (fillBlankRemainingBlanks != null && !fillBlankRemainingBlanks.isEmpty()) {
                    fillBlankCurrentWord = fillBlankGenerator.fillOneBlank(
                            fillBlankCurrentWord, fillBlankOriginalEntry.getWord(), fillBlankRemainingBlanks);

                    boolean hideTranslation = !fillBlankShowTranslation;
                    listener.onFillBlankWord(fillBlankCurrentWord, fillBlankOriginalEntry, fillBlankHidePhrases, hideTranslation);

                    // If it was the last blank, wait another fill interval before going to the next word
                    scheduleNext(fillBlankIntervalSeconds);
                    return;
                } else {
                    // All blanks were filled, and we already waited. Time for next normal word!
                    inFillBlankPhase = false;
                    fillBlankOriginalEntry = null;
                    fillBlankCurrentWord = null;
                    fillBlankRemainingBlanks = null;
                }
            }
        }

        // --- Normal word emission ---
        if (!loopPlayback && playbackMode == PlaybackMode.RANDOM && sessionPlayedCount >= words.size()) {
            stop();
            listener.onPlaybackFinished();
            return;
        }
        int position = nextPosition();
        if (position == -1) {
            stop();
            listener.onPlaybackFinished();
            return;
        }
        sessionPlayedCount++;
        WordEntry word = words.get(position);
        listener.onWord(word);
        publishProgress();

        synchronized (this) {
            if (fillBlankMode && word.getWord() != null && word.getWord().length() > 1) {
                fillBlankOriginalEntry = word;
                FillBlankGenerator.BlankResult result = fillBlankGenerator.generateBlanked(word.getWord());
                fillBlankCurrentWord = result.getBlankedWord();
                fillBlankRemainingBlanks = new ArrayList<>(result.getBlankPositions());
                inFillBlankPhase = true;
                initialBlankShown = false;
                scheduleNext(intervalSeconds); // Wait normal interval before showing the initial blanked word
            } else {
                scheduleNext(intervalSeconds);
            }
        }
    }

    private synchronized int nextPosition() {
        if (playbackMode == PlaybackMode.RANDOM) {
            randomPlayedCount++;
            return random.nextInt(words.size());
        }

        if (playbackMode == PlaybackMode.SHUFFLE_NO_REPEAT) {
            if (shuffleOrder.size() != words.size()) {
                shuffleOrder = newShuffleOrder(words.size());
                shufflePosition = 0;
            }
            if (shufflePosition >= shuffleOrder.size()) {
                if (!loopPlayback) return -1;
                shuffleOrder = newShuffleOrder(words.size());
                shufflePosition = 0;
            }
            return shuffleOrder.get(shufflePosition++);
        }

        if (!loopPlayback && nextWordIndex >= words.size()) {
            return -1;
        }
        int position = Math.floorMod(nextWordIndex, words.size());
        nextWordIndex = position + 1;
        if (loopPlayback) {
            nextWordIndex = Math.floorMod(nextWordIndex, words.size());
        }
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

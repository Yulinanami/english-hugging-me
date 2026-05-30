package me.englishhugging.core;

import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.PlaybackMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WordSchedulerTest {

    @Test
    void testSequentialPlayback() throws InterruptedException {
        List<WordEntry> words = Arrays.asList(
                new WordEntry("apple", null, null),
                new WordEntry("banana", null, null),
                new WordEntry("cherry", null, null)
        );

        WordSchedulerConfig config = new WordSchedulerConfig(
                2, PlaybackMode.SEQUENTIAL, 0, "", 0, 0, "", false, false, 2, false, false
        );

        AtomicInteger count = new AtomicInteger(0);

        WordScheduler scheduler = new WordScheduler(words, config, new WordScheduler.Listener() {
            @Override
            public void onWord(WordEntry wordEntry) {
                count.incrementAndGet();
            }

            @Override
            public void onFillBlankWord(String displayWord, WordEntry originalEntry, boolean hidePhrases, boolean hideTranslation) {
            }

            @Override
            public void onPlaybackFinished() {
            }
        }, (nextWordIndex, shuffleOrder, shufflePosition, randomPlayedCount) -> {});

        scheduler.start();
        Thread.sleep(100);
        scheduler.stop();

        assertTrue(count.get() >= 1, "Should emit at least one word immediately");
    }
}

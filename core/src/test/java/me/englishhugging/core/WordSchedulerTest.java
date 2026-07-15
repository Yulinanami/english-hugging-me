package me.englishhugging.core;

import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.core.settings.PlaybackProgress;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WordScheduler 的黑盒回归测试。
 *
 * <p>调度器的最小主间隔为 2 秒，因此依赖定时推进的用例通过“把起始进度设到只差一步”
 * 来压缩等待时间；依赖“立即发射”语义（start / resume / 关闭填空）的用例都在毫秒级完成。
 */
class WordSchedulerTest {

    /**
     * 回调收集器：把各类事件转成可等待、可断言的数据。
     */
    private static final class RecordingListener implements WordScheduler.Listener {
        final Queue<String> words = new ConcurrentLinkedQueue<>();
        final Queue<String> blankFrames = new ConcurrentLinkedQueue<>();
        final CountDownLatch wordLatch;
        final CountDownLatch blankLatch;
        final CountDownLatch finishedLatch = new CountDownLatch(1);
        volatile boolean lastHidePhrases;
        volatile boolean lastHideTranslation;
        volatile WordEntry lastBlankOriginal;

        RecordingListener(int expectedWords, int expectedBlanks) {
            this.wordLatch = new CountDownLatch(expectedWords);
            this.blankLatch = new CountDownLatch(expectedBlanks);
        }

        @Override
        public void onWord(WordEntry wordEntry) {
            this.words.add(wordEntry.word());
            this.wordLatch.countDown();
        }

        @Override
        public void onFillBlankWord(String displayWord, WordEntry originalEntry, boolean hidePhrases, boolean hideTranslation) {
            this.blankFrames.add(displayWord);
            this.lastBlankOriginal = originalEntry;
            this.lastHidePhrases = hidePhrases;
            this.lastHideTranslation = hideTranslation;
            this.blankLatch.countDown();
        }

        @Override
        public void onPlaybackFinished() {
            this.finishedLatch.countDown();
        }
    }

    private static List<WordEntry> words(String... spellings) {
        List<WordEntry> list = new ArrayList<>();
        for (String spelling : spellings) {
            list.add(new WordEntry(spelling, null, null));
        }
        return list;
    }

    /**
     * 统一的配置工厂：主间隔取允许的最小值 2 秒；填空开启时隐藏短语、不显示释义。
     */
    private static WordSchedulerConfig config(
            PlaybackMode mode, PlaybackProgress progress, String prefix, boolean loop, boolean fillBlank) {
        return new WordSchedulerConfig(2, mode, progress, prefix, loop, fillBlank, 1, true, false);
    }

    @Test
    void sequentialEmitsFirstWordImmediately() throws InterruptedException {
        RecordingListener listener = new RecordingListener(1, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("apple", "banana", "cherry"),
                config(PlaybackMode.SEQUENTIAL, PlaybackProgress.EMPTY, "", true, false),
                listener, progress -> { })) {
            scheduler.start();

            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS), "第一个词应当立即发射");
            assertEquals("apple", listener.words.peek());
        }
    }

    @Test
    void sequentialProgressCallbackAdvancesIndex() throws InterruptedException {
        CountDownLatch progressLatch = new CountDownLatch(1);
        AtomicReference<PlaybackProgress> captured = new AtomicReference<>();
        RecordingListener listener = new RecordingListener(1, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("apple", "banana", "cherry"),
                config(PlaybackMode.SEQUENTIAL, PlaybackProgress.EMPTY, "", true, false),
                listener,
                progress -> {
                    captured.set(progress);
                    progressLatch.countDown();
                })) {
            scheduler.start();

            assertTrue(progressLatch.await(2, TimeUnit.SECONDS), "发词后应当回调进度");
            assertEquals(1, captured.get().nextWordIndex());
        }
    }

    @Test
    void resumeAfterPauseEmitsNextWordImmediately() throws InterruptedException {
        RecordingListener listener = new RecordingListener(1, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("apple", "banana", "cherry"),
                config(PlaybackMode.SEQUENTIAL, PlaybackProgress.EMPTY, "", true, false),
                listener, progress -> { })) {
            scheduler.start();
            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS));

            scheduler.pause();
            assertTrue(scheduler.isPaused());

            // resume 应当立即补发下一个词（主间隔 2 秒，1 秒内到达即证明没有等满间隔）
            int seenBeforeResume = listener.words.size();
            scheduler.resume();
            long deadline = System.currentTimeMillis() + 1000;
            while (listener.words.size() <= seenBeforeResume && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }

            assertTrue(listener.words.size() > seenBeforeResume, "resume 应当立即补发下一个词");
            assertFalse(scheduler.isPaused());
        }
    }

    @Test
    void sequentialWithPrefixFinishesWithoutLoop() throws InterruptedException {
        // 只有设置了前缀时 loopPlayback=false 才生效（无前缀会被强制循环），此语义由本用例钉住
        RecordingListener listener = new RecordingListener(1, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("alpha", "apricot"),
                config(PlaybackMode.SEQUENTIAL, new PlaybackProgress(1, "", 0, 0), "a", false, false),
                listener, progress -> { })) {
            scheduler.start();

            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS));
            assertEquals("apricot", listener.words.peek(), "应当从保存的进度处继续播放");
            assertTrue(listener.finishedLatch.await(5, TimeUnit.SECONDS), "不循环时播完最后一个词应当宣告结束");
        }
    }

    @Test
    void randomWithPrefixFinishesAfterWholeSetWithoutLoop() throws InterruptedException {
        RecordingListener listener = new RecordingListener(1, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("apple"),
                config(PlaybackMode.RANDOM, PlaybackProgress.EMPTY, "a", false, false),
                listener, progress -> { })) {
            scheduler.start();

            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS));
            assertTrue(listener.finishedLatch.await(5, TimeUnit.SECONDS), "随机模式播满全集后应当宣告结束");
        }
    }

    @Test
    void shuffleNoRepeatPlaysEachWordOnceThenFinishes() throws InterruptedException {
        RecordingListener listener = new RecordingListener(2, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("ant", "axe"),
                config(PlaybackMode.SHUFFLE_NO_REPEAT, PlaybackProgress.EMPTY, "a", false, false),
                listener, progress -> { })) {
            scheduler.start();

            assertTrue(listener.wordLatch.await(6, TimeUnit.SECONDS), "两个词都应当被播放");
            assertTrue(listener.finishedLatch.await(5, TimeUnit.SECONDS), "乱序不重复播完一轮后应当宣告结束");
            assertEquals(2, listener.words.size());
            assertNotEquals(listener.words.poll(), listener.words.poll(), "乱序不重复模式不应重复播放同一个词");
        }
    }

    @Test
    void fillBlankShowsBlankedFrameAfterMainInterval() throws InterruptedException {
        RecordingListener listener = new RecordingListener(1, 1);
        try (WordScheduler scheduler = new WordScheduler(
                words("banana"),
                config(PlaybackMode.SEQUENTIAL, PlaybackProgress.EMPTY, "", true, true),
                listener, progress -> { })) {
            scheduler.start();

            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS), "填空模式下也应当先发完整词");
            assertTrue(listener.blankLatch.await(5, TimeUnit.SECONDS), "主间隔之后应当出现首帧挖空");

            String frame = listener.blankFrames.peek();
            assertEquals("banana".length(), frame.length());
            assertTrue(frame.indexOf('_') >= 0, "首帧应当包含未揭开的下划线");
            assertEquals("banana", listener.lastBlankOriginal.word());
            assertTrue(listener.lastHidePhrases);
            assertTrue(listener.lastHideTranslation, "配置不显示释义时应当传递隐藏标记");
        }
    }

    @Test
    void disablingFillBlankJumpsToNextWordImmediately() throws InterruptedException {
        RecordingListener listener = new RecordingListener(1, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("banana", "cherry"),
                config(PlaybackMode.SEQUENTIAL, PlaybackProgress.EMPTY, "", true, true),
                listener, progress -> { })) {
            scheduler.start();
            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS));

            // 此刻已进入 banana 的填空阶段；关闭填空应当立即斩断流程并跳到下一个完整词
            scheduler.updateFillBlankSettings(false, 1, true, false);
            long deadline = System.currentTimeMillis() + 1500;
            while (listener.words.size() < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }

            assertEquals(2, listener.words.size(), "关闭填空后应当立即发下一个词");
            assertEquals("cherry", listener.words.toArray()[1]);
        }
    }
}

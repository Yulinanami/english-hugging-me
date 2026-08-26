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
 * WordScheduler 的单元测试。
 *
 * <p>单词切换的最小间隔为 2 秒，因此依赖定时推进的测试用例通过将起始进度设为最后一项来减少等待时间；
 * 依赖立即响应逻辑（启动、恢复、关闭填空）的测试用例均可快速完成。
 */
class WordSchedulerTest {

    /**
     * 回调数据收集辅助类。
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
     * 生成测试用配置对象：播放间隔设为 2 秒，填空开启时隐藏短语、不显示释义。
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

            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS), "第一个单词应当立即展示");
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

            assertTrue(progressLatch.await(2, TimeUnit.SECONDS), "切换单词后应当回调保存进度");
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

            // resume 应当立即播放下一个单词（播放间隔 2 秒，1 秒内收到即证明无需等待完整间隔）
            int seenBeforeResume = listener.words.size();
            scheduler.resume();
            long deadline = System.currentTimeMillis() + 1000;
            while (listener.words.size() <= seenBeforeResume && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }

            assertTrue(listener.words.size() > seenBeforeResume, "恢复播放后应当立即显示下一个单词");
            assertFalse(scheduler.isPaused());
        }
    }

    @Test
    void sequentialWithPrefixFinishesWithoutLoop() throws InterruptedException {
        // 只有设置了前缀时 loopPlayback=false 才生效（无前缀时默认循环播放），此逻辑由本测试用例验证
        RecordingListener listener = new RecordingListener(1, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("alpha", "apricot"),
                config(PlaybackMode.SEQUENTIAL, new PlaybackProgress(1, "", 0, 0), "a", false, false),
                listener, progress -> { })) {
            scheduler.start();

            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS));
            assertEquals("apricot", listener.words.peek(), "应当从保存的进度处继续播放");
            assertTrue(listener.finishedLatch.await(5, TimeUnit.SECONDS), "不循环时播完最后一个单词应当触发结束回调");
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
            assertTrue(listener.finishedLatch.await(5, TimeUnit.SECONDS), "随机模式播满单词总数后应当触发结束回调");
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
            assertTrue(listener.finishedLatch.await(5, TimeUnit.SECONDS), "乱序不重复模式播完一轮后应当触发结束回调");
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

            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS), "填空模式下也应当先展示完整单词");
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

            // 此刻已进入 banana 的填空阶段；关闭填空应当立即结束当前填空并切换到下一个完整单词
            scheduler.updateFillBlankSettings(false, 1, true, false);
            long deadline = System.currentTimeMillis() + 1500;
            while (listener.words.size() < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }

            assertEquals(2, listener.words.size(), "关闭填空后应当立即展示下一个单词");
            assertEquals("cherry", listener.words.toArray()[1]);
        }
    }

    @Test
    void resumeWithDelayPostponesNextWordEmission() throws InterruptedException {
        RecordingListener listener = new RecordingListener(1, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("apple", "banana"),
                config(PlaybackMode.SEQUENTIAL, PlaybackProgress.EMPTY, "", true, false),
                listener, progress -> { })) {
            scheduler.start();
            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS), "第一个单词应当立即展示");
            assertEquals("apple", listener.words.peek());

            // 模拟用户拖拽时暂停
            scheduler.pause();
            assertTrue(scheduler.isPaused());

            // 模拟用户松手，以延时 2 秒恢复
            scheduler.resumeWithDelay(2);
            assertFalse(scheduler.isPaused());

            // 500ms 内不应立即发射新单词（避免松手闪跳）
            Thread.sleep(500);
            assertEquals(1, listener.words.size(), "延时恢复时不应立即切词");

            // 等待延时结束后应当正常发射下一个单词
            long deadline = System.currentTimeMillis() + 2500;
            while (listener.words.size() < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertEquals(2, listener.words.size(), "延时结束后应当切换到下一个单词");
            assertEquals("banana", listener.words.toArray()[1]);
        }
    }

    @Test
    void resumeWithRemainingInheritsRemainingTime() throws InterruptedException {
        RecordingListener listener = new RecordingListener(1, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("apple", "banana"),
                config(PlaybackMode.SEQUENTIAL, PlaybackProgress.EMPTY, "", true, false),
                listener, progress -> { })) {
            scheduler.start();
            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS), "第一个单词应当立即展示");
            assertEquals("apple", listener.words.peek());

            // 在 300ms 时暂停（此时剩余约 1700ms）
            Thread.sleep(300);
            scheduler.pause();
            assertTrue(scheduler.isPaused());

            // 松手恢复，保底设为 500ms，应当等待剩余的约 1700ms
            scheduler.resumeWithRemaining(500);
            assertFalse(scheduler.isPaused());

            // 600ms 内不应切词
            Thread.sleep(600);
            assertEquals(1, listener.words.size(), "剩余时间充足时不应提前切词");

            // 2.2 秒后应正常切词
            long deadline = System.currentTimeMillis() + 2000;
            while (listener.words.size() < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertEquals(2, listener.words.size(), "应当在继承的剩余时间到达后切词");
            assertEquals("banana", listener.words.toArray()[1]);
        }
    }

    @Test
    void resumeWithRemainingAppliesGracePeriodWhenNearEnd() throws InterruptedException {
        RecordingListener listener = new RecordingListener(1, 0);
        try (WordScheduler scheduler = new WordScheduler(
                words("apple", "banana"),
                config(PlaybackMode.SEQUENTIAL, PlaybackProgress.EMPTY, "", true, false),
                listener, progress -> { })) {
            scheduler.start();
            assertTrue(listener.wordLatch.await(2, TimeUnit.SECONDS));

            // 等待 1800ms，只剩约 200ms
            Thread.sleep(1800);
            scheduler.pause();

            // 松手恢复，保底设为 800ms，应当等待 800ms 而不是 200ms，防止闪跳
            scheduler.resumeWithRemaining(800);

            Thread.sleep(400);
            assertEquals(1, listener.words.size(), "保底期内不应立即闪跳切词");

            long deadline = System.currentTimeMillis() + 1500;
            while (listener.words.size() < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertEquals(2, listener.words.size(), "保底期结束后应当平滑切词");
        }
    }
}

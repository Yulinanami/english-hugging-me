package me.englishhugging.core;

import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.core.settings.PlaybackProgress;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PlaybackCursor 的纯逻辑单元测试：不涉及线程与定时器，毫秒级完成。
 */
class PlaybackCursorTest {

    private static PlaybackCursor cursor(int wordCount, PlaybackMode mode, boolean loop, PlaybackProgress initial) {
        return new PlaybackCursor(wordCount, mode, loop, initial, new Random());
    }

    @Test
    void sequentialAdvancesInOrderAndWrapsWhenLooping() {
        PlaybackCursor cursor = cursor(3, PlaybackMode.SEQUENTIAL, true, PlaybackProgress.EMPTY);

        assertEquals(0, cursor.next());
        assertEquals(1, cursor.next());
        assertEquals(2, cursor.next());
        assertEquals(0, cursor.next(), "循环模式播到底应当回卷到开头");
    }

    @Test
    void sequentialStopsAtEndWithoutLoop() {
        PlaybackCursor cursor = cursor(2, PlaybackMode.SEQUENTIAL, false, PlaybackProgress.EMPTY);

        assertEquals(0, cursor.next());
        assertEquals(1, cursor.next());
        assertEquals(-1, cursor.next(), "不循环时播完应当返回 -1");
        assertEquals(-1, cursor.next(), "结束状态应当保持稳定");
    }

    @Test
    void sequentialResumesFromSavedProgress() {
        PlaybackCursor cursor = cursor(3, PlaybackMode.SEQUENTIAL, true, new PlaybackProgress(1, "", 0, 0));

        assertEquals(1, cursor.next(), "应当从保存的下标继续播放");
        assertEquals(2, cursor.snapshot().nextWordIndex(), "快照应当指向再下一个词");
    }

    @Test
    void sequentialOutOfRangeProgressIsResetToZero() {
        PlaybackCursor cursor = cursor(3, PlaybackMode.SEQUENTIAL, true, new PlaybackProgress(99, "", 0, 0));

        assertEquals(0, cursor.next(), "越界的历史进度应当作废归零");
    }

    @Test
    void shuffleNoRepeatCoversAllWordsExactlyOnceThenFinishes() {
        PlaybackCursor cursor = cursor(5, PlaybackMode.SHUFFLE_NO_REPEAT, false, PlaybackProgress.EMPTY);

        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            int position = cursor.next();
            assertTrue(position >= 0 && position < 5);
            assertTrue(seen.add(position), "同一轮内不应当重复出现同一个下标");
        }
        assertEquals(-1, cursor.next(), "不循环时抽完一轮应当返回 -1");
    }

    @Test
    void shuffleReshufflesNewRoundWhenLooping() {
        PlaybackCursor cursor = cursor(3, PlaybackMode.SHUFFLE_NO_REPEAT, true, PlaybackProgress.EMPTY);

        Set<Integer> firstRound = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            firstRound.add(cursor.next());
        }
        Set<Integer> secondRound = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            secondRound.add(cursor.next());
        }

        assertEquals(Set.of(0, 1, 2), firstRound);
        assertEquals(Set.of(0, 1, 2), secondRound, "循环模式抽完一轮应当重新洗牌再来一轮");
    }

    @Test
    void shuffleStateRoundTripsThroughSnapshot() {
        PlaybackCursor first = cursor(4, PlaybackMode.SHUFFLE_NO_REPEAT, false, PlaybackProgress.EMPTY);

        List<Integer> playedByFirst = new ArrayList<>();
        playedByFirst.add(first.next());
        playedByFirst.add(first.next());

        // 用快照重建游标：应当接着抽剩下的两个，不与前两个重复
        PlaybackCursor resumed = cursor(4, PlaybackMode.SHUFFLE_NO_REPEAT, false, first.snapshot());

        Set<Integer> playedByResumed = new HashSet<>();
        playedByResumed.add(resumed.next());
        playedByResumed.add(resumed.next());
        assertEquals(-1, resumed.next());

        Set<Integer> all = new HashSet<>(playedByFirst);
        all.addAll(playedByResumed);
        assertEquals(Set.of(0, 1, 2, 3), all, "快照恢复后应当恰好补完剩余的下标");
    }

    @Test
    void corruptedShuffleOrderIsRebuilt() {
        // 长度不符 / 重复 / 越界的序列串都应当被丢弃重塑，而不是崩溃
        PlaybackCursor cursor = cursor(3, PlaybackMode.SHUFFLE_NO_REPEAT, false,
                new PlaybackProgress(0, "9,9,abc", 0, 0));

        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            int position = cursor.next();
            assertTrue(position >= 0 && position < 3);
            seen.add(position);
        }
        assertEquals(3, seen.size());
    }

    @Test
    void randomFinishesAfterWholeSetWithoutLoopAndCountsPlays() {
        PlaybackCursor cursor = cursor(3, PlaybackMode.RANDOM, false, PlaybackProgress.EMPTY);

        for (int i = 0; i < 3; i++) {
            int position = cursor.next();
            assertTrue(position >= 0 && position < 3);
        }
        assertEquals(-1, cursor.next(), "随机模式本轮播满词库数量后应当结束");
        assertEquals(3, cursor.snapshot().randomPlayedCount());
    }

    @Test
    void randomSessionResetAllowsReplay() {
        PlaybackCursor cursor = cursor(2, PlaybackMode.RANDOM, false, PlaybackProgress.EMPTY);

        cursor.next();
        cursor.next();
        assertEquals(-1, cursor.next());

        cursor.resetSession();
        assertNotEquals(-1, cursor.next(), "重置会话后应当可以重新播放");
    }
}

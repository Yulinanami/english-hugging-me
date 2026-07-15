package me.englishhugging.core;

import me.englishhugging.core.display.FillBlankGenerator;
import me.englishhugging.core.model.WordEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FillBlankSession 的纯逻辑单元测试：不涉及线程与定时器，毫秒级完成。
 */
class FillBlankSessionTest {

    @Test
    void firstFrameShowsInitialBlanks() {
        WordEntry entry = new WordEntry("banana", null, null);
        FillBlankSession session = new FillBlankSession(entry, new FillBlankGenerator());

        String frame = session.nextFrame();

        assertEquals("banana".length(), frame.length(), "挖空不应当改变单词长度");
        assertTrue(frame.indexOf('_') >= 0, "首帧应当带着未揭开的下划线");
        assertEquals(entry, session.entry());
    }

    @Test
    void framesProgressivelyRevealUntilComplete() {
        WordEntry entry = new WordEntry("elephant", null, null);
        FillBlankSession session = new FillBlankSession(entry, new FillBlankGenerator());

        String frame = session.nextFrame();
        int initialBlanks = countBlanks(frame);
        assertTrue(initialBlanks > 0);

        // 每推进一帧应当恰好少一个下划线，直到完整还原原词
        String lastFrame = frame;
        for (int expected = initialBlanks - 1; expected >= 0; expected--) {
            lastFrame = session.nextFrame();
            assertEquals(expected, countBlanks(lastFrame));
        }

        assertEquals("elephant", lastFrame, "最后一帧应当是完整的原词");
        assertNull(session.nextFrame(), "揭完所有空位后应当宣告会话结束");
    }

    private static int countBlanks(String frame) {
        int count = 0;
        for (int i = 0; i < frame.length(); i++) {
            if (frame.charAt(i) == '_') {
                count++;
            }
        }
        return count;
    }
}

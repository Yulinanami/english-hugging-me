package me.englishhugging.core;

import me.englishhugging.core.display.FillBlankGenerator;
import me.englishhugging.core.model.WordEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个单词的填空考核会话：从“挖出空位的初始帧”开始，随每次心跳逐步揭开一个字母。
 *
 * <p>职责边界：只管一个词的挖空与揭字状态，不含线程、定时与回调逻辑，
 * 因此可以脱离定时器直接进行毫秒级单元测试。
 *
 * <p>非线程安全——由 {@link WordScheduler} 在自身锁内创建、推进和丢弃。
 */
final class FillBlankSession {

    private final WordEntry entry;
    private final FillBlankGenerator generator;
    private final List<Integer> remainingBlanks;

    private String currentWord;
    private boolean initialShown = false;

    FillBlankSession(WordEntry entry, FillBlankGenerator generator) {
        this.entry = entry;
        this.generator = generator;

        FillBlankGenerator.BlankResult result = generator.generateBlanked(entry.word());
        this.currentWord = result.blankedWord();
        this.remainingBlanks = new ArrayList<>(result.blankPositions());
    }

    /** 本会话考核的原始词条（供 UI 渲染释义等后备信息）。 */
    WordEntry entry() {
        return this.entry;
    }

    /**
     * 推进一帧。
     *
     * @return 本次心跳要展示的字符串：第一次调用返回初始挖空帧，其后每次揭开一个字母；
     *         返回 null 表示所有空位都已揭完，会话结束，调度器应当落回正常取词
     */
    String nextFrame() {
        if (!this.initialShown) {
            // 第一步：展示第一次刚被挖出空位的样子（此时一个空都没填）
            this.initialShown = true;
            return this.currentWord;
        }

        if (this.remainingBlanks.isEmpty()) {
            // 所有的空位都填完了，宣告会话结束
            return null;
        }

        // 慢慢地一个一个把空填补回去
        this.currentWord = this.generator.fillOneBlank(this.currentWord, this.entry.word(), this.remainingBlanks);
        return this.currentWord;
    }
}

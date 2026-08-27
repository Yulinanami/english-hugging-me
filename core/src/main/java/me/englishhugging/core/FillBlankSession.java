package me.englishhugging.core;

import me.englishhugging.core.display.FillBlankGenerator;
import me.englishhugging.core.model.WordEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个单词的填空提示过程。
 *
 * <p>这个类负责记录一个单词在挖空复习时的过程：
 * 从一开始被挖掉几个字母（如 "a_p_e"），到每次调用 {@link #nextFrame()} 时逐步恢复一个字母，
 * 直到所有字母全部恢复完毕。
 */
final class FillBlankSession {

    /** 当前参与填空复习的原始单词条目 */
    private final WordEntry entry;

    /** 挖空与补全辅助工具 */
    private final FillBlankGenerator generator;

    /** 剩余尚未揭开的挖空字母位置列表 */
    private final List<Integer> remainingBlanks;

    /** 当前帧显示的带空位或部分揭开的单词文本 */
    private String currentWord;

    /** 标记初始挖空状态是否已经展示过 */
    private boolean initialShown = false;

    /**
     * 创建单词填空复习过程对象。
     *
     * @param entry     要进行填空测试的单词条目
     * @param generator 填空提示工具
     */
    FillBlankSession(WordEntry entry, FillBlankGenerator generator) {
        this.entry = entry;
        this.generator = generator;

        FillBlankGenerator.BlankResult result = generator.generateBlanked(entry.word());
        this.currentWord = result.blankedWord();
        this.remainingBlanks = new ArrayList<>(result.blankPositions());
    }

    /**
     * 获取当前测试的原始单词条目。
     *
     * @return 原始单词条目
     */
    WordEntry entry() {
        return this.entry;
    }

    /**
     * 推进填空进度，返回下一步要显示的单词字符串。
     *
     * <p>首次调用返回初始挖空状态的单词，之后每次调用填回一个字母；全部填满后返回 null。
     *
     * @return 当前要显示的单词字符串，若全部填完则返回 null
     */
    String nextFrame() {
        if (!this.initialShown) {
            // 首次展示初始挖空单词
            this.initialShown = true;
            return this.currentWord;
        }

        if (this.remainingBlanks.isEmpty()) {
            // 所有空位已全部揭开，填空结束
            return null;
        }

        // 揭开下一个空位
        this.currentWord = this.generator.fillOneBlank(this.currentWord, this.entry.word(), this.remainingBlanks);
        return this.currentWord;
    }
}

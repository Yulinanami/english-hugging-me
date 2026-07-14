package me.englishhugging.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单个单词的完整词条模型，对应 JSON 词库中的一个独立条目。
 *
 * <p>包含了单词本体、一系列不同的词性与翻译，以及若干帮助理解的常用短语。
 * 此类是整个项目的核心数据结构。
 *
 * <p>紧凑构造器会把 null 集合归一为空列表、剔除 null 元素并做不可变包装，
 * 因此 {@link #translations()} 与 {@link #phrases()} 永远不会返回 null，
 * 外部也无法修改其内容。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * WordEntry entry = new WordEntry("apple",
 *         Collections.singletonList(new Translation("苹果", "n.")),
 *         Collections.singletonList(new Phrase("an apple a day", "每天一个苹果")));
 * System.out.println(entry.word());
 * </code></pre>
 *
 * @param word         英文单词本体，如 "apple"
 * @param translations 该单词的一组释义，允许一个单词在不同词性下有多个翻译
 * @param phrases      包含该单词的常用短语或例句列表
 */
public record WordEntry(String word, List<Translation> translations, List<Phrase> phrases) {

    public WordEntry {
        translations = safeCopy(translations);
        phrases = safeCopy(phrases);
    }

    /**
     * 归一化集合：null 集合视为空列表，剔除 null 元素（用户手编 JSON 可能出现），
     * 并做不可变包装。core 需兼容 Android minSdk 26，故不能使用 List.copyOf（API 30+）。
     */
    private static <T> List<T> safeCopy(List<T> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> copy = new ArrayList<>(source.size());
        for (T item : source) {
            if (item != null) {
                copy.add(item);
            }
        }
        return Collections.unmodifiableList(copy);
    }
}

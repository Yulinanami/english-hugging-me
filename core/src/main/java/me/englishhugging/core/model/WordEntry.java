package me.englishhugging.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 词库中的单个单词。
 *
 * <p>包含英文单词拼写、中文释义列表以及常用短语例句。
 *
 * @param word         英文单词拼写，如 "apple"
 * @param translations 中文释义列表
 * @param phrases      常用短语与例句列表
 */
public record WordEntry(String word, List<Translation> translations, List<Phrase> phrases) {

    public WordEntry {
        translations = safeCopy(translations);
        phrases = safeCopy(phrases);
    }

    /**
     * 复制列表并转为只读列表，同时剔除 null 元素。
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

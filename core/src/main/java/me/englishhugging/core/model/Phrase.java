package me.englishhugging.core.model;

/**
 * 单词短语与例句。
 *
 * <p>保存英文短语及其对应的中文翻译。
 *
 * @param phrase      英文短语或例句内容
 * @param translation 短语对应的中文翻译
 */
public record Phrase(String phrase, String translation) {
}

package me.englishhugging.core.model;

/**
 * 单词释义与词性。
 *
 * <p>保存单词的中文释义以及对应的词性（如 "n."、"v." 等）。
 *
 * @param translation 中文释义内容，例如："苹果"
 * @param type        单词词性标识，例如："n."、"v."、"adj."
 */
public record Translation(String translation, String type) {
}

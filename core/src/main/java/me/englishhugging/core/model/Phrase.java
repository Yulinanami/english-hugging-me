package me.englishhugging.core.model;

/**
 * 单词相关短语及例句数据模型。
 *
 * <p>用于存储一个英文短语及其对应的中文翻译，通常作为 {@link WordEntry} 内部
 * 帮助用户更好地理解单词用法的辅助信息。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 创建一个短语对象并输出
 * Phrase p = new Phrase("an apple a day", "每天一个苹果");
 * System.out.println(p.phrase() + " -> " + p.translation());
 * </code></pre>
 *
 * @param phrase      英文短语或例句内容
 * @param translation 短语对应的中文翻译
 */
public record Phrase(String phrase, String translation) {
}

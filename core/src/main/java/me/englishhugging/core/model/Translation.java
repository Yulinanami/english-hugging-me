package me.englishhugging.core.model;

/**
 * 单词释义数据模型。
 *
 * <p>该类用于承载单词的单个翻译结果以及该翻译所对应的词性。
 * 它通常作为 {@link WordEntry} 内部的一个组成部分被加载和展示。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 创建一个名词 "苹果" 的释义对象
 * Translation t = new Translation("苹果", "n.");
 * System.out.println(t.type() + " " + t.translation()); // 输出: n. 苹果
 * </code></pre>
 *
 * @param translation 中文释义内容，例如："苹果"
 * @param type        单词词性标识，例如："n."、"v."、"adj."
 */
public record Translation(String translation, String type) {
}

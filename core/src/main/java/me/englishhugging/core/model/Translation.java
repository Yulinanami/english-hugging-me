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
 * System.out.println(t.getType() + " " + t.getTranslation()); // 输出: n. 苹果
 * </code></pre>
 */
public final class Translation {

    /**
     * 中文释义内容，例如："苹果"
     */
    private String translation;

    /**
     * 单词词性标识，例如："n."、"v."、"adj."
     */
    private String type;

    /**
     * 默认构造函数，供 Gson 等序列化工具使用。
     */
    public Translation() {
        // 空构造函数，反序列化需要
    }

    /**
     * 构造一个新的单词释义对象。
     *
     * @param translation 单词的具体翻译内容
     * @param type        该翻译对应的词性
     */
    public Translation(String translation, String type) {
        this.translation = translation;
        this.type = type;
    }

    /**
     * 获取具体的中文翻译内容。
     *
     * @return 翻译字符串
     */
    public String getTranslation() {
        return this.translation;
    }

    /**
     * 设置中文翻译内容。
     *
     * @param translation 翻译内容
     */
    public void setTranslation(String translation) {
        this.translation = translation;
    }

    /**
     * 获取单词的词性标识。
     *
     * @return 词性字符串
     */
    public String getType() {
        return this.type;
    }

    /**
     * 设置单词的词性标识。
     *
     * @param type 词性字符串
     */
    public void setType(String type) {
        this.type = type;
    }
}

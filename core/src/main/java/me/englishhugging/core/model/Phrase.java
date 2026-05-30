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
 * System.out.println(p.getPhrase() + " -> " + p.getTranslation());
 * </code></pre>
 */
public final class Phrase {

    /**
     * 英文短语或例句内容
     */
    private String phrase;

    /**
     * 短语对应的中文翻译
     */
    private String translation;

    /**
     * 默认构造函数，供 Gson 等序列化工具使用。
     */
    public Phrase() {
        // 空构造函数
    }

    /**
     * 构造一个新的短语对象。
     *
     * @param phrase      英文短语内容
     * @param translation 短语的中文翻译
     */
    public Phrase(String phrase, String translation) {
        this.phrase = phrase;
        this.translation = translation;
    }

    /**
     * 获取英文短语内容。
     *
     * @return 短语字符串
     */
    public String getPhrase() {
        return this.phrase;
    }

    /**
     * 设置英文短语内容。
     *
     * @param phrase 英文短语
     */
    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    /**
     * 获取短语对应的中文翻译。
     *
     * @return 翻译字符串
     */
    public String getTranslation() {
        return this.translation;
    }

    /**
     * 设置短语对应的中文翻译。
     *
     * @param translation 中文翻译
     */
    public void setTranslation(String translation) {
        this.translation = translation;
    }
}

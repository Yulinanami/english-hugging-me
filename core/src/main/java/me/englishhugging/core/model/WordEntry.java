package me.englishhugging.core.model;

import java.util.List;

/**
 * 单个单词的完整词条模型，对应 JSON 词库中的一个独立条目。
 *
 * <p>包含了单词本体、一系列不同的词性与翻译，以及若干帮助理解的常用短语。
 * 此类是整个项目的核心数据结构。
 * 
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 构建包含多个释义和短语的单词词条
 * List&lt;Translation&gt; translations = new ArrayList&lt;&gt;();
 * translations.add(new Translation("苹果", "n."));
 * 
 * List&lt;Phrase&gt; phrases = new ArrayList&lt;&gt;();
 * phrases.add(new Phrase("an apple a day", "每天一个苹果"));
 * 
 * WordEntry entry = new WordEntry("apple", translations, phrases);
 * System.out.println(entry.getWord());
 * </code></pre>
 */
public final class WordEntry {

    /**
     * 英文单词本体，如 "apple"
     */
    private String word;

    /**
     * 该单词的一组释义，允许一个单词在不同词性下有多个翻译
     */
    private List<Translation> translations;

    /**
     * 包含该单词的常用短语或例句列表
     */
    private List<Phrase> phrases;

    /**
     * 默认构造函数，供 Gson 等序列化框架通过反射初始化对象时使用。
     */
    public WordEntry() {
        // 空构造函数
    }

    /**
     * 构造一个新的单词词条对象。
     *
     * @param word         单词本体
     * @param translations 翻译列表
     * @param phrases      短语列表
     */
    public WordEntry(String word, List<Translation> translations, List<Phrase> phrases) {
        this.word = word;
        this.translations = translations;
        this.phrases = phrases;
    }

    /**
     * 获取英文单词本体。
     *
     * @return 单词字符串
     */
    public String getWord() {
        return this.word;
    }

    /**
     * 获取单词的所有释义。
     *
     * @return {@link Translation} 对象列表。如果文件未定义则可能为 null，请注意判空。
     */
    public List<Translation> getTranslations() {
        return this.translations;
    }

    /**
     * 获取单词的相关短语或例句。
     *
     * @return {@link Phrase} 对象列表。如果文件未定义则可能为 null，请注意判空。
     */
    public List<Phrase> getPhrases() {
        return this.phrases;
    }

    /**
     * 覆写 toString 方法以方便在控制台进行调试打印。
     *
     * @return 包含单词及其翻译的文本表示
     */
    @Override
    public String toString() {
        int transCount = 0;
        if (this.translations != null) {
            transCount = this.translations.size();
        }
        
        int phraseCount = 0;
        if (this.phrases != null) {
            phraseCount = this.phrases.size();
        }
        
        return "WordEntry{word='" + this.word + "', translations=" + transCount + ", phrases=" + phraseCount + "}";
    }

    /**
     * 创建一个深拷贝（防卫性拷贝），防止外部修改内部集合。
     *
     * @return 拷贝后的词条对象
     */
    public WordEntry defensiveCopy() {
        java.util.List<Translation> transCopy = null;
        if (this.translations != null) {
            transCopy = new java.util.ArrayList<>(this.translations.size());
            for (Translation t : this.translations) {
                transCopy.add(new Translation(t.getTranslation(), t.getType()));
            }
        }
        
        java.util.List<Phrase> phraseCopy = null;
        if (this.phrases != null) {
            phraseCopy = new java.util.ArrayList<>(this.phrases.size());
            for (Phrase p : this.phrases) {
                phraseCopy.add(new Phrase(p.getPhrase(), p.getTranslation()));
            }
        }
        
        return new WordEntry(this.word, transCopy, phraseCopy);
    }
}

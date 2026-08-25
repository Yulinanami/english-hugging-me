package me.englishhugging.core.model;

/**
 * 单词展示文本片段。
 *
 * <p>将单词、词性、释义与短语拆分为独立片段，便于富文本界面分别应用颜色与字号样式。
 *
 * @param type 片段类型（单词、词性、释义等）
 * @param text 片段显示的文本内容
 */
public record WordDisplaySegment(Type type, String text) {

    /**
     * 文本片段类型。
     */
    public enum Type {
        /** 英文单词 */
        WORD,
        /** 词性标识（如 n.、v.） */
        TYPE,
        /** 中文释义 */
        TRANSLATION,
        /** 英文短语或例句 */
        PHRASE,
        /** 短语中文翻译 */
        PHRASE_TRANSLATION,
        /** 换行符 */
        LINE_BREAK
    }
}

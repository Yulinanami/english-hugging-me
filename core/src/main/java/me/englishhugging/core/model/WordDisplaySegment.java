package me.englishhugging.core.model;

/**
 * 单词展示片段，用于对一个复杂的词条文本进行分段和着色渲染。
 *
 * <p>在悬浮窗等富文本 UI 中，我们不能简单地将整个词条拼接成一个长字符串，
 * 而是需要将其分解为具有不同语义的片段（如单词本身、词性、中文释义），
 * 这样就可以根据设置应用不同的颜色和字体大小。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 创建一个代表词性的文本片段
 * WordDisplaySegment segment = new WordDisplaySegment(WordDisplaySegment.Type.TYPE, "n. ");
 * 
 * if (segment.getType() == WordDisplaySegment.Type.TYPE) {
 *     // 根据类型设置专属的颜色渲染
 *     renderText(segment.getText(), Colors.RED);
 * }
 * </code></pre>
 */
public final class WordDisplaySegment {

    /**
     * 片段类型的枚举，定义了此片段在界面上的视觉语义。
     */
    public enum Type {
        /** 英文单词本体 */
        WORD,
        /** 词性标识 (如 n., v.) */
        TYPE,
        /** 中文释义内容 */
        TRANSLATION,
        /** 英文短语或例句 */
        PHRASE,
        /** 短语对应的中文翻译 */
        PHRASE_TRANSLATION,
        /** 控制排版的换行符 */
        LINE_BREAK
    }

    /** 当前片段的类型 */
    private final Type type;
    
    /** 当前片段承载的文本内容 */
    private final String text;

    /**
     * 构造一个新的单词展示片段。
     *
     * @param type 片段的视觉语义类型
     * @param text 片段需要显示的纯文本
     */
    public WordDisplaySegment(Type type, String text) {
        this.type = type;
        this.text = text;
    }

    /**
     * 获取该片段的视觉类型。
     *
     * @return 片段枚举类型
     */
    public Type getType() {
        return this.type;
    }

    /**
     * 获取该片段的文本内容。
     *
     * @return 供渲染的字符串
     */
    public String getText() {
        return this.text;
    }
}

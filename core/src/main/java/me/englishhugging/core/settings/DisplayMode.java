package me.englishhugging.core.settings;

/**
 * 单词在界面上的显示模式枚举。
 * 
 * <p>用户可以在设置中自由选择词条内容的丰富程度，以适应不同的屏幕大小
 * 或不同的记忆阶段需求。
 * 
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 将显示模式切换为最精简的只显示单词
 * settings.setDisplayMode(DisplayMode.WORD_ONLY);
 * </code></pre>
 */
public enum DisplayMode {
    
    /**
     * 极简模式：只显示英文单词本体。
     * 适合复习阶段或极小的桌面悬浮窗。
     */
    WORD_ONLY("只显示单词"),
    
    /**
     * 标准模式：显示单词本体和对应的中文释义。
     * 适合绝大部分日常背词场景。
     */
    WORD_WITH_TRANSLATION("单词 + 释义"),
    
    /**
     * 详细模式：显示单词、释义，并且追加相关的短语例句。
     * 适合初次记忆或需要结合语境理解的场景。
     */
    WORD_WITH_TRANSLATION_AND_PHRASE("单词 + 释义 + 短语");

    /**
     * 对应在 UI 下拉列表中显示的人类可读标签
     */
    private final String label;

    /**
     * 构造一个新的枚举值并绑定其显示标签。
     *
     * @param label 中文显示名称
     */
    DisplayMode(String label) {
        this.label = label;
    }

    /**
     * 获取供 UI 界面展示的中文描述。
     *
     * @return 中文标签字符串
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * 获取所有的可用显示模式中文标签，常用于填充 UI 下拉列表。
     *
     * @return 包含所有标签的字符串数组
     */
    public static String[] labels() {
        DisplayMode[] values = values();
        String[] labelsList = new String[values.length];
        
        for (int i = 0; i < values.length; i++) {
            labelsList[i] = values[i].label;
        }
        
        return labelsList;
    }
}

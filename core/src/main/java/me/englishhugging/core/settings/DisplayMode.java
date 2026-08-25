package me.englishhugging.core.settings;

/**
 * 单词在界面上的显示模式。
 * 
 * <p>用户可以在设置中选择词条内容的丰富程度（如只显示单词、显示单词加释义、显示单词加释义及短语）。
 */
public enum DisplayMode {
    
    /**
     * 极简模式：只显示英文单词。
     * 适合复习阶段或极小的桌面悬浮窗。
     */
    WORD_ONLY("只显示单词"),
    
    /**
     * 标准模式：显示单词和中文释义。
     * 适合绝大部分日常背词场景。
     */
    WORD_WITH_TRANSLATION("单词 + 释义"),
    
    /**
     * 详细模式：显示单词、释义，并且追加相关的短语例句。
     * 适合初次记忆或需要结合语境理解的场景。
     */
    WORD_WITH_TRANSLATION_AND_PHRASE("单词 + 释义 + 短语");

    /**
     * 下拉框中显示的中文名称
     */
    private final String label;

    /**
     * 初始化显示模式。
     *
     * @param label 下拉框中显示的中文名称
     */
    DisplayMode(String label) {
        this.label = label;
    }

    /**
     * 返回下拉框中显示的中文名称。
     *
     * @return 中文标签字符串
     */
    @Override
    public String toString() {
        return this.label;
    }

    /**
     * 获取所有显示模式的中文名称数组。
     *
     * @return 包含所有显示模式中文名称的字符串数组
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

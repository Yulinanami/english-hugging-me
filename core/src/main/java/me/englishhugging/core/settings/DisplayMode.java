package me.englishhugging.core.settings;

public enum DisplayMode {
    WORD_ONLY("只显示单词"),
    WORD_WITH_TRANSLATION("单词 + 释义"),
    WORD_WITH_TRANSLATION_AND_PHRASE("单词 + 释义 + 短语");

    private final String label;

    DisplayMode(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static String[] labels() {
        DisplayMode[] values = values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].label;
        }
        return labels;
    }
}

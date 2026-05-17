package me.englishhugging.core.settings;

public enum PlaybackMode {
    SEQUENTIAL("顺序播放"),
    RANDOM("随机播放"),
    SHUFFLE_NO_REPEAT("随机不重复");

    private final String label;

    PlaybackMode(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static String[] labels() {
        PlaybackMode[] values = values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].label;
        }
        return labels;
    }
}

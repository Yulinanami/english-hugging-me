package me.englishhugging.core.settings;

/**
 * 词库播放顺序控制模式。
 * 
 * <p>定义了选取下一个播放单词的顺序规则（顺序播放、完全随机、随机不重复）。
 */
public enum PlaybackMode {

    /**
     * 顺序播放模式。
     * 按照词库文件中单词出现的先后顺序依次播放。
     */
    SEQUENTIAL("顺序播放"),

    /**
     * 完全随机模式。
     * 每次都从整个词库中随机挑取一个单词。由于是独立随机，因此有一定概率会短时间内重复出现同一个词。
     */
    RANDOM("完全随机"),

    /**
     * 随机但不重复模式（乱序模式）。
     * 生成打乱的单词列表并依次播放，在播完一轮前绝不重复。
     */
    SHUFFLE_NO_REPEAT("随机不重复");

    /**
     * 下拉框中显示的中文名称
     */
    private final String label;

    /**
     * 初始化播放模式。
     *
     * @param label 下拉框中显示的中文名称
     */
    PlaybackMode(String label) {
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
     * 获取所有播放模式的中文名称数组。
     *
     * @return 包含所有播放模式中文名称的字符串数组
     */
    public static String[] labels() {
        PlaybackMode[] values = values();
        String[] labelsList = new String[values.length];
        
        for (int i = 0; i < values.length; i++) {
            labelsList[i] = values[i].label;
        }
        
        return labelsList;
    }
}

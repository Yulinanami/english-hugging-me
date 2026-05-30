package me.englishhugging.core.settings;

/**
 * 词库播放顺序控制枚举。
 * 
 * <p>定义了 {@link me.englishhugging.core.WordScheduler} 选取下一个播放单词的算法逻辑。
 * 支持严格按顺序、纯随机或是无放回乱序三种核心模式。
 * 
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 设置为随机打乱但不重复，适合需要全面复习的场景
 * settings.setPlaybackMode(PlaybackMode.SHUFFLE_NO_REPEAT);
 * </code></pre>
 */
public enum PlaybackMode {

    /**
     * 顺序播放模式。
     * 严格按照 JSON 词库文件中单词出现的先后顺序进行轮询播放。
     */
    SEQUENTIAL("顺序播放"),

    /**
     * 完全随机模式。
     * 每次都从整个词库中随机挑取一个单词。由于是独立随机，因此有一定概率会短时间内重复出现同一个词。
     */
    RANDOM("完全随机"),

    /**
     * 随机但不重复模式（乱序模式）。
     * 生成一个全局乱序索引列表，按照该列表遍历。在遍历完所有单词之前，绝对不会出现重复的词汇。
     */
    SHUFFLE_NO_REPEAT("随机不重复");

    /**
     * 对应设置 UI 中的中文标签。
     */
    private final String label;

    /**
     * 构造播放模式枚举值。
     *
     * @param label UI 中文名称
     */
    PlaybackMode(String label) {
        this.label = label;
    }

    /**
     * 获取用于界面显示的中文名称。
     *
     * @return 中文标签
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * 获取所有的可用播放模式中文标签，便于绑定到 UI 下拉菜单。
     *
     * @return 标签字符串数组
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

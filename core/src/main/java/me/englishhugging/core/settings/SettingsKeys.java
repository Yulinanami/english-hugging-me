package me.englishhugging.core.settings;

/**
 * 核心设置项常量键名集合。
 *
 * <p>此接口（或常量类）定义了所有用于持久化存储或在组件之间传递设置时所需要的 Key 字符串。
 * 集中管理 Key 可以有效防止硬编码导致的拼写错误，并且方便全局重构与查找。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 在存储引擎中使用常量 Key 保存或读取值
 * String vocabFile = storage.getString(SettingsKeys.VOCABULARY_FILE_NAME, "1-初中-顺序.json");
 * </code></pre>
 */
public final class SettingsKeys {
    
    /** 当前选中的词库文件名，例如 "1-初中-顺序.json" */
    public static final String VOCABULARY_FILE_NAME = "vocabularyFileName";
    
    /** 词库文件的根路径（如 assets 或本地目录） */
    public static final String VOCABULARY_PATH = "vocabularyPath";
    
    /** 词条显示模式枚举值，关联 {@link DisplayMode} */
    public static final String DISPLAY_MODE = "displayMode";
    
    /** 桌面端悬浮窗的交互模式（如鼠标穿透），关联 {@link OverlayMode} */
    public static final String OVERLAY_MODE = "overlayMode";
    
    /** 词汇播放的顺序模式（如顺序、随机），关联 {@link PlaybackMode} */
    public static final String PLAYBACK_MODE = "playbackMode";
    
    /** 词汇轮播的时间间隔（秒） */
    public static final String INTERVAL_SECONDS = "intervalSeconds";
    
    /** 顺序播放模式下，下一个即将展示的单词的数组索引 */
    public static final String NEXT_WORD_INDEX = "nextWordIndex";
    
    /** 乱序播放模式下，全局的乱序索引列表序列化字符串 */
    public static final String SHUFFLE_ORDER = "shuffleOrder";
    
    /** 乱序播放模式下，当前播放到乱序列表中的哪个位置 */
    public static final String SHUFFLE_POSITION = "shufflePosition";
    
    /** 完全随机播放模式下，当前学习周期内已经随机播放了多少个单词 */
    public static final String RANDOM_PLAYED_COUNT = "randomPlayedCount";
    
    /** 桌面端：悬浮窗左上角 X 轴坐标 */
    public static final String X = "x";
    
    /** 桌面端：悬浮窗左上角 Y 轴坐标 */
    public static final String Y = "y";
    
    /** 桌面端：悬浮窗的宽度 */
    public static final String WIDTH = "width";
    
    /** 桌面端：悬浮窗的高度 */
    public static final String HEIGHT = "height";
    
    /** 桌面端/移动端：悬浮窗的背景不透明度 (0.0 到 1.0) */
    public static final String OPACITY = "opacity";
    
    /** 单词本体文本的渲染颜色 (Hex 字符串，如 "#FFFFFF") */
    public static final String WORD_COLOR = "wordColor";
    
    /** 词性标识文本的渲染颜色 */
    public static final String TYPE_COLOR = "typeColor";
    
    /** 中文释义文本的渲染颜色 */
    public static final String TRANSLATION_COLOR = "translationColor";
    
    /** 辅助短语和例句文本的渲染颜色 */
    public static final String PHRASE_COLOR = "phraseColor";
    
    /** 主体单词的字体大小 */
    public static final String WORD_FONT_SIZE = "wordFontSize";
    
    /** 释义、短语等详情内容的字体大小 */
    public static final String DETAIL_FONT_SIZE = "detailFontSize";
    
    /** 单词过滤前缀：如果设置了此值，则只播放以该字母或字符串开头的单词 */
    public static final String STARTING_PREFIX = "startingPrefix";
    
    /** 是否开启循环播放模式（播放到结尾后自动从头开始） */
    public static final String LOOP_PLAYBACK = "loopPlayback";
    
    /** 是否开启桌面端自动调整悬浮窗大小的模式 */
    public static final String RESIZE_MODE = "resizeMode";
    
    /** 是否开启填空模式（即挖空单词的部分字母要求用户回想） */
    public static final String FILL_BLANK_MODE = "fillBlankMode";
    
    /** 填空模式下，每个提示字母展现的时间间隔（秒） */
    public static final String FILL_BLANK_INTERVAL_SECONDS = "fillBlankIntervalSeconds";
    
    /** 填空模式进行中，是否隐藏辅助的短语例句（防止通过例句猜出单词） */
    public static final String FILL_BLANK_HIDE_PHRASES = "fillBlankHidePhrases";
    
    /** 填空模式进行中，是否仍然显示中文释义 */
    public static final String FILL_BLANK_SHOW_TRANSLATION = "fillBlankShowTranslation";

    /**
     * 私有构造函数，防止常量类被实例化。
     */
    private SettingsKeys() {
        // 无需实例化
    }
}

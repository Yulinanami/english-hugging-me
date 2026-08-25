package me.englishhugging.core.settings;

/**
 * 应用设置常量键名集合。
 *
 * <p>定义了所有用于保存或读取配置项的唯一键名。
 */
public final class SettingsKeys {
    
    /** 当前选中的词库文件名，例如 "1-初中-顺序.json" */
    public static final String VOCABULARY_FILE_NAME = "vocabularyFileName";
    
    /** 词库文件的根路径（如 assets 或本地目录） */
    public static final String VOCABULARY_PATH = "vocabularyPath";
    
    /** 单词显示模式，关联 {@link DisplayMode} */
    public static final String DISPLAY_MODE = "displayMode";
    
    /** 桌面端悬浮窗的交互模式（如鼠标穿透），关联 {@link OverlayMode} */
    public static final String OVERLAY_MODE = "overlayMode";
    
    /** 词汇播放的顺序模式（如顺序、随机），关联 {@link PlaybackMode} */
    public static final String PLAYBACK_MODE = "playbackMode";
    
    /** 单词切换间隔时间（秒） */
    public static final String INTERVAL_SECONDS = "intervalSeconds";
    
    /** 顺序播放模式下，下一个即将展示的单词序号 */
    public static final String NEXT_WORD_INDEX = "nextWordIndex";
    
    /** 乱序播放模式下，打乱的单词序号列表字符串（逗号分隔） */
    public static final String SHUFFLE_ORDER = "shuffleOrder";
    
    /** 乱序播放模式下，当前播放到的乱序列表位置 */
    public static final String SHUFFLE_POSITION = "shufflePosition";
    
    /** 完全随机播放模式下，当前已累计随机播放的单词数量 */
    public static final String RANDOM_PLAYED_COUNT = "randomPlayedCount";
    
    /** 悬浮窗左上角 X 轴坐标（像素） */
    public static final String X = "x";
    
    /** 悬浮窗左上角 Y 轴坐标（像素） */
    public static final String Y = "y";
    
    /** 悬浮窗宽度（像素） */
    public static final String WIDTH = "width";
    
    /** 悬浮窗高度（像素） */
    public static final String HEIGHT = "height";
    
    /** 悬浮窗背景不透明度（0.0 到 1.0） */
    public static final String OPACITY = "opacity";
    
    /** 单词文本渲染颜色（十六进制颜色字符串，如 "#FFFFFF"） */
    public static final String WORD_COLOR = "wordColor";
    
    /** 词性标识文本渲染颜色 */
    public static final String TYPE_COLOR = "typeColor";
    
    /** 中文释义文本渲染颜色 */
    public static final String TRANSLATION_COLOR = "translationColor";
    
    /** 例句短语文本颜色 */
    public static final String PHRASE_COLOR = "phraseColor";
    
    /** 单词字号（像素/sp） */
    public static final String WORD_FONT_SIZE = "wordFontSize";
    
    /** 释义与短语字号（像素/sp） */
    public static final String DETAIL_FONT_SIZE = "detailFontSize";
    
    /** 单词首字母筛选前缀 */
    public static final String STARTING_PREFIX = "startingPrefix";
    
    /** 是否循环播放 */
    public static final String LOOP_PLAYBACK = "loopPlayback";
    
    /** 是否开启窗口大小调节模式 */
    public static final String RESIZE_MODE = "resizeMode";
    
    /** 是否开启字母挖空模式 */
    public static final String FILL_BLANK_MODE = "fillBlankMode";
    
    /** 填空提示恢复间隔（秒） */
    public static final String FILL_BLANK_INTERVAL_SECONDS = "fillBlankIntervalSeconds";
    
    /** 填空时是否隐藏短语例句 */
    public static final String FILL_BLANK_HIDE_PHRASES = "fillBlankHidePhrases";
    
    /** 填空时是否显示中文释义 */
    public static final String FILL_BLANK_SHOW_TRANSLATION = "fillBlankShowTranslation";

    /**
     * 私有构造函数，无需实例化。
     */
    private SettingsKeys() {
        // 无需实例化
    }
}

package me.englishhugging.core.settings;

import lombok.Getter;
import lombok.Setter;

/**
 * 跨平台应用程序的内存配置实体类。
 *
 * <p>此实体类持有了运行该程序所需的所有运行时设置（例如窗口位置、单词播放规则等）。
 * 它不关心这些设置保存在哪里，只负责在内存中暂存这些值并提供参数校验。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 从持久化引擎中反序列化得到对象
 * AppSettings settings = SettingsMapper.load(storage);
 * 
 * // 获取用户偏好的字体大小
 * int currentSize = settings.getWordFontSize();
 * 
 * // 更新并应用边界校验
 * settings.setWordFontSize(currentSize + 2);
 * </code></pre>
 *
 * <p>没有额外逻辑的 getter/setter 由 Lombok 生成；需要空值处理、格式校验或
 * 数值范围限制的 setter 仍在本类中显式实现。</p>
 */
@Getter
@Setter
public final class AppSettings {
    
    /** 默认的词库文件相对路径 */
    public static final String DEFAULT_VOCABULARY_PATH = "vocabulary/1-初中-顺序.json";
    
    /** 默认的词库文件名 */
    public static final String DEFAULT_VOCABULARY_FILE_NAME = "1-初中-顺序.json";

    // --- 词库源配置；字段注释会由 Lombok 复制到生成的访问器 ---
    /** 词库的绝对或相对路径。 */
    private String vocabularyPath = DEFAULT_VOCABULARY_PATH;
    /** 当前选中词库的文件名称，用于界面展示。 */
    private String vocabularyFileName = DEFAULT_VOCABULARY_FILE_NAME;
    
    // --- 显示与交互模式 ---
    /** 单词、释义和短语的显示组合。 */
    private DisplayMode displayMode = DisplayMode.WORD_WITH_TRANSLATION_AND_PHRASE;
    /** 悬浮窗可拖拽或点击穿透的交互模式。 */
    private OverlayMode overlayMode = OverlayMode.CLICK_THROUGH;
    
    // --- 播放控制逻辑 ---
    /** 词条顺序、随机或随机不重复的抽取规则。 */
    private PlaybackMode playbackMode = PlaybackMode.RANDOM;
    /** 每个单词在屏幕上的驻留秒数，写入时最低限制为 2 秒。 */
    private int intervalSeconds = 8;
    /** 仅播放指定前缀的过滤条件，写入时会去除空白并转为小写。 */
    private String startingPrefix = "";
    /** 播放完整个词库后是否从头继续。 */
    private boolean loopPlayback = true;
    
    // --- 播放进度缓存 ---
    /** 当前词库在各播放模式下的进度快照。 */
    private PlaybackProgress playbackProgress = PlaybackProgress.EMPTY;
    
    // --- 悬浮窗位置与大小 (仅 Desktop 适用) ---
    /** 悬浮窗左上角 X 坐标。 */
    private double x = 80;
    /** 悬浮窗左上角 Y 坐标。 */
    private double y = 80;
    /** 悬浮窗宽度，写入正数时最低限制为 260 像素。 */
    private double width = 620;
    /** 悬浮窗高度，写入正数时最低限制为 80 像素。 */
    private double height = 150;
    /** 桌面端当前是否处于调整大小模式。 */
    private boolean resizeMode = false;
    
    // --- UI 外观与排版 ---
    /** 悬浮窗不透明度，写入时限制在 0.2 到 1.0。 */
    private double opacity = 0.85;
    /** 单词本体的十六进制字体颜色。 */
    private String wordColor = "#FFFFFF";
    /** 词性标识的十六进制字体颜色。 */
    private String typeColor = "#7DD3FC";
    /** 中文释义的十六进制字体颜色。 */
    private String translationColor = "#FDE68A";
    /** 英文短语的十六进制字体颜色。 */
    private String phraseColor = "#86EFAC";
    /** 单词主体字号，写入时限制在 16 到 72。 */
    private int wordFontSize = 30;
    /** 释义、词性等详细文字字号，写入时限制在 12 到 60。 */
    private int detailFontSize = 24;
    
    // --- 填空考核模式 ---
    /** 是否开启随机隐藏字母的挖空模式。 */
    private boolean fillBlankMode = false;
    /** 挖空模式逐个恢复字母的间隔秒数，最低为 1 秒。 */
    private int fillBlankIntervalSeconds = 3;
    /** 挖空时是否隐藏短语，避免泄露答案。 */
    private boolean fillBlankHidePhrases = true;
    /** 挖空时是否继续显示中文释义。 */
    private boolean fillBlankShowTranslation = true;

    // --- 需要业务校验的 Setter；其余访问器由 Lombok 生成 ---

    /**
     * 设置单词展示间隔。最低被限制为 2 秒。
     */
    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = Math.max(2, intervalSeconds);
    }

    /**
     * 整体替换播放进度。传入 null 视为清零；数值归一化由 {@link PlaybackProgress} 自身负责。
     */
    public void setPlaybackProgress(PlaybackProgress playbackProgress) {
        if (playbackProgress == null) {
            this.playbackProgress = PlaybackProgress.EMPTY;
        } else {
            this.playbackProgress = playbackProgress;
        }
    }

    /**
     * 清空所有类型的播放进度统计，使其从头开始。
     */
    public void resetPlaybackProgress() {
        this.playbackProgress = PlaybackProgress.EMPTY;
    }

    /**
     * 设置悬浮窗的宽度。
     * 当等于 0 时可能意味着折叠；大于 0 时，最小会被钳制在 260 像素以保证内容不溢出。
     */
    public void setWidth(double width) {
        if (width <= 0) {
            this.width = 0;
        } else {
            this.width = Math.max(260, width);
        }
    }

    /**
     * 设置悬浮窗的高度。最小高度为 80 像素。
     */
    public void setHeight(double height) {
        if (height <= 0) {
            this.height = 0;
        } else {
            this.height = Math.max(80, height);
        }
    }

    /**
     * 设置背景不透明度。范围会被严格钳制在 0.2 到 1.0 之间。
     */
    public void setOpacity(double opacity) {
        if (opacity < 0.2) {
            this.opacity = 0.2;
        } else {
            this.opacity = Math.min(opacity, 1.0);
        }
    }

    /**
     * 设置并校验单词本体的字体颜色。如果传入非法格式将保留原值。
     */
    public void setWordColor(String wordColor) {
        this.wordColor = validColorOrCurrent(wordColor, this.wordColor);
    }

    /** 设置并校验词性标识的字体颜色。 */
    public void setTypeColor(String typeColor) {
        this.typeColor = validColorOrCurrent(typeColor, this.typeColor);
    }

    /** 设置并校验中文释义的字体颜色。 */
    public void setTranslationColor(String translationColor) {
        this.translationColor = validColorOrCurrent(translationColor, this.translationColor);
    }

    /** 设置并校验英文短语的字体颜色。 */
    public void setPhraseColor(String phraseColor) {
        this.phraseColor = validColorOrCurrent(phraseColor, this.phraseColor);
    }

    /**
     * 设置大号文字字号。限制范围 16-72。
     */
    public void setWordFontSize(int wordFontSize) {
        this.wordFontSize = clamp(wordFontSize, 16, 72);
    }

    /**
     * 设置小号文字字号。限制范围 12-60。
     */
    public void setDetailFontSize(int detailFontSize) {
        this.detailFontSize = clamp(detailFontSize, 12, 60);
    }

    /** 设置过滤前缀，将自动转为小写。 */
    public void setStartingPrefix(String startingPrefix) {
        if (startingPrefix == null) {
            this.startingPrefix = "";
        } else {
            this.startingPrefix = startingPrefix.trim().toLowerCase();
        }
    }

    /** 设置填空模式逐字提示间隔，最低为 1 秒。 */
    public void setFillBlankIntervalSeconds(int fillBlankIntervalSeconds) {
        this.fillBlankIntervalSeconds = Math.max(1, fillBlankIntervalSeconds);
    }

    // --- 内部辅助校验工具 ---

    /**
     * 校验 Hex 颜色值的合法性，如果不合法则回退到旧值。
     *
     * @param value   试图传入的新颜色值（如 "#AABBCC"）
     * @param current 发生错误时的当前后备值
     * @return 合法的颜色字符串
     */
    private static String validColorOrCurrent(String value, String current) {
        if (value == null) {
            return current;
        }
        String trimmed = value.trim();
        if (trimmed.matches("#[0-9a-fA-F]{6}")) {
            return trimmed.toUpperCase();
        } else {
            return current;
        }
    }

    /**
     * 通用的数值边界钳制方法。
     *
     * @param value 原始值
     * @param min   允许的最小值
     * @param max   允许的最大值
     * @return 钳制后的安全值
     */
    private static int clamp(int value, int min, int max) {
        int clamped = Math.max(min, value);
        return Math.min(max, clamped);
    }
}

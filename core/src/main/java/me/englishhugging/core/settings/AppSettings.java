package me.englishhugging.core.settings;

import lombok.Getter;
import lombok.Setter;

/**
 * 跨平台应用设置。
 *
 * <p>保存桌面端与移动端通用的所有用户配置（词库选择、播放模式、字号颜色、填空设置等），
 * 并在 Setter 中对数值提供合理的范围限制。
 */
@Getter
@Setter
public final class AppSettings {
    
    /** 默认的词库文件相对路径 */
    public static final String DEFAULT_VOCABULARY_PATH = "vocabulary/1-初中-顺序.json";
    
    /** 默认的词库文件名 */
    public static final String DEFAULT_VOCABULARY_FILE_NAME = "1-初中-顺序.json";

    // --- 词库设置 ---
    /** 词库文件路径 */
    private String vocabularyPath = DEFAULT_VOCABULARY_PATH;
    /** 词库显示名称 */
    private String vocabularyFileName = DEFAULT_VOCABULARY_FILE_NAME;
    
    // --- 显示与交互模式 ---
    /** 单词显示模式（纯单词/释义/短语） */
    private DisplayMode displayMode = DisplayMode.WORD_WITH_TRANSLATION_AND_PHRASE;
    /** 悬浮窗交互模式（可拖拽/鼠标穿透） */
    private OverlayMode overlayMode = OverlayMode.CLICK_THROUGH;
    
    // --- 播放控制 ---
    /** 播放顺序（顺序/随机/乱序） */
    private PlaybackMode playbackMode = PlaybackMode.RANDOM;
    /** 单词切换间隔时间（秒，最少 2 秒） */
    private int intervalSeconds = 8;
    /** 首字母筛选前缀 */
    private String startingPrefix = "";
    /** 是否循环播放 */
    private boolean loopPlayback = true;
    
    // --- 播放进度 ---
    /** 当前词库的背诵进度 */
    private PlaybackProgress playbackProgress = PlaybackProgress.EMPTY;
    
    // --- 悬浮窗位置与尺寸（桌面端） ---
    /** 悬浮窗左上角 X 坐标（像素） */
    private double x = 80;
    /** 悬浮窗左上角 Y 坐标（像素） */
    private double y = 80;
    /** 悬浮窗宽度（像素） */
    private double width = 620;
    /** 悬浮窗高度（像素） */
    private double height = 150;
    /** 是否处于调节窗口大小模式 */
    private boolean resizeMode = false;
    
    // --- 样式与外观 ---
    /** 悬浮窗背景不透明度（0.2 ~ 1.0） */
    private double opacity = 0.85;
    /** 单词颜色（如 #FFFFFF） */
    private String wordColor = "#FFFFFF";
    /** 词性颜色（如 #7DD3FC） */
    private String typeColor = "#7DD3FC";
    /** 中文释义颜色（如 #FDE68A） */
    private String translationColor = "#FDE68A";
    /** 例句短语颜色（如 #86EFAC） */
    private String phraseColor = "#86EFAC";
    /** 单词字号（像素/sp，范围 16 ~ 72） */
    private int wordFontSize = 30;
    /** 释义与短语字号（像素/sp，范围 12 ~ 60） */
    private int detailFontSize = 24;
    
    // --- 填空模式 ---
    /** 是否开启字母挖空 */
    private boolean fillBlankMode = false;
    /** 填空提示恢复间隔（秒） */
    private int fillBlankIntervalSeconds = 3;
    /** 填空时是否隐藏短语 */
    private boolean fillBlankHidePhrases = true;
    /** 填空时是否显示中文释义 */
    private boolean fillBlankShowTranslation = true;

    // --- 需要业务校验的 Setter；其余 Getter/Setter 由 Lombok 自动生成 ---

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
     * 传 0 表示自适应内容尺寸；大于 0 时最小限制为 260 像素。
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
     * 设置背景不透明度，限制在 0.2 到 1.0 之间。
     */
    public void setOpacity(double opacity) {
        if (opacity < 0.2) {
            this.opacity = 0.2;
        } else {
            this.opacity = Math.min(opacity, 1.0);
        }
    }

    /**
     * 设置并校验单词文本的字体颜色。如果传入非法格式将保留原值。
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
     * 设置单词字号（像素/sp），范围限制在 16 到 72 之间。
     */
    public void setWordFontSize(int wordFontSize) {
        this.wordFontSize = clamp(wordFontSize, 16, 72);
    }

    /**
     * 设置释义与短语字号（像素/sp），范围限制在 12 到 60 之间。
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
     * 校验十六进制颜色值格式，格式错误时保留原值。
     *
     * @param value   待校验的新颜色值（如 "#AABBCC"）
     * @param current 格式错误时保留的原值
     * @return 合法的十六进制颜色字符串
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
     * 数值范围限制方法。
     *
     * @param value 原始值
     * @param min   允许的最小值
     * @param max   允许的最大值
     * @return 限制后的安全值
     */
    private static int clamp(int value, int min, int max) {
        int clamped = Math.max(min, value);
        return Math.min(max, clamped);
    }
}

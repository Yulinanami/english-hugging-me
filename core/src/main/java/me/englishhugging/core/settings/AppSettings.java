package me.englishhugging.core.settings;

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
 */
public final class AppSettings {
    
    /** 默认的词库文件相对路径 */
    public static final String DEFAULT_VOCABULARY_PATH = "vocabulary/1-初中-顺序.json";
    
    /** 默认的词库文件名 */
    public static final String DEFAULT_VOCABULARY_FILE_NAME = "1-初中-顺序.json";

    // --- 词库源配置 ---
    private String vocabularyPath = DEFAULT_VOCABULARY_PATH;
    private String vocabularyFileName = DEFAULT_VOCABULARY_FILE_NAME;
    
    // --- 显示与交互模式 ---
    private DisplayMode displayMode = DisplayMode.WORD_WITH_TRANSLATION_AND_PHRASE;
    private OverlayMode overlayMode = OverlayMode.CLICK_THROUGH;
    
    // --- 播放控制逻辑 ---
    private PlaybackMode playbackMode = PlaybackMode.RANDOM;
    private int intervalSeconds = 8;
    private String startingPrefix = "";
    private boolean loopPlayback = true;
    
    // --- 播放进度缓存 ---
    private int nextWordIndex = 0;
    private String shuffleOrder = "";
    private int shufflePosition = 0;
    private int randomPlayedCount = 0;
    
    // --- 悬浮窗位置与大小 (仅 Desktop 适用) ---
    private double x = 80;
    private double y = 80;
    private double width = 620;
    private double height = 150;
    private boolean resizeMode = false;
    
    // --- UI 外观与排版 ---
    private double opacity = 0.85;
    private String wordColor = "#FFFFFF";
    private String typeColor = "#7DD3FC";
    private String translationColor = "#FDE68A";
    private String phraseColor = "#86EFAC";
    private int wordFontSize = 30;
    private int detailFontSize = 24;
    
    // --- 填空考核模式 ---
    private boolean fillBlankMode = false;
    private int fillBlankIntervalSeconds = 3;
    private boolean fillBlankHidePhrases = true;
    private boolean fillBlankShowTranslation = true;

    // --- Getters and Setters ---

    /**
     * 获取词库的绝对或相对路径。
     */
    public String getVocabularyPath() {
        return this.vocabularyPath;
    }

    /**
     * 设置词库路径。
     */
    public void setVocabularyPath(String vocabularyPath) {
        this.vocabularyPath = vocabularyPath;
    }

    /**
     * 获取当前选中词库的文件名称，常用于展示给用户。
     */
    public String getVocabularyFileName() {
        return this.vocabularyFileName;
    }

    /**
     * 设置词库文件名称。
     */
    public void setVocabularyFileName(String vocabularyFileName) {
        this.vocabularyFileName = vocabularyFileName;
    }

    /**
     * 获取内容展示的丰富程度（例如是否包含例句）。
     */
    public DisplayMode getDisplayMode() {
        return this.displayMode;
    }

    /**
     * 设置内容展示模式。
     */
    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    /**
     * 获取悬浮窗是否可拖拽或鼠标穿透的交互模式。
     */
    public OverlayMode getOverlayMode() {
        return this.overlayMode;
    }

    /**
     * 设置悬浮窗交互模式。
     */
    public void setOverlayMode(OverlayMode overlayMode) {
        this.overlayMode = overlayMode;
    }

    /**
     * 获取词条抽取规则（如顺序、随机）。
     */
    public PlaybackMode getPlaybackMode() {
        return this.playbackMode;
    }

    /**
     * 设置词条抽取规则。
     */
    public void setPlaybackMode(PlaybackMode playbackMode) {
        this.playbackMode = playbackMode;
    }

    /**
     * 获取每个单词在屏幕上驻留展示的时间（秒）。
     */
    public int getIntervalSeconds() {
        return this.intervalSeconds;
    }

    /**
     * 设置单词展示间隔。最低被限制为 2 秒。
     */
    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = Math.max(2, intervalSeconds);
    }

    /**
     * 获取顺序模式下，下一个被播放的单词的数组下标。
     */
    public int getNextWordIndex() {
        return this.nextWordIndex;
    }

    /**
     * 设置下一个顺序播放的单词下标，不能小于 0。
     */
    public void setNextWordIndex(int nextWordIndex) {
        this.nextWordIndex = Math.max(0, nextWordIndex);
    }

    /**
     * 获取乱序播放模式下的伪随机序列字符串（通常由逗号分隔的数字组成）。
     */
    public String getShuffleOrder() {
        return this.shuffleOrder;
    }

    /**
     * 设置乱序播放序列。
     */
    public void setShuffleOrder(String shuffleOrder) {
        if (shuffleOrder == null) {
            this.shuffleOrder = "";
        } else {
            this.shuffleOrder = shuffleOrder.trim();
        }
    }

    /**
     * 获取乱序模式下当前正在消费的索引位置。
     */
    public int getShufflePosition() {
        return this.shufflePosition;
    }

    /**
     * 设置乱序模式的位置。
     */
    public void setShufflePosition(int shufflePosition) {
        this.shufflePosition = Math.max(0, shufflePosition);
    }

    /**
     * 获取完全随机模式下总共已经播放过的单词数量。
     */
    public int getRandomPlayedCount() {
        return this.randomPlayedCount;
    }

    /**
     * 设置完全随机模式下已经播放的数量。
     */
    public void setRandomPlayedCount(int randomPlayedCount) {
        this.randomPlayedCount = Math.max(0, randomPlayedCount);
    }

    /**
     * 清空所有类型的播放进度统计，使其从头开始。
     */
    public void resetPlaybackProgress() {
        this.nextWordIndex = 0;
        this.shuffleOrder = "";
        this.shufflePosition = 0;
        this.randomPlayedCount = 0;
    }

    /** 获取悬浮窗的 X 坐标 */
    public double getX() {
        return this.x;
    }

    /** 设置悬浮窗的 X 坐标 */
    public void setX(double x) {
        this.x = x;
    }

    /** 获取悬浮窗的 Y 坐标 */
    public double getY() {
        return this.y;
    }

    /** 设置悬浮窗的 Y 坐标 */
    public void setY(double y) {
        this.y = y;
    }

    /** 获取悬浮窗的宽度 */
    public double getWidth() {
        return this.width;
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

    /** 获取悬浮窗的高度 */
    public double getHeight() {
        return this.height;
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
     * 获取悬浮窗背景的不透明度。
     */
    public double getOpacity() {
        return this.opacity;
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
     * 获取单词本体的字体颜色（Hex）。
     */
    public String getWordColor() {
        return this.wordColor;
    }

    /**
     * 设置并校验单词本体的字体颜色。如果传入非法格式将保留原值。
     */
    public void setWordColor(String wordColor) {
        this.wordColor = validColorOrCurrent(wordColor, this.wordColor);
    }

    /** 获取词性标识的字体颜色（Hex） */
    public String getTypeColor() {
        return this.typeColor;
    }

    public void setTypeColor(String typeColor) {
        this.typeColor = validColorOrCurrent(typeColor, this.typeColor);
    }

    /** 获取中文释义的字体颜色（Hex） */
    public String getTranslationColor() {
        return this.translationColor;
    }

    public void setTranslationColor(String translationColor) {
        this.translationColor = validColorOrCurrent(translationColor, this.translationColor);
    }

    /** 获取英文例句短语的字体颜色（Hex） */
    public String getPhraseColor() {
        return this.phraseColor;
    }

    public void setPhraseColor(String phraseColor) {
        this.phraseColor = validColorOrCurrent(phraseColor, this.phraseColor);
    }

    /**
     * 获取大号文字（单词主体）的字号。
     */
    public int getWordFontSize() {
        return this.wordFontSize;
    }

    /**
     * 设置大号文字字号。限制范围 16-72。
     */
    public void setWordFontSize(int wordFontSize) {
        this.wordFontSize = clamp(wordFontSize, 16, 72);
    }

    /**
     * 获取小号文字（释义、词性等）的字号。
     */
    public int getDetailFontSize() {
        return this.detailFontSize;
    }

    /**
     * 设置小号文字字号。限制范围 12-60。
     */
    public void setDetailFontSize(int detailFontSize) {
        this.detailFontSize = clamp(detailFontSize, 12, 60);
    }

    /** 获取字母过滤前缀。仅播放以该字符开始的词汇。 */
    public String getStartingPrefix() {
        return this.startingPrefix;
    }

    /** 设置过滤前缀，将自动转为小写。 */
    public void setStartingPrefix(String startingPrefix) {
        if (startingPrefix == null) {
            this.startingPrefix = "";
        } else {
            this.startingPrefix = startingPrefix.trim().toLowerCase();
        }
    }

    /** 是否允许循环播放整个词库。 */
    public boolean isLoopPlayback() {
        return this.loopPlayback;
    }

    public void setLoopPlayback(boolean loopPlayback) {
        this.loopPlayback = loopPlayback;
    }

    /** 获取桌面端当前是否处于调整大小模式。 */
    public boolean isResizeMode() {
        return this.resizeMode;
    }

    public void setResizeMode(boolean resizeMode) {
        this.resizeMode = resizeMode;
    }

    /** 获取当前是否开启了填空考核模式。 */
    public boolean isFillBlankMode() {
        return this.fillBlankMode;
    }

    public void setFillBlankMode(boolean fillBlankMode) {
        this.fillBlankMode = fillBlankMode;
    }

    /** 获取填空模式下逐个字母提示的时间间隔。 */
    public int getFillBlankIntervalSeconds() {
        return this.fillBlankIntervalSeconds;
    }

    public void setFillBlankIntervalSeconds(int fillBlankIntervalSeconds) {
        this.fillBlankIntervalSeconds = Math.max(1, fillBlankIntervalSeconds);
    }

    /** 填空时是否隐比例句以防止降低难度。 */
    public boolean isFillBlankHidePhrases() {
        return this.fillBlankHidePhrases;
    }

    public void setFillBlankHidePhrases(boolean fillBlankHidePhrases) {
        this.fillBlankHidePhrases = fillBlankHidePhrases;
    }

    /** 填空时是否依旧显示中文提示。 */
    public boolean isFillBlankShowTranslation() {
        return this.fillBlankShowTranslation;
    }

    public void setFillBlankShowTranslation(boolean fillBlankShowTranslation) {
        this.fillBlankShowTranslation = fillBlankShowTranslation;
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

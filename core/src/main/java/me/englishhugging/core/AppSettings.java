package me.englishhugging.core;

public final class AppSettings {
    public static final String DEFAULT_VOCABULARY_PATH = "vocabulary/1-初中-顺序.json";
    public static final String DEFAULT_VOCABULARY_FILE_NAME = "1-初中-顺序.json";

    private String vocabularyPath = DEFAULT_VOCABULARY_PATH;
    private String vocabularyFileName = DEFAULT_VOCABULARY_FILE_NAME;
    private DisplayMode displayMode = DisplayMode.WORD_WITH_TRANSLATION;
    private OverlayMode overlayMode = OverlayMode.DRAGGABLE;
    private PlaybackMode playbackMode = PlaybackMode.SHUFFLE_NO_REPEAT;
    private int intervalSeconds = 8;
    private int nextWordIndex = 0;
    private String shuffleOrder = "";
    private int shufflePosition = 0;
    private int randomPlayedCount = 0;
    private double x = 80;
    private double y = 80;
    private double width = 620;
    private double height = 150;
    private double opacity = 0.85;
    private String wordColor = "#FFFFFF";
    private String typeColor = "#7DD3FC";
    private String translationColor = "#FDE68A";
    private String phraseColor = "#86EFAC";
    private int wordFontSize = 30;
    private int detailFontSize = 24;

    public String getVocabularyPath() {
        return vocabularyPath;
    }

    public void setVocabularyPath(String vocabularyPath) {
        if (vocabularyPath != null && vocabularyPath.trim().length() > 0) {
            this.vocabularyPath = vocabularyPath.trim();
        }
    }

    public String getVocabularyFileName() {
        return vocabularyFileName;
    }

    public void setVocabularyFileName(String vocabularyFileName) {
        if (vocabularyFileName != null && vocabularyFileName.trim().length() > 0) {
            this.vocabularyFileName = vocabularyFileName.trim();
        }
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(DisplayMode displayMode) {
        if (displayMode != null) {
            this.displayMode = displayMode;
        }
    }

    public OverlayMode getOverlayMode() {
        return overlayMode;
    }

    public void setOverlayMode(OverlayMode overlayMode) {
        if (overlayMode != null) {
            this.overlayMode = overlayMode;
        }
    }

    public PlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public void setPlaybackMode(PlaybackMode playbackMode) {
        if (playbackMode != null) {
            this.playbackMode = playbackMode;
        }
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = Math.max(2, intervalSeconds);
    }

    public int getNextWordIndex() {
        return nextWordIndex;
    }

    public void setNextWordIndex(int nextWordIndex) {
        this.nextWordIndex = Math.max(0, nextWordIndex);
    }

    public String getShuffleOrder() {
        return shuffleOrder;
    }

    public void setShuffleOrder(String shuffleOrder) {
        this.shuffleOrder = shuffleOrder == null ? "" : shuffleOrder.trim();
    }

    public int getShufflePosition() {
        return shufflePosition;
    }

    public void setShufflePosition(int shufflePosition) {
        this.shufflePosition = Math.max(0, shufflePosition);
    }

    public int getRandomPlayedCount() {
        return randomPlayedCount;
    }

    public void setRandomPlayedCount(int randomPlayedCount) {
        this.randomPlayedCount = Math.max(0, randomPlayedCount);
    }

    public void resetPlaybackProgress() {
        nextWordIndex = 0;
        shuffleOrder = "";
        shufflePosition = 0;
        randomPlayedCount = 0;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = Math.max(260, width);
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = Math.max(80, height);
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        if (opacity < 0.2) {
            this.opacity = 0.2;
        } else if (opacity > 1.0) {
            this.opacity = 1.0;
        } else {
            this.opacity = opacity;
        }
    }

    public String getWordColor() {
        return wordColor;
    }

    public void setWordColor(String wordColor) {
        this.wordColor = validColorOrCurrent(wordColor, this.wordColor);
    }

    public String getTypeColor() {
        return typeColor;
    }

    public void setTypeColor(String typeColor) {
        this.typeColor = validColorOrCurrent(typeColor, this.typeColor);
    }

    public String getTranslationColor() {
        return translationColor;
    }

    public void setTranslationColor(String translationColor) {
        this.translationColor = validColorOrCurrent(translationColor, this.translationColor);
    }

    public String getPhraseColor() {
        return phraseColor;
    }

    public void setPhraseColor(String phraseColor) {
        this.phraseColor = validColorOrCurrent(phraseColor, this.phraseColor);
    }

    public int getWordFontSize() {
        return wordFontSize;
    }

    public void setWordFontSize(int wordFontSize) {
        this.wordFontSize = clamp(wordFontSize, 16, 72);
    }

    public int getDetailFontSize() {
        return detailFontSize;
    }

    public void setDetailFontSize(int detailFontSize) {
        this.detailFontSize = clamp(detailFontSize, 12, 60);
    }

    private static String validColorOrCurrent(String value, String current) {
        if (value == null) {
            return current;
        }
        String trimmed = value.trim();
        if (trimmed.matches("#[0-9a-fA-F]{6}")) {
            return trimmed.toUpperCase();
        }
        return current;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

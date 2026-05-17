package me.englishhugging.desktop.settings;

import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.DisplayMode;
import me.englishhugging.core.settings.OverlayMode;
import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.core.settings.SettingsKeys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class DesktopSettingsStore {
    private final Path configPath = Paths.get(
            System.getProperty("user.home"), ".english-hugging-me", "desktop.properties"
    );

    public AppSettings load() {
        AppSettings settings = new AppSettings();
        if (!Files.exists(configPath)) return settings;

        Properties p = readProperties();
        settings.setVocabularyPath(migrateVocabularyPath(p.getProperty(SettingsKeys.VOCABULARY_PATH)));
        settings.setDisplayMode(parseEnum(DisplayMode.class, p.getProperty(SettingsKeys.DISPLAY_MODE), settings.getDisplayMode()));
        settings.setOverlayMode(parseEnum(OverlayMode.class, p.getProperty(SettingsKeys.OVERLAY_MODE), settings.getOverlayMode()));
        settings.setPlaybackMode(parseEnum(PlaybackMode.class, p.getProperty(SettingsKeys.PLAYBACK_MODE), settings.getPlaybackMode()));
        settings.setIntervalSeconds(parseInt(p.getProperty(SettingsKeys.INTERVAL_SECONDS), settings.getIntervalSeconds()));
        settings.setNextWordIndex(parseInt(p.getProperty(SettingsKeys.NEXT_WORD_INDEX), settings.getNextWordIndex()));
        settings.setShuffleOrder(p.getProperty(SettingsKeys.SHUFFLE_ORDER));
        settings.setShufflePosition(parseInt(p.getProperty(SettingsKeys.SHUFFLE_POSITION), settings.getShufflePosition()));
        settings.setRandomPlayedCount(parseInt(p.getProperty(SettingsKeys.RANDOM_PLAYED_COUNT), settings.getRandomPlayedCount()));
        settings.setX(parseDouble(p.getProperty(SettingsKeys.X), settings.getX()));
        settings.setY(parseDouble(p.getProperty(SettingsKeys.Y), settings.getY()));
        settings.setWidth(parseDouble(p.getProperty(SettingsKeys.WIDTH), settings.getWidth()));
        settings.setHeight(parseDouble(p.getProperty(SettingsKeys.HEIGHT), settings.getHeight()));
        settings.setOpacity(parseDouble(p.getProperty(SettingsKeys.OPACITY), settings.getOpacity()));
        settings.setWordColor(p.getProperty(SettingsKeys.WORD_COLOR));
        settings.setTypeColor(p.getProperty(SettingsKeys.TYPE_COLOR));
        settings.setTranslationColor(p.getProperty(SettingsKeys.TRANSLATION_COLOR));
        settings.setPhraseColor(p.getProperty(SettingsKeys.PHRASE_COLOR));
        settings.setWordFontSize(parseInt(p.getProperty(SettingsKeys.WORD_FONT_SIZE), settings.getWordFontSize()));
        settings.setDetailFontSize(parseInt(p.getProperty(SettingsKeys.DETAIL_FONT_SIZE), settings.getDetailFontSize()));
        return settings;
    }

    public void save(AppSettings s) {
        Properties p = readProperties();
        p.setProperty(SettingsKeys.VOCABULARY_PATH, s.getVocabularyPath());
        p.setProperty(SettingsKeys.DISPLAY_MODE, s.getDisplayMode().name());
        p.setProperty(SettingsKeys.OVERLAY_MODE, s.getOverlayMode().name());
        p.setProperty(SettingsKeys.PLAYBACK_MODE, s.getPlaybackMode().name());
        p.setProperty(SettingsKeys.INTERVAL_SECONDS, Integer.toString(s.getIntervalSeconds()));
        p.setProperty(SettingsKeys.NEXT_WORD_INDEX, Integer.toString(s.getNextWordIndex()));
        p.setProperty(SettingsKeys.SHUFFLE_ORDER, s.getShuffleOrder());
        p.setProperty(SettingsKeys.SHUFFLE_POSITION, Integer.toString(s.getShufflePosition()));
        p.setProperty(SettingsKeys.RANDOM_PLAYED_COUNT, Integer.toString(s.getRandomPlayedCount()));
        p.setProperty(SettingsKeys.X, Double.toString(s.getX()));
        p.setProperty(SettingsKeys.Y, Double.toString(s.getY()));
        p.setProperty(SettingsKeys.WIDTH, Double.toString(s.getWidth()));
        p.setProperty(SettingsKeys.HEIGHT, Double.toString(s.getHeight()));
        p.setProperty(SettingsKeys.OPACITY, Double.toString(s.getOpacity()));
        p.setProperty(SettingsKeys.WORD_COLOR, s.getWordColor());
        p.setProperty(SettingsKeys.TYPE_COLOR, s.getTypeColor());
        p.setProperty(SettingsKeys.TRANSLATION_COLOR, s.getTranslationColor());
        p.setProperty(SettingsKeys.PHRASE_COLOR, s.getPhraseColor());
        p.setProperty(SettingsKeys.WORD_FONT_SIZE, Integer.toString(s.getWordFontSize()));
        p.setProperty(SettingsKeys.DETAIL_FONT_SIZE, Integer.toString(s.getDetailFontSize()));
        writeProperties(p);
    }

    public void loadPlaybackProgress(AppSettings s, String vocabularyKey) {
        Properties p = readProperties();
        s.setNextWordIndex(parseInt(p.getProperty(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX)), s.getNextWordIndex()));
        s.setShuffleOrder(p.getProperty(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_ORDER), s.getShuffleOrder()));
        s.setShufflePosition(parseInt(p.getProperty(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION)), s.getShufflePosition()));
        s.setRandomPlayedCount(parseInt(p.getProperty(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT)), s.getRandomPlayedCount()));
    }

    public void savePlaybackProgress(AppSettings s, String vocabularyKey) {
        Properties p = readProperties();
        p.setProperty(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX), Integer.toString(s.getNextWordIndex()));
        p.setProperty(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_ORDER), s.getShuffleOrder());
        p.setProperty(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION), Integer.toString(s.getShufflePosition()));
        p.setProperty(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT), Integer.toString(s.getRandomPlayedCount()));
        writeProperties(p);
    }

    public String playbackRecordLine(String vocabularyKey, String label) {
        Properties p = readProperties();
        int nextWordIndex = parseInt(p.getProperty(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX)), 0);
        int shufflePosition = parseInt(p.getProperty(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION)), 0);
        int randomPlayedCount = parseInt(p.getProperty(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT)), 0);
        return label + "：顺序播放到第 " + (nextWordIndex + 1) + " 个；随机播放 " + randomPlayedCount + " 个；随机不重复 " + shufflePosition + " 个";
    }

    private Properties readProperties() {
        Properties p = new Properties();
        if (!Files.exists(configPath)) return p;
        try (InputStream in = Files.newInputStream(configPath)) { p.load(in); } catch (IOException ignored) {}
        return p;
    }

    private void writeProperties(Properties p) {
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream out = Files.newOutputStream(configPath)) { p.store(out, "English Hugging Me desktop settings"); }
        } catch (IOException ignored) {}
    }

    private static String progressKey(String vocabularyKey, String key) {
        String safe = vocabularyKey == null ? "" : vocabularyKey.replace('\\', '/');
        return "progress." + safe + "." + key;
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (RuntimeException ignored) { return fallback; }
    }

    private static double parseDouble(String value, double fallback) {
        try { return Double.parseDouble(value); } catch (RuntimeException ignored) { return fallback; }
    }

    private static String migrateVocabularyPath(String value) {
        if (value == null) return null;
        return value.replace("english-vocabulary/json/", "vocabulary/").replace("english-vocabulary\\json\\", "vocabulary\\");
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        try { return Enum.valueOf(type, value); } catch (RuntimeException ignored) { return fallback; }
    }
}

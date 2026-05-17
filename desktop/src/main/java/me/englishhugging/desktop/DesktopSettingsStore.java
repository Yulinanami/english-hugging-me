package me.englishhugging.desktop;

import me.englishhugging.core.AppSettings;
import me.englishhugging.core.DisplayMode;
import me.englishhugging.core.OverlayMode;
import me.englishhugging.core.PlaybackMode;
import me.englishhugging.core.SettingsKeys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

final class DesktopSettingsStore {
    private static final String KEY_VOCABULARY_PATH = SettingsKeys.VOCABULARY_PATH;
    private static final String KEY_DISPLAY_MODE = SettingsKeys.DISPLAY_MODE;
    private static final String KEY_OVERLAY_MODE = SettingsKeys.OVERLAY_MODE;
    private static final String KEY_PLAYBACK_MODE = SettingsKeys.PLAYBACK_MODE;
    private static final String KEY_INTERVAL_SECONDS = SettingsKeys.INTERVAL_SECONDS;
    private static final String KEY_NEXT_WORD_INDEX = SettingsKeys.NEXT_WORD_INDEX;
    private static final String KEY_SHUFFLE_ORDER = SettingsKeys.SHUFFLE_ORDER;
    private static final String KEY_SHUFFLE_POSITION = SettingsKeys.SHUFFLE_POSITION;
    private static final String KEY_RANDOM_PLAYED_COUNT = SettingsKeys.RANDOM_PLAYED_COUNT;
    private static final String KEY_X = SettingsKeys.X;
    private static final String KEY_Y = SettingsKeys.Y;
    private static final String KEY_WIDTH = SettingsKeys.WIDTH;
    private static final String KEY_HEIGHT = SettingsKeys.HEIGHT;
    private static final String KEY_OPACITY = SettingsKeys.OPACITY;
    private static final String KEY_WORD_COLOR = SettingsKeys.WORD_COLOR;
    private static final String KEY_TYPE_COLOR = SettingsKeys.TYPE_COLOR;
    private static final String KEY_TRANSLATION_COLOR = SettingsKeys.TRANSLATION_COLOR;
    private static final String KEY_PHRASE_COLOR = SettingsKeys.PHRASE_COLOR;
    private static final String KEY_WORD_FONT_SIZE = SettingsKeys.WORD_FONT_SIZE;
    private static final String KEY_DETAIL_FONT_SIZE = SettingsKeys.DETAIL_FONT_SIZE;

    private final Path configPath = Paths.get(
            System.getProperty("user.home"),
            ".english-hugging-me",
            "desktop.properties"
    );

    AppSettings load() {
        AppSettings settings = new AppSettings();
        if (!Files.exists(configPath)) {
            return settings;
        }

        Properties properties = readProperties();

        settings.setVocabularyPath(migrateVocabularyPath(properties.getProperty(KEY_VOCABULARY_PATH)));
        settings.setDisplayMode(parseEnum(DisplayMode.class, properties.getProperty(KEY_DISPLAY_MODE), settings.getDisplayMode()));
        settings.setOverlayMode(parseEnum(OverlayMode.class, properties.getProperty(KEY_OVERLAY_MODE), settings.getOverlayMode()));
        settings.setPlaybackMode(parseEnum(PlaybackMode.class, properties.getProperty(KEY_PLAYBACK_MODE), settings.getPlaybackMode()));
        settings.setIntervalSeconds(parseInt(properties.getProperty(KEY_INTERVAL_SECONDS), settings.getIntervalSeconds()));
        settings.setNextWordIndex(parseInt(properties.getProperty(KEY_NEXT_WORD_INDEX), settings.getNextWordIndex()));
        settings.setShuffleOrder(properties.getProperty(KEY_SHUFFLE_ORDER));
        settings.setShufflePosition(parseInt(properties.getProperty(KEY_SHUFFLE_POSITION), settings.getShufflePosition()));
        settings.setRandomPlayedCount(parseInt(properties.getProperty(KEY_RANDOM_PLAYED_COUNT), settings.getRandomPlayedCount()));
        settings.setX(parseDouble(properties.getProperty(KEY_X), settings.getX()));
        settings.setY(parseDouble(properties.getProperty(KEY_Y), settings.getY()));
        settings.setWidth(parseDouble(properties.getProperty(KEY_WIDTH), settings.getWidth()));
        settings.setHeight(parseDouble(properties.getProperty(KEY_HEIGHT), settings.getHeight()));
        settings.setOpacity(parseDouble(properties.getProperty(KEY_OPACITY), settings.getOpacity()));
        settings.setWordColor(properties.getProperty(KEY_WORD_COLOR));
        settings.setTypeColor(properties.getProperty(KEY_TYPE_COLOR));
        settings.setTranslationColor(properties.getProperty(KEY_TRANSLATION_COLOR));
        settings.setPhraseColor(properties.getProperty(KEY_PHRASE_COLOR));
        settings.setWordFontSize(parseInt(properties.getProperty(KEY_WORD_FONT_SIZE), settings.getWordFontSize()));
        settings.setDetailFontSize(parseInt(properties.getProperty(KEY_DETAIL_FONT_SIZE), settings.getDetailFontSize()));
        return settings;
    }

    void save(AppSettings settings) {
        Properties properties = readProperties();
        properties.setProperty(KEY_VOCABULARY_PATH, settings.getVocabularyPath());
        properties.setProperty(KEY_DISPLAY_MODE, settings.getDisplayMode().name());
        properties.setProperty(KEY_OVERLAY_MODE, settings.getOverlayMode().name());
        properties.setProperty(KEY_PLAYBACK_MODE, settings.getPlaybackMode().name());
        properties.setProperty(KEY_INTERVAL_SECONDS, Integer.toString(settings.getIntervalSeconds()));
        properties.setProperty(KEY_NEXT_WORD_INDEX, Integer.toString(settings.getNextWordIndex()));
        properties.setProperty(KEY_SHUFFLE_ORDER, settings.getShuffleOrder());
        properties.setProperty(KEY_SHUFFLE_POSITION, Integer.toString(settings.getShufflePosition()));
        properties.setProperty(KEY_RANDOM_PLAYED_COUNT, Integer.toString(settings.getRandomPlayedCount()));
        properties.setProperty(KEY_X, Double.toString(settings.getX()));
        properties.setProperty(KEY_Y, Double.toString(settings.getY()));
        properties.setProperty(KEY_WIDTH, Double.toString(settings.getWidth()));
        properties.setProperty(KEY_HEIGHT, Double.toString(settings.getHeight()));
        properties.setProperty(KEY_OPACITY, Double.toString(settings.getOpacity()));
        properties.setProperty(KEY_WORD_COLOR, settings.getWordColor());
        properties.setProperty(KEY_TYPE_COLOR, settings.getTypeColor());
        properties.setProperty(KEY_TRANSLATION_COLOR, settings.getTranslationColor());
        properties.setProperty(KEY_PHRASE_COLOR, settings.getPhraseColor());
        properties.setProperty(KEY_WORD_FONT_SIZE, Integer.toString(settings.getWordFontSize()));
        properties.setProperty(KEY_DETAIL_FONT_SIZE, Integer.toString(settings.getDetailFontSize()));

        writeProperties(properties);
    }

    void loadPlaybackProgress(AppSettings settings, String vocabularyKey) {
        Properties properties = readProperties();
        settings.setNextWordIndex(parseInt(properties.getProperty(progressKey(vocabularyKey, KEY_NEXT_WORD_INDEX)), settings.getNextWordIndex()));
        settings.setShuffleOrder(properties.getProperty(progressKey(vocabularyKey, KEY_SHUFFLE_ORDER), settings.getShuffleOrder()));
        settings.setShufflePosition(parseInt(properties.getProperty(progressKey(vocabularyKey, KEY_SHUFFLE_POSITION)), settings.getShufflePosition()));
        settings.setRandomPlayedCount(parseInt(properties.getProperty(progressKey(vocabularyKey, KEY_RANDOM_PLAYED_COUNT)), settings.getRandomPlayedCount()));
    }

    void savePlaybackProgress(AppSettings settings, String vocabularyKey) {
        Properties properties = readProperties();
        properties.setProperty(progressKey(vocabularyKey, KEY_NEXT_WORD_INDEX), Integer.toString(settings.getNextWordIndex()));
        properties.setProperty(progressKey(vocabularyKey, KEY_SHUFFLE_ORDER), settings.getShuffleOrder());
        properties.setProperty(progressKey(vocabularyKey, KEY_SHUFFLE_POSITION), Integer.toString(settings.getShufflePosition()));
        properties.setProperty(progressKey(vocabularyKey, KEY_RANDOM_PLAYED_COUNT), Integer.toString(settings.getRandomPlayedCount()));
        writeProperties(properties);
    }

    String playbackRecordLine(String vocabularyKey, String label) {
        Properties properties = readProperties();
        int nextWordIndex = parseInt(properties.getProperty(progressKey(vocabularyKey, KEY_NEXT_WORD_INDEX)), 0);
        int shufflePosition = parseInt(properties.getProperty(progressKey(vocabularyKey, KEY_SHUFFLE_POSITION)), 0);
        int randomPlayedCount = parseInt(properties.getProperty(progressKey(vocabularyKey, KEY_RANDOM_PLAYED_COUNT)), 0);
        return label + "：顺序播放到第 " + (nextWordIndex + 1) + " 个；随机播放 " + randomPlayedCount + " 个；随机不重复 " + shufflePosition + " 个";
    }

    private Properties readProperties() {
        Properties properties = new Properties();
        if (!Files.exists(configPath)) {
            return properties;
        }
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException ignored) {
        }
        return properties;
    }

    private void writeProperties(Properties properties) {
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(configPath)) {
                properties.store(outputStream, "English Hugging Me desktop settings");
            }
        } catch (IOException ignored) {
        }
    }

    private static String progressKey(String vocabularyKey, String key) {
        String safeVocabularyKey = vocabularyKey == null ? "" : vocabularyKey.replace('\\', '/');
        return "progress." + safeVocabularyKey + "." + key;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String migrateVocabularyPath(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("english-vocabulary/json/", "vocabulary/")
                .replace("english-vocabulary\\json\\", "vocabulary\\");
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}

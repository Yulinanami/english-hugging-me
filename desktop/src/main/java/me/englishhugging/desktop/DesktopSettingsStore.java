package me.englishhugging.desktop;

import me.englishhugging.core.AppSettings;
import me.englishhugging.core.DisplayMode;
import me.englishhugging.core.OverlayMode;
import me.englishhugging.core.PlaybackMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

final class DesktopSettingsStore {
    private static final String KEY_VOCABULARY_PATH = "vocabularyPath";
    private static final String KEY_DISPLAY_MODE = "displayMode";
    private static final String KEY_OVERLAY_MODE = "overlayMode";
    private static final String KEY_PLAYBACK_MODE = "playbackMode";
    private static final String KEY_INTERVAL_SECONDS = "intervalSeconds";
    private static final String KEY_NEXT_WORD_INDEX = "nextWordIndex";
    private static final String KEY_SHUFFLE_ORDER = "shuffleOrder";
    private static final String KEY_SHUFFLE_POSITION = "shufflePosition";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_OPACITY = "opacity";
    private static final String KEY_WORD_COLOR = "wordColor";
    private static final String KEY_TYPE_COLOR = "typeColor";
    private static final String KEY_TRANSLATION_COLOR = "translationColor";
    private static final String KEY_PHRASE_COLOR = "phraseColor";
    private static final String KEY_WORD_FONT_SIZE = "wordFontSize";
    private static final String KEY_DETAIL_FONT_SIZE = "detailFontSize";

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

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException ignored) {
            return settings;
        }

        settings.setVocabularyPath(migrateVocabularyPath(properties.getProperty(KEY_VOCABULARY_PATH)));
        settings.setDisplayMode(parseEnum(DisplayMode.class, properties.getProperty(KEY_DISPLAY_MODE), settings.getDisplayMode()));
        settings.setOverlayMode(parseEnum(OverlayMode.class, properties.getProperty(KEY_OVERLAY_MODE), settings.getOverlayMode()));
        settings.setPlaybackMode(parseEnum(PlaybackMode.class, properties.getProperty(KEY_PLAYBACK_MODE), settings.getPlaybackMode()));
        settings.setIntervalSeconds(parseInt(properties.getProperty(KEY_INTERVAL_SECONDS), settings.getIntervalSeconds()));
        settings.setNextWordIndex(parseInt(properties.getProperty(KEY_NEXT_WORD_INDEX), settings.getNextWordIndex()));
        settings.setShuffleOrder(properties.getProperty(KEY_SHUFFLE_ORDER));
        settings.setShufflePosition(parseInt(properties.getProperty(KEY_SHUFFLE_POSITION), settings.getShufflePosition()));
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
        Properties properties = new Properties();
        properties.setProperty(KEY_VOCABULARY_PATH, settings.getVocabularyPath());
        properties.setProperty(KEY_DISPLAY_MODE, settings.getDisplayMode().name());
        properties.setProperty(KEY_OVERLAY_MODE, settings.getOverlayMode().name());
        properties.setProperty(KEY_PLAYBACK_MODE, settings.getPlaybackMode().name());
        properties.setProperty(KEY_INTERVAL_SECONDS, Integer.toString(settings.getIntervalSeconds()));
        properties.setProperty(KEY_NEXT_WORD_INDEX, Integer.toString(settings.getNextWordIndex()));
        properties.setProperty(KEY_SHUFFLE_ORDER, settings.getShuffleOrder());
        properties.setProperty(KEY_SHUFFLE_POSITION, Integer.toString(settings.getShufflePosition()));
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

        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(configPath)) {
                properties.store(outputStream, "English Hugging Me desktop settings");
            }
        } catch (IOException ignored) {
        }
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

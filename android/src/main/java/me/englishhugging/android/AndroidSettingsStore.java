package me.englishhugging.android;

import android.content.Context;
import android.content.SharedPreferences;

import me.englishhugging.core.AppSettings;
import me.englishhugging.core.DisplayMode;
import me.englishhugging.core.OverlayMode;
import me.englishhugging.core.PlaybackMode;
import me.englishhugging.core.SettingsKeys;
import me.englishhugging.core.VocabularyCatalog;

final class AndroidSettingsStore {
    static final String[] VOCABULARY_FILES = VocabularyCatalog.fileNames();

    private static final String PREFS = "english_hugging_settings";
    private static final String KEY_VOCABULARY_FILE_NAME = SettingsKeys.VOCABULARY_FILE_NAME;
    private static final String KEY_DISPLAY_MODE = SettingsKeys.DISPLAY_MODE;
    private static final String KEY_OVERLAY_MODE = SettingsKeys.OVERLAY_MODE;
    private static final String KEY_PLAYBACK_MODE = SettingsKeys.PLAYBACK_MODE;
    private static final String KEY_INTERVAL_SECONDS = SettingsKeys.INTERVAL_SECONDS;
    private static final String KEY_NEXT_WORD_INDEX = SettingsKeys.NEXT_WORD_INDEX;
    private static final String KEY_SHUFFLE_ORDER = SettingsKeys.SHUFFLE_ORDER;
    private static final String KEY_SHUFFLE_POSITION = SettingsKeys.SHUFFLE_POSITION;
    private static final String KEY_X = SettingsKeys.X;
    private static final String KEY_Y = SettingsKeys.Y;
    private static final String KEY_OPACITY = SettingsKeys.OPACITY;

    private AndroidSettingsStore() {
    }

    static AppSettings load(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AppSettings settings = new AppSettings();
        settings.setVocabularyFileName(preferences.getString(KEY_VOCABULARY_FILE_NAME, AppSettings.DEFAULT_VOCABULARY_FILE_NAME));
        settings.setDisplayMode(parseEnum(DisplayMode.class, preferences.getString(KEY_DISPLAY_MODE, settings.getDisplayMode().name()), settings.getDisplayMode()));
        settings.setOverlayMode(parseEnum(OverlayMode.class, preferences.getString(KEY_OVERLAY_MODE, settings.getOverlayMode().name()), settings.getOverlayMode()));
        settings.setPlaybackMode(parseEnum(PlaybackMode.class, preferences.getString(KEY_PLAYBACK_MODE, settings.getPlaybackMode().name()), settings.getPlaybackMode()));
        settings.setIntervalSeconds(preferences.getInt(KEY_INTERVAL_SECONDS, settings.getIntervalSeconds()));
        settings.setNextWordIndex(preferences.getInt(KEY_NEXT_WORD_INDEX, settings.getNextWordIndex()));
        settings.setShuffleOrder(preferences.getString(KEY_SHUFFLE_ORDER, settings.getShuffleOrder()));
        settings.setShufflePosition(preferences.getInt(KEY_SHUFFLE_POSITION, settings.getShufflePosition()));
        settings.setX(preferences.getFloat(KEY_X, (float) settings.getX()));
        settings.setY(preferences.getFloat(KEY_Y, (float) settings.getY()));
        settings.setOpacity(preferences.getFloat(KEY_OPACITY, (float) settings.getOpacity()));
        return settings;
    }

    static void save(Context context, AppSettings settings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_VOCABULARY_FILE_NAME, settings.getVocabularyFileName())
                .putString(KEY_DISPLAY_MODE, settings.getDisplayMode().name())
                .putString(KEY_OVERLAY_MODE, settings.getOverlayMode().name())
                .putString(KEY_PLAYBACK_MODE, settings.getPlaybackMode().name())
                .putInt(KEY_INTERVAL_SECONDS, settings.getIntervalSeconds())
                .putInt(KEY_NEXT_WORD_INDEX, settings.getNextWordIndex())
                .putString(KEY_SHUFFLE_ORDER, settings.getShuffleOrder())
                .putInt(KEY_SHUFFLE_POSITION, settings.getShufflePosition())
                .putFloat(KEY_X, (float) settings.getX())
                .putFloat(KEY_Y, (float) settings.getY())
                .putFloat(KEY_OPACITY, (float) settings.getOpacity())
                .apply();
    }

    static int vocabularyIndex(String fileName) {
        return VocabularyCatalog.indexOfFileName(fileName);
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}

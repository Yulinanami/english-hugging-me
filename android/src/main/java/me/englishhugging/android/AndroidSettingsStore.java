package me.englishhugging.android;

import android.content.Context;
import android.content.SharedPreferences;

import me.englishhugging.core.AppSettings;
import me.englishhugging.core.DisplayMode;
import me.englishhugging.core.OverlayMode;

final class AndroidSettingsStore {
    static final String[] VOCABULARY_FILES = {
            "1-初中-顺序.json",
            "2-高中-顺序.json",
            "3-CET4-顺序.json",
            "4-CET6-顺序.json",
            "5-考研-顺序.json",
            "6-托福-顺序.json",
            "7-SAT-顺序.json"
    };

    private static final String PREFS = "english_hugging_settings";
    private static final String KEY_VOCABULARY_FILE_NAME = "vocabularyFileName";
    private static final String KEY_DISPLAY_MODE = "displayMode";
    private static final String KEY_OVERLAY_MODE = "overlayMode";
    private static final String KEY_INTERVAL_SECONDS = "intervalSeconds";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_OPACITY = "opacity";

    private AndroidSettingsStore() {
    }

    static AppSettings load(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AppSettings settings = new AppSettings();
        settings.setVocabularyFileName(preferences.getString(KEY_VOCABULARY_FILE_NAME, AppSettings.DEFAULT_VOCABULARY_FILE_NAME));
        settings.setDisplayMode(parseEnum(DisplayMode.class, preferences.getString(KEY_DISPLAY_MODE, settings.getDisplayMode().name()), settings.getDisplayMode()));
        settings.setOverlayMode(parseEnum(OverlayMode.class, preferences.getString(KEY_OVERLAY_MODE, settings.getOverlayMode().name()), settings.getOverlayMode()));
        settings.setIntervalSeconds(preferences.getInt(KEY_INTERVAL_SECONDS, settings.getIntervalSeconds()));
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
                .putInt(KEY_INTERVAL_SECONDS, settings.getIntervalSeconds())
                .putFloat(KEY_X, (float) settings.getX())
                .putFloat(KEY_Y, (float) settings.getY())
                .putFloat(KEY_OPACITY, (float) settings.getOpacity())
                .apply();
    }

    static int vocabularyIndex(String fileName) {
        for (int i = 0; i < VOCABULARY_FILES.length; i++) {
            if (VOCABULARY_FILES[i].equals(fileName)) {
                return i;
            }
        }
        return 0;
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}

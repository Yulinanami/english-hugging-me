package me.englishhugging.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.GsonBuilder;

import me.englishhugging.core.AppSettings;
import me.englishhugging.core.DisplayMode;
import me.englishhugging.core.OverlayMode;
import me.englishhugging.core.PlaybackMode;
import me.englishhugging.core.SettingsKeys;
import me.englishhugging.core.VocabularyCatalog;
import me.englishhugging.core.VocabularyJsonLoader;
import me.englishhugging.core.WordEntry;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

final class AndroidSettingsStore {
    static final String CUSTOM_VOCABULARY_FILE_NAME = "自定义词汇";
    static final String[] VOCABULARY_FILES = vocabularyFiles();

    private static final String PREFS = "english_hugging_settings";
    private static final String KEY_VOCABULARY_FILE_NAME = SettingsKeys.VOCABULARY_FILE_NAME;
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
    private static final String KEY_OPACITY = SettingsKeys.OPACITY;
    private static final String KEY_WORD_COLOR = SettingsKeys.WORD_COLOR;
    private static final String KEY_TYPE_COLOR = SettingsKeys.TYPE_COLOR;
    private static final String KEY_TRANSLATION_COLOR = SettingsKeys.TRANSLATION_COLOR;
    private static final String KEY_PHRASE_COLOR = SettingsKeys.PHRASE_COLOR;
    private static final String KEY_CUSTOM_VOCABULARY_JSON = "customVocabularyJson";

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
        settings.setRandomPlayedCount(preferences.getInt(KEY_RANDOM_PLAYED_COUNT, settings.getRandomPlayedCount()));
        settings.setX(preferences.getFloat(KEY_X, (float) settings.getX()));
        settings.setY(preferences.getFloat(KEY_Y, (float) settings.getY()));
        settings.setOpacity(preferences.getFloat(KEY_OPACITY, (float) settings.getOpacity()));
        settings.setWordColor(preferences.getString(KEY_WORD_COLOR, settings.getWordColor()));
        settings.setTypeColor(preferences.getString(KEY_TYPE_COLOR, settings.getTypeColor()));
        settings.setTranslationColor(preferences.getString(KEY_TRANSLATION_COLOR, settings.getTranslationColor()));
        settings.setPhraseColor(preferences.getString(KEY_PHRASE_COLOR, settings.getPhraseColor()));
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
                .putInt(KEY_RANDOM_PLAYED_COUNT, settings.getRandomPlayedCount())
                .putFloat(KEY_X, (float) settings.getX())
                .putFloat(KEY_Y, (float) settings.getY())
                .putFloat(KEY_OPACITY, (float) settings.getOpacity())
                .putString(KEY_WORD_COLOR, settings.getWordColor())
                .putString(KEY_TYPE_COLOR, settings.getTypeColor())
                .putString(KEY_TRANSLATION_COLOR, settings.getTranslationColor())
                .putString(KEY_PHRASE_COLOR, settings.getPhraseColor())
                .apply();
    }

    static void loadPlaybackProgress(Context context, AppSettings settings, String vocabularyKey) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        settings.setNextWordIndex(preferences.getInt(progressKey(vocabularyKey, KEY_NEXT_WORD_INDEX), settings.getNextWordIndex()));
        settings.setShuffleOrder(preferences.getString(progressKey(vocabularyKey, KEY_SHUFFLE_ORDER), settings.getShuffleOrder()));
        settings.setShufflePosition(preferences.getInt(progressKey(vocabularyKey, KEY_SHUFFLE_POSITION), settings.getShufflePosition()));
        settings.setRandomPlayedCount(preferences.getInt(progressKey(vocabularyKey, KEY_RANDOM_PLAYED_COUNT), settings.getRandomPlayedCount()));
    }

    static void savePlaybackProgress(Context context, AppSettings settings, String vocabularyKey) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(progressKey(vocabularyKey, KEY_NEXT_WORD_INDEX), settings.getNextWordIndex())
                .putString(progressKey(vocabularyKey, KEY_SHUFFLE_ORDER), settings.getShuffleOrder())
                .putInt(progressKey(vocabularyKey, KEY_SHUFFLE_POSITION), settings.getShufflePosition())
                .putInt(progressKey(vocabularyKey, KEY_RANDOM_PLAYED_COUNT), settings.getRandomPlayedCount())
                .apply();
    }

    static String[] playbackRecordLines(Context context) {
        List<String> lines = new ArrayList<>();
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            lines.add(playbackRecordLine(context, item.getFileName(), item.getDisplayName()));
        }
        if (hasCustomVocabulary(context)) {
            lines.add(playbackRecordLine(context, CUSTOM_VOCABULARY_FILE_NAME, CUSTOM_VOCABULARY_FILE_NAME));
        }
        return lines.toArray(new String[0]);
    }

    static List<WordEntry> loadCustomWords(Context context) {
        String json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_CUSTOM_VOCABULARY_JSON, "[]");
        try {
            return new ArrayList<>(new VocabularyJsonLoader().load(new StringReader(json)));
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    static void appendCustomWord(Context context, WordEntry wordEntry) {
        List<WordEntry> words = loadCustomWords(context);
        words.add(wordEntry);
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(words);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM_VOCABULARY_JSON, json)
                .apply();
    }

    static boolean isCustomVocabulary(String fileName) {
        return CUSTOM_VOCABULARY_FILE_NAME.equals(fileName);
    }

    static int vocabularyIndex(String fileName) {
        for (int i = 0; i < VOCABULARY_FILES.length; i++) {
            if (VOCABULARY_FILES[i].equals(fileName)) {
                return i;
            }
        }
        return 0;
    }

    private static boolean hasCustomVocabulary(Context context) {
        return !loadCustomWords(context).isEmpty();
    }

    private static String playbackRecordLine(Context context, String vocabularyKey, String label) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int nextWordIndex = preferences.getInt(progressKey(vocabularyKey, KEY_NEXT_WORD_INDEX), 0);
        int shufflePosition = preferences.getInt(progressKey(vocabularyKey, KEY_SHUFFLE_POSITION), 0);
        int randomPlayedCount = preferences.getInt(progressKey(vocabularyKey, KEY_RANDOM_PLAYED_COUNT), 0);
        return label + "：顺序播放到第 " + (nextWordIndex + 1) + " 个；随机播放 " + randomPlayedCount + " 个；随机不重复 " + shufflePosition + " 个";
    }

    private static String progressKey(String vocabularyKey, String key) {
        return "progress." + vocabularyKey + "." + key;
    }

    private static String[] vocabularyFiles() {
        String[] builtIn = VocabularyCatalog.fileNames();
        String[] values = new String[builtIn.length + 1];
        System.arraycopy(builtIn, 0, values, 0, builtIn.length);
        values[values.length - 1] = CUSTOM_VOCABULARY_FILE_NAME;
        return values;
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}

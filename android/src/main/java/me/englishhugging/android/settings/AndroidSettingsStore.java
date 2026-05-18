package me.englishhugging.android.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.GsonBuilder;

import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.DisplayMode;
import me.englishhugging.core.settings.OverlayMode;
import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.core.settings.SettingsKeys;
import me.englishhugging.core.vocabulary.VocabularyCatalog;
import me.englishhugging.core.vocabulary.VocabularyJsonLoader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class AndroidSettingsStore {
    public static final String CUSTOM_VOCABULARY_FILE_NAME = "自定义词汇";
    public static final String[] VOCABULARY_FILES = vocabularyFiles();

    private static final String PREFS = "english_hugging_settings";
    private static final String KEY_CUSTOM_VOCABULARY_JSON = "customVocabularyJson";

    private AndroidSettingsStore() {}

    public static AppSettings load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AppSettings s = new AppSettings();
        s.setVocabularyFileName(p.getString(SettingsKeys.VOCABULARY_FILE_NAME, AppSettings.DEFAULT_VOCABULARY_FILE_NAME));
        s.setDisplayMode(parseEnum(DisplayMode.class, p.getString(SettingsKeys.DISPLAY_MODE, s.getDisplayMode().name()), s.getDisplayMode()));
        s.setOverlayMode(parseEnum(OverlayMode.class, p.getString(SettingsKeys.OVERLAY_MODE, s.getOverlayMode().name()), s.getOverlayMode()));
        s.setPlaybackMode(parseEnum(PlaybackMode.class, p.getString(SettingsKeys.PLAYBACK_MODE, s.getPlaybackMode().name()), s.getPlaybackMode()));
        s.setIntervalSeconds(p.getInt(SettingsKeys.INTERVAL_SECONDS, s.getIntervalSeconds()));
        s.setNextWordIndex(p.getInt(SettingsKeys.NEXT_WORD_INDEX, s.getNextWordIndex()));
        s.setShuffleOrder(p.getString(SettingsKeys.SHUFFLE_ORDER, s.getShuffleOrder()));
        s.setShufflePosition(p.getInt(SettingsKeys.SHUFFLE_POSITION, s.getShufflePosition()));
        s.setRandomPlayedCount(p.getInt(SettingsKeys.RANDOM_PLAYED_COUNT, s.getRandomPlayedCount()));
        s.setX(p.getFloat(SettingsKeys.X, (float) s.getX()));
        s.setY(p.getFloat(SettingsKeys.Y, (float) s.getY()));
        s.setOpacity(p.getFloat(SettingsKeys.OPACITY, (float) s.getOpacity()));
        s.setWordColor(p.getString(SettingsKeys.WORD_COLOR, s.getWordColor()));
        s.setTypeColor(p.getString(SettingsKeys.TYPE_COLOR, s.getTypeColor()));
        s.setTranslationColor(p.getString(SettingsKeys.TRANSLATION_COLOR, s.getTranslationColor()));
        s.setPhraseColor(p.getString(SettingsKeys.PHRASE_COLOR, s.getPhraseColor()));
        return s;
    }

    public static void save(Context context, AppSettings s) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(SettingsKeys.VOCABULARY_FILE_NAME, s.getVocabularyFileName())
                .putString(SettingsKeys.DISPLAY_MODE, s.getDisplayMode().name())
                .putString(SettingsKeys.OVERLAY_MODE, s.getOverlayMode().name())
                .putString(SettingsKeys.PLAYBACK_MODE, s.getPlaybackMode().name())
                .putInt(SettingsKeys.INTERVAL_SECONDS, s.getIntervalSeconds())
                .putInt(SettingsKeys.NEXT_WORD_INDEX, s.getNextWordIndex())
                .putString(SettingsKeys.SHUFFLE_ORDER, s.getShuffleOrder())
                .putInt(SettingsKeys.SHUFFLE_POSITION, s.getShufflePosition())
                .putInt(SettingsKeys.RANDOM_PLAYED_COUNT, s.getRandomPlayedCount())
                .putFloat(SettingsKeys.X, (float) s.getX())
                .putFloat(SettingsKeys.Y, (float) s.getY())
                .putFloat(SettingsKeys.OPACITY, (float) s.getOpacity())
                .putString(SettingsKeys.WORD_COLOR, s.getWordColor())
                .putString(SettingsKeys.TYPE_COLOR, s.getTypeColor())
                .putString(SettingsKeys.TRANSLATION_COLOR, s.getTranslationColor())
                .putString(SettingsKeys.PHRASE_COLOR, s.getPhraseColor())
                .apply();
    }

    public static void loadPlaybackProgress(Context context, AppSettings s, String vocabularyKey) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        s.setNextWordIndex(p.getInt(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX), s.getNextWordIndex()));
        s.setShuffleOrder(p.getString(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_ORDER), s.getShuffleOrder()));
        s.setShufflePosition(p.getInt(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION), s.getShufflePosition()));
        s.setRandomPlayedCount(p.getInt(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT), s.getRandomPlayedCount()));
    }

    public static void savePlaybackProgress(Context context, AppSettings s, String vocabularyKey) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX), s.getNextWordIndex())
                .putString(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_ORDER), s.getShuffleOrder())
                .putInt(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION), s.getShufflePosition())
                .putInt(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT), s.getRandomPlayedCount())
                .apply();
    }

    public static void clearAllPlaybackProgress(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = p.edit();
        for (String key : p.getAll().keySet()) {
            if (key.startsWith("progress.")) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    public static String[] playbackRecordLines(Context context) {
        List<String> lines = new ArrayList<>();
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            lines.add(playbackRecordLine(context, item.getFileName(), item.getDisplayName()));
        }
        if (hasCustomVocabulary(context)) lines.add(playbackRecordLine(context, CUSTOM_VOCABULARY_FILE_NAME, CUSTOM_VOCABULARY_FILE_NAME));
        return lines.toArray(new String[0]);
    }

    public static List<WordEntry> loadCustomWords(Context context) {
        String json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CUSTOM_VOCABULARY_JSON, "[]");
        try { return new ArrayList<>(new VocabularyJsonLoader().load(new StringReader(json))); }
        catch (Exception ignored) { return new ArrayList<>(); }
    }

    public static void appendCustomWord(Context context, WordEntry wordEntry) {
        List<WordEntry> words = loadCustomWords(context);
        words.add(wordEntry);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_CUSTOM_VOCABULARY_JSON, new GsonBuilder().setPrettyPrinting().create().toJson(words))
                .apply();
    }

    public static boolean isCustomVocabulary(String fileName) { return CUSTOM_VOCABULARY_FILE_NAME.equals(fileName); }

    private static boolean hasCustomVocabulary(Context context) { return !loadCustomWords(context).isEmpty(); }

    private static String playbackRecordLine(Context context, String vocabularyKey, String label) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int nextWordIndex = p.getInt(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX), 0);
        int shufflePosition = p.getInt(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION), 0);
        int randomPlayedCount = p.getInt(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT), 0);
        return label + "：顺序播放到第 " + (nextWordIndex + 1) + " 个；随机播放 " + randomPlayedCount + " 个；随机不重复 " + shufflePosition + " 个";
    }

    private static String progressKey(String vocabularyKey, String key) { return "progress." + vocabularyKey + "." + key; }

    private static String[] vocabularyFiles() {
        String[] builtIn = VocabularyCatalog.fileNames();
        String[] values = new String[builtIn.length + 1];
        System.arraycopy(builtIn, 0, values, 0, builtIn.length);
        values[values.length - 1] = CUSTOM_VOCABULARY_FILE_NAME;
        return values;
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        try { return Enum.valueOf(type, value); } catch (RuntimeException ignored) { return fallback; }
    }
}

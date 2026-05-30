package me.englishhugging.android.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.GsonBuilder;

import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.SettingsMapper;
import me.englishhugging.core.settings.SettingsStorage;
import me.englishhugging.core.vocabulary.VocabularyCatalog;
import me.englishhugging.core.vocabulary.VocabularyJsonLoader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 移动端专用的设置与本地存储桥接器。
 *
 * <p>这个类实现了 {@link SettingsStorage} 接口，将 Android 平台的
 * {@link SharedPreferences} 机制注入到核心模块的配置引擎中。
 * 此外，它还负责将用户在移动端编辑的“自定义词库”序列化为 JSON 字符串，
 * 并塞入 SharedPreferences 中进行持久化保存。
 */
public final class AndroidSettingsStore {
    
    // --- 常量定义 ---
    public static final String CUSTOM_VOCABULARY_FILE_NAME = SettingsMapper.CUSTOM_VOCABULARY_FILE_NAME;
    public static final String[] VOCABULARY_FILES = vocabularyFiles();

    private static final String PREFS = "english_hugging_settings";
    private static final String KEY_CUSTOM_VOCABULARY_JSON = "customVocabularyJson";

    /**
     * 阻止工具类被实例化。
     */
    private AndroidSettingsStore() {
        // 无需实例化
    }

    /**
     * SharedPreferences 的适配器实现。
     * 它拦截了核心层对 KV 存储的所有增删改查请求，并路由给 Android 系统。
     */
    private static class SharedPrefsStorage implements SettingsStorage {
        private final SharedPreferences prefs;
        private final SharedPreferences.Editor editor;
        private boolean editing = false;

        SharedPrefsStorage(Context context) {
            this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            this.editor = this.prefs.edit();
        }

        @Override
        public String getString(String key, String defaultValue) {
            return this.prefs.getString(key, defaultValue);
        }

        @Override
        public int getInt(String key, int defaultValue) {
            return this.prefs.getInt(key, defaultValue);
        }

        @Override
        public double getDouble(String key, double defaultValue) {
            return this.prefs.getFloat(key, (float) defaultValue);
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            return this.prefs.getBoolean(key, defaultValue);
        }

        @Override
        public void putString(String key, String value) {
            this.editor.putString(key, value);
            this.editing = true;
        }

        @Override
        public void putInt(String key, int value) {
            this.editor.putInt(key, value);
            this.editing = true;
        }

        @Override
        public void putDouble(String key, double value) {
            this.editor.putFloat(key, (float) value);
            this.editing = true;
        }

        @Override
        public void putBoolean(String key, boolean value) {
            this.editor.putBoolean(key, value);
            this.editing = true;
        }

        @Override
        public void remove(String key) {
            this.editor.remove(key);
            this.editing = true;
        }

        @Override
        public Iterable<String> getAllKeys() {
            return this.prefs.getAll().keySet();
        }

        @Override
        public void commit() {
            if (this.editing) {
                this.editor.apply();
                this.editing = false;
            }
        }
    }

    /**
     * 从本地存储中完整反序列化出 AppSettings 模型。
     */
    public static AppSettings load(Context context) {
        return SettingsMapper.load(new SharedPrefsStorage(context));
    }

    /**
     * 将业务测在内存中修改好的 AppSettings 同步到 Android 文件系统。
     */
    public static void save(Context context, AppSettings s) {
        SettingsMapper.save(new SharedPrefsStorage(context), s);
    }

    /**
     * 加载指定词库的历史播放进度（游标位置、洗牌状态等）。
     */
    public static void loadPlaybackProgress(Context context, AppSettings s, String vocabularyKey) {
        SettingsMapper.loadPlaybackProgress(new SharedPrefsStorage(context), s, vocabularyKey);
    }

    /**
     * 保存指定词库的当前播放进度，以便下次继续。
     */
    public static void savePlaybackProgress(Context context, AppSettings s, String vocabularyKey) {
        SettingsMapper.savePlaybackProgress(new SharedPrefsStorage(context), s, vocabularyKey);
    }

    /**
     * 强行抹除 Android 系统中保存的所有词库的播放历史。
     */
    public static void clearAllPlaybackProgress(Context context) {
        SettingsMapper.clearAllPlaybackProgress(new SharedPrefsStorage(context));
    }

    /**
     * 读取所有带有播放记录的词库并生成带格式的进度报表行，供 UI 直接展示。
     */
    public static String[] playbackRecordLines(Context context) {
        return SettingsMapper.playbackRecordLines(new SharedPrefsStorage(context), hasCustomVocabulary(context));
    }

    /**
     * 读取用户自己录入的“自定义生词本”。
     * 由于 Android 没有像桌面端那样方便的裸文件访问权限，我们将自定义生词转为 JSON 字符串硬塞在 SharedPreferences 里。
     */
    public static List<WordEntry> loadCustomWords(Context context) {
        String json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CUSTOM_VOCABULARY_JSON, "[]");
        try { 
            return new ArrayList<>(new VocabularyJsonLoader().load(new StringReader(json))); 
        } catch (Exception ignored) { 
            return new ArrayList<>(); 
        }
    }

    /**
     * 往自定义词库中追加一个生词，如果存在同拼写的单词则直接覆盖。
     */
    public static void appendCustomWord(Context context, WordEntry wordEntry) {
        List<WordEntry> words = loadCustomWords(context);
        
        // 移除同名老单词
        words.removeIf(w -> w.getWord().equals(wordEntry.getWord()));
        
        words.add(wordEntry);
        saveCustomWords(context, words);
    }

    /**
     * 全量覆盖并保存自定义词库。
     */
    public static void saveCustomWords(Context context, List<WordEntry> words) {
        String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(words);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM_VOCABULARY_JSON, jsonString)
                .apply();
    }

    /**
     * 判断一个文件路径配置是不是指向那个虚拟的“自定义词库”。
     */
    public static boolean isCustomVocabulary(String fileName) {
        return CUSTOM_VOCABULARY_FILE_NAME.equals(fileName);
    }

    /**
     * 检查当前用户有没有录入过自定义词汇。
     */
    private static boolean hasCustomVocabulary(Context context) {
        return !loadCustomWords(context).isEmpty();
    }

    /**
     * 缝合系统预置的词库列表与特殊的“自定义词库”选项，提供给下拉菜单使用。
     */
    private static String[] vocabularyFiles() {
        String[] builtIn = VocabularyCatalog.fileNames();
        String[] values = new String[builtIn.length + 1];
        
        System.arraycopy(builtIn, 0, values, 0, builtIn.length);
        values[values.length - 1] = CUSTOM_VOCABULARY_FILE_NAME;
        
        return values;
    }
}

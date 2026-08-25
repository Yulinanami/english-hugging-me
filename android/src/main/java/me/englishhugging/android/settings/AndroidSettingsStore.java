package me.englishhugging.android.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.GsonBuilder;

import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.SettingsKeys;
import me.englishhugging.core.settings.SettingsMapper;
import me.englishhugging.core.settings.SettingsStorage;
import me.englishhugging.core.vocabulary.VocabularyCatalog;
import me.englishhugging.core.vocabulary.VocabularyJsonLoader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Android 手机端配置与自定义生词存储工具。
 *
 * <p>负责将应用设置和自定义生词保存到 Android 的 SharedPreferences 中。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 加载全局配置
 * AppSettings settings = AndroidSettingsStore.load(context);
 *
 * // 修改并保存
 * settings.setWordFontSize(26);
 * AndroidSettingsStore.save(context, settings);
 * </code></pre>
 */
public final class AndroidSettingsStore {
    
    /** 自定义词库专用虚拟文件名 */
    public static final String CUSTOM_VOCABULARY_FILE_NAME = SettingsMapper.CUSTOM_VOCABULARY_FILE_NAME;

    /** 所有内置词库文件名列表 */
    public static final String[] VOCABULARY_FILES = vocabularyFiles();

    /** 存储应用设置的 SharedPreferences 文件名称 */
    private static final String PREFS = "english_hugging_settings";

    /** 存储自定义生词 JSON 字符串的键名 */
    private static final String KEY_CUSTOM_VOCABULARY_JSON = "customVocabularyJson";

    /**
     * 私有构造函数，无需实例化。
     */
    private AndroidSettingsStore() {
        // 无需实例化
    }

    /**
     * 基于 SharedPreferences 的设置存储实现。
     */
    private static class SharedPrefsStorage implements SettingsStorage {
        /** SharedPreferences 实例 */
        private final SharedPreferences prefs;
        /** 本地存储修改对象 */
        private final SharedPreferences.Editor editor;
        /** 标记本次是否有尚未提交的修改 */
        private boolean editing = false;

        /**
         * 创建存储对象。
         *
         * @param context Android 上下文
         */
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
     * 从本地读取并加载全部应用设置。
     *
     * <p>如果是首次打开应用（尚未设置悬浮窗宽高），会默认将宽高设为 0，让悬浮窗自动适应内容大小。
     *
     * @param context Android 上下文对象
     * @return 包含当前所有配置的 AppSettings 对象
     */
    public static AppSettings load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AppSettings settings = SettingsMapper.load(new SharedPrefsStorage(context));

        // Android 首次启动默认让悬浮窗随内容自动适配；已保存的手动尺寸仍按用户设置加载。
        if (!prefs.contains(SettingsKeys.WIDTH)) {
            settings.setWidth(0);
        }
        if (!prefs.contains(SettingsKeys.HEIGHT)) {
            settings.setHeight(0);
        }

        return settings;
    }

    /**
     * 将应用设置保存到本地。
     *
     * @param context Android 上下文对象
     * @param s       待保存的 AppSettings 设置对象
     */
    public static void save(Context context, AppSettings s) {
        SettingsMapper.save(new SharedPrefsStorage(context), s);
    }

    /**
     * 加载指定词库的历史背诵进度（如背到第几个词、乱序列表等）。
     *
     * @param context       Android 上下文对象
     * @param s             接收进度的设置对象
     * @param vocabularyKey 词库文件名或标识
     */
    public static void loadPlaybackProgress(Context context, AppSettings s, String vocabularyKey) {
        SettingsMapper.loadPlaybackProgress(new SharedPrefsStorage(context), s, vocabularyKey);
    }

    /**
     * 保存指定词库当前的背诵进度。
     *
     * @param context       Android 上下文对象
     * @param s             当前设置对象
     * @param vocabularyKey 词库文件名或标识
     */
    public static void savePlaybackProgress(Context context, AppSettings s, String vocabularyKey) {
        SettingsMapper.savePlaybackProgress(new SharedPrefsStorage(context), s, vocabularyKey);
    }

    /**
     * 清除本地记录的所有词库的背诵进度。
     *
     * @param context Android 上下文对象
     */
    public static void clearAllPlaybackProgress(Context context) {
        SettingsMapper.clearAllPlaybackProgress(new SharedPrefsStorage(context));
    }

    /**
     * 获取所有词库的背诵进度描述文本，用于在“学习记录”界面展示。
     *
     * @param context Android 上下文对象
     * @return 每一行代表一个词库进度的字符串数组
     */
    public static String[] playbackRecordLines(Context context) {
        return SettingsMapper.playbackRecordLines(new SharedPrefsStorage(context), hasCustomVocabulary(context));
    }

    /**
     * 读取用户自己添加的自定义生词列表。
     *
     * <p>Android 端将自定义单词以 JSON 字符串保存在 SharedPreferences 中。
     *
     * @param context Android 上下文对象
     * @return 自定义生词列表；如果没有任何词或解析出错则返回空列表
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
     * 往自定义词库中添加一个新单词。如果单词已经存在，则替换旧单词。
     *
     * @param context   Android 上下文对象
     * @param wordEntry 要添加的单词对象
     */
    public static void appendCustomWord(Context context, WordEntry wordEntry) {
        List<WordEntry> words = loadCustomWords(context);
        
        // 移除同名旧单词
        words.removeIf(w -> w.word().equals(wordEntry.word()));
        
        words.add(wordEntry);
        saveCustomWords(context, words);
    }

    /**
     * 保存完整的自定义生词列表。
     *
     * @param context Android 上下文对象
     * @param words   自定义单词列表
     */
    public static void saveCustomWords(Context context, List<WordEntry> words) {
        String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(words);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM_VOCABULARY_JSON, jsonString)
                .apply();
    }

    /**
     * 判断指定的文件名是否表示“自定义词汇”。
     *
     * @param fileName 词库文件名
     * @return 如果是自定义词库则返回 true
     */
    public static boolean isCustomVocabulary(String fileName) {
        return CUSTOM_VOCABULARY_FILE_NAME.equals(fileName);
    }

    /**
     * 检查用户是否添加过自定义词汇。
     *
     * @param context Android 上下文对象
     * @return 如果有自定义词汇返回 true
     */
    private static boolean hasCustomVocabulary(Context context) {
        return !loadCustomWords(context).isEmpty();
    }

    /**
     * 组合内置词库与自定义词库选项，用于在界面下拉框中展示。
     *
     * @return 包含所有可用词库选项的字符串数组
     */
    private static String[] vocabularyFiles() {
        String[] builtIn = VocabularyCatalog.fileNames();
        String[] values = new String[builtIn.length + 1];
        
        System.arraycopy(builtIn, 0, values, 0, builtIn.length);
        values[values.length - 1] = CUSTOM_VOCABULARY_FILE_NAME;
        
        return values;
    }
}

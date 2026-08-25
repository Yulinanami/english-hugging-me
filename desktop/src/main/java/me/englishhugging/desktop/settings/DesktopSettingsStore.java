package me.englishhugging.desktop.settings;

import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.SettingsMapper;
import me.englishhugging.core.settings.SettingsStorage;
import me.englishhugging.core.vocabulary.VocabularyJsonLoader;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 桌面端本地配置与自定义生词存储工具。
 *
 * <p>负责将应用设置与自定义生词保存到用户家目录下的配置文件中。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * DesktopSettingsStore store = new DesktopSettingsStore();
 * 
 * // 启动时加载本地保存的配置数据
 * AppSettings settings = store.load();
 * 
 * // 追加一个用户手敲的自定义单词
 * store.appendCustomWord(new WordEntry("awesome", ...));
 * </code></pre>
 */
public final class DesktopSettingsStore {

    /** 跨平台的自定义词库虚拟键名标识 */
    public static final String CUSTOM_VOCABULARY_FILE_NAME = SettingsMapper.CUSTOM_VOCABULARY_FILE_NAME;

    /** 桌面端全局配置保存位置：用户家目录下的隐藏属性文件 */
    private static final File SETTINGS_FILE = new File(System.getProperty("user.home"), ".english-hugging-me.properties");
    
    /** 桌面端自定义生词本保存位置：用户家目录下的隐藏 JSON 文件 */
    private static final File CUSTOM_WORDS_FILE = new File(System.getProperty("user.home"), ".english-hugging-me-custom.json");

    /**
     * 默认构造函数。
     */
    public DesktopSettingsStore() {
        // 无需额外初始化
    }

    /**
     * 基于 Java Properties 属性文件的键值对存储实现。
     * 把通用设置接口的操作保存到本地的 .properties 文件中。
     */
    private static class PropertiesStorage implements SettingsStorage {
        
        /** Properties 属性集合对象 */
        private final Properties p;
        /** 标记是否有尚未保存到硬盘的修改 */
        private boolean modified = false;

        /**
         * 初始化本地属性存储。如果文件存在则预先读取内容。
         */
        PropertiesStorage() {
            this.p = new Properties();
            if (SETTINGS_FILE.exists()) {
                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(SETTINGS_FILE), StandardCharsets.UTF_8)) {
                    this.p.load(reader);
                } catch (IOException ignored) {
                    // 读取失败时忽略，视为空白配置
                }
            }
        }

        @Override
        public String getString(String key, String defaultValue) {
            return this.p.getProperty(key, defaultValue);
        }

        @Override
        public int getInt(String key, int defaultValue) {
            try {
                return Integer.parseInt(this.p.getProperty(key));
            } catch (Exception e) {
                return defaultValue;
            }
        }

        @Override
        public double getDouble(String key, double defaultValue) {
            try {
                return Double.parseDouble(this.p.getProperty(key));
            } catch (Exception e) {
                return defaultValue;
            }
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            String value = this.p.getProperty(key);
            return value != null ? Boolean.parseBoolean(value) : defaultValue;
        }

        @Override
        public void putString(String key, String value) {
            if (value != null) {
                this.p.setProperty(key, value);
            } else {
                this.p.remove(key);
            }
            this.modified = true;
        }

        @Override
        public void putInt(String key, int value) {
            this.p.setProperty(key, String.valueOf(value));
            this.modified = true;
        }

        @Override
        public void putDouble(String key, double value) {
            this.p.setProperty(key, String.valueOf(value));
            this.modified = true;
        }

        @Override
        public void putBoolean(String key, boolean value) {
            this.p.setProperty(key, String.valueOf(value));
            this.modified = true;
        }

        @Override
        public void remove(String key) {
            this.p.remove(key);
            this.modified = true;
        }

        @Override
        public Iterable<String> getAllKeys() {
            return this.p.stringPropertyNames();
        }

        @Override
        public void commit() {
            if (!this.modified) {
                return;
            }
            
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(SETTINGS_FILE), StandardCharsets.UTF_8)) {
                this.p.store(writer, "English Hugging Me Settings");
            } catch (IOException ignored) {
                // 保存失败时静默忽略
            }
            this.modified = false;
        }
    }

    /**
     * 从本地配置文件中加载全部应用设置。
     *
     * @return 包含当前所有配置项的 AppSettings 实例
     */
    public AppSettings load() {
        return SettingsMapper.load(new PropertiesStorage());
    }

    /**
     * 将应用设置写入本地配置文件保存。
     *
     * @param settings 待保存的应用设置对象
     */
    public void save(AppSettings settings) {
        SettingsMapper.save(new PropertiesStorage(), settings);
    }

    /**
     * 从本地存储中读取指定词库的历史背诵进度。
     *
     * @param settings      接收进度的配置对象
     * @param vocabularyKey 词库文件名或标识
     */
    public void loadPlaybackProgress(AppSettings settings, String vocabularyKey) {
        SettingsMapper.loadPlaybackProgress(new PropertiesStorage(), settings, vocabularyKey);
    }

    /**
     * 将指定词库当前的背诵进度保存到本地。
     *
     * @param settings      包含最新进度的配置对象
     * @param vocabularyKey 词库文件名或标识
     */
    public void savePlaybackProgress(AppSettings settings, String vocabularyKey) {
        SettingsMapper.savePlaybackProgress(new PropertiesStorage(), settings, vocabularyKey);
    }

    /**
     * 清除本地记录的所有词库的背诵进度。
     */
    public void clearAllPlaybackProgress() {
        SettingsMapper.clearAllPlaybackProgress(new PropertiesStorage());
    }

    /**
     * 获取所有词库的背诵进度总结文本，供设置界面的“播放记录”面板展示。
     *
     * @return 包含每本词库学习进度的一组文本行
     */
    public String[] playbackRecordLines() {
        return SettingsMapper.playbackRecordLines(new PropertiesStorage(), hasCustomVocabulary());
    }

    /**
     * 获取单个词库的一行背诵进度描述。
     *
     * @param vocabularyKey 词库文件名
     * @param label         词库显示名称
     * @return 一行进度描述文字
     */
    public String playbackRecordLine(String vocabularyKey, String label) {
        return SettingsMapper.playbackRecordLine(new PropertiesStorage(), vocabularyKey, label);
    }

    /**
     * 从本地 JSON 文件加载用户自定义的生词列表。
     *
     * @return 自定义生词列表；如果文件不存在或损坏则返回空列表
     */
    public List<WordEntry> loadCustomWords() {
        if (!CUSTOM_WORDS_FILE.exists()) {
            return new ArrayList<>();
        }
        
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(CUSTOM_WORDS_FILE), StandardCharsets.UTF_8)) {
            VocabularyJsonLoader loader = new VocabularyJsonLoader();
            return new ArrayList<>(loader.load(reader));
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    /**
     * 往自定义生词本中添加一个新单词。如果单词已存在（拼写相同），则覆盖旧条目。
     *
     * @param wordEntry 要添加的单词对象
     */
    public void appendCustomWord(WordEntry wordEntry) {
        List<WordEntry> words = loadCustomWords();
        
        // 删除已经存在的同拼写单词
        words.removeIf(w -> w.word().equals(wordEntry.word()));
        words.add(wordEntry);
        
        saveCustomWords(words);
    }

    /**
     * 保存完整的自定义单词列表到本地 JSON 文件中。
     *
     * @param words 待保存的单词列表
     */
    public void saveCustomWords(List<WordEntry> words) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(CUSTOM_WORDS_FILE), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(words, writer);
        } catch (Exception ignored) {
            // 保存异常时忽略
        }
    }

    /**
     * 判断指定的文件名是否为“自定义词汇”。
     *
     * @param fileName 候选文件名
     * @return 如果是自定义词库则返回 true
     */
    public boolean isCustomVocabulary(String fileName) {
        return CUSTOM_VOCABULARY_FILE_NAME.equals(fileName);
    }

    /**
     * 检查本地是否存在非空的自定义生词本文件。
     *
     * @return 如果存在有效生词本文件返回 true
     */
    private boolean hasCustomVocabulary() {
        return CUSTOM_WORDS_FILE.exists() && CUSTOM_WORDS_FILE.length() > 0;
    }
}

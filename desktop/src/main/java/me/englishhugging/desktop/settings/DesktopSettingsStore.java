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
 * 桌面端的本地配置存储引擎。
 *
 * <p>这个类是桌面端持久化的核心。它内部实现了一个基于 Java {@link Properties} 文件的 {@link SettingsStorage}，
 * 用于在用户的主目录下以文本文件的形式读写全局的配置参数以及自定义的生词本。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * DesktopSettingsStore store = new DesktopSettingsStore();
 * 
 * // 启动时加载最新的持久化数据到内存
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
        // 留空，通过内部类桥接读写逻辑
    }

    /**
     * 私有的本地持久化实现，它扮演了 Core 模块中抽象的 {@link SettingsStorage} 角色。
     */
    private static class PropertiesStorage implements SettingsStorage {
        
        private final Properties p;
        private boolean modified = false;

        /**
         * 初始化物理存储。如果本地文件存在则预先加载它。
         */
        PropertiesStorage() {
            this.p = new Properties();
            if (SETTINGS_FILE.exists()) {
                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(SETTINGS_FILE), StandardCharsets.UTF_8)) {
                    this.p.load(reader);
                } catch (IOException ignored) {
                    // 读取失败时忽略，将其视为一张白纸
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
            if (value != null) {
                return Boolean.parseBoolean(value);
            } else {
                return defaultValue;
            }
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
                // 如果写入磁盘被拒绝或发生 IO 异常，暂且丢弃本次落盘
            }
            this.modified = false;
        }
    }

    /**
     * 将整个应用配置反序列化到内存。
     *
     * @return 最新的配置实例
     */
    public AppSettings load() {
        return SettingsMapper.load(new PropertiesStorage());
    }

    /**
     * 将整个应用配置序列化落盘。
     *
     * @param settings 更新后的配置实例
     */
    public void save(AppSettings settings) {
        SettingsMapper.save(new PropertiesStorage(), settings);
    }

    /**
     * 从持久化文件中提取某一个专属词库的当前进度。
     *
     * @param settings      需要被注入进度的目标配置对象
     * @param vocabularyKey 词库的唯一标识（通常是文件名）
     */
    public void loadPlaybackProgress(AppSettings settings, String vocabularyKey) {
        SettingsMapper.loadPlaybackProgress(new PropertiesStorage(), settings, vocabularyKey);
    }

    /**
     * 仅将某个词库对应的进度保存至磁盘。
     *
     * @param settings      作为数据源的配置对象
     * @param vocabularyKey 词库唯一标识
     */
    public void savePlaybackProgress(AppSettings settings, String vocabularyKey) {
        SettingsMapper.savePlaybackProgress(new PropertiesStorage(), settings, vocabularyKey);
    }

    /**
     * 销毁所有词库产生的播放记录快照。
     */
    public void clearAllPlaybackProgress() {
        SettingsMapper.clearAllPlaybackProgress(new PropertiesStorage());
    }

    /**
     * 生成包含所有可用词库历史进度摘要的文本行数组，用于在 UI 的记录面板展示。
     *
     * @return 多行进度字符串
     */
    public String[] playbackRecordLines() {
        return SettingsMapper.playbackRecordLines(new PropertiesStorage(), hasCustomVocabulary());
    }

    /**
     * 获取单一词库的一句进度描述。
     *
     * @param vocabularyKey 词库文件键名
     * @param label         展示的中文别名标签
     * @return 一行进度总结
     */
    public String playbackRecordLine(String vocabularyKey, String label) {
        return SettingsMapper.playbackRecordLine(new PropertiesStorage(), vocabularyKey, label);
    }

    /**
     * 加载当前桌面系统的自定义生词本文件。
     * 如果文件破损或不存在，将优雅地返回一个空集合。
     *
     * @return 完整的用户自定义词汇列表
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
     * 向自定义生词本中追加一个全新的词汇并直接落地。
     * 如果新词汇已经存在（按照拼写判断），则它会覆写掉旧的条目。
     *
     * @param wordEntry 要加入的新词条对象
     */
    public void appendCustomWord(WordEntry wordEntry) {
        List<WordEntry> words = loadCustomWords();
        
        // 删除已经存在的同拼写单词
        words.removeIf(w -> w.getWord().equals(wordEntry.getWord()));
        words.add(wordEntry);
        
        saveCustomWords(words);
    }

    /**
     * 将整个自定义词汇列表写回 JSON 文件，使用美化的格式以方便用户在外部编辑器中查看。
     *
     * @param words 词汇列表
     */
    public void saveCustomWords(List<WordEntry> words) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(CUSTOM_WORDS_FILE), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(words, writer);
        } catch (Exception ignored) {
            // 如果无法打开文件流进行写入，忽略异常
        }
    }

    /**
     * 辅助判断方法：当前的文件名是否命中了约定的自定义生词本特征。
     *
     * @param fileName 候选文件名
     * @return 是否为自定义生词本
     */
    public boolean isCustomVocabulary(String fileName) {
        return CUSTOM_VOCABULARY_FILE_NAME.equals(fileName);
    }

    /**
     * 内部辅助方法：判断用户的设备上是否真的存在非空的自定义词库文件。
     *
     * @return 包含内容则返回 true
     */
    private boolean hasCustomVocabulary() {
        return CUSTOM_WORDS_FILE.exists() && CUSTOM_WORDS_FILE.length() > 0;
    }
}

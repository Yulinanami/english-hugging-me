package me.englishhugging.core.settings;

import me.englishhugging.core.vocabulary.VocabularyCatalog;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用设置转换与读写工具。
 *
 * <p>这个类负责在本地键值对存储（{@link SettingsStorage}）与应用设置对象（{@link AppSettings}）
 * 之间进行数据的互相转换与读写，集中管理所有配置项的读取与保存。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 从存储中加载应用配置
 * AppSettings config = SettingsMapper.load(storage);
 * 
 * // 读取并设置某个特定词库（如 CET-4）的背诵进度
 * SettingsMapper.loadPlaybackProgress(storage, config, "CET-4");
 * </code></pre>
 */
public final class SettingsMapper {

    /** 标识自定义词汇表的统一虚拟名称常量 */
    public static final String CUSTOM_VOCABULARY_FILE_NAME = "自定义词汇";

    /**
     * 私有构造函数，无需实例化。
     */
    private SettingsMapper() {
        // 无需实例化
    }

    /**
     * 把存储中读取到的键值对数据转换为 {@link AppSettings} 配置对象。
     *
     * @param storage 键值对存储对象
     * @return 转换后的应用设置对象
     */
    public static AppSettings load(SettingsStorage storage) {
        AppSettings s = new AppSettings();
        
        // 1. 词库路径相关
        String rawVocabPath = storage.getString(SettingsKeys.VOCABULARY_PATH, AppSettings.DEFAULT_VOCABULARY_PATH);
        s.setVocabularyPath(migrateVocabularyPath(rawVocabPath));
        s.setVocabularyFileName(storage.getString(SettingsKeys.VOCABULARY_FILE_NAME, AppSettings.DEFAULT_VOCABULARY_FILE_NAME));
        
        // 2. 枚举类型的解析加载
        String dModeStr = storage.getString(SettingsKeys.DISPLAY_MODE, s.getDisplayMode().name());
        s.setDisplayMode(parseEnum(DisplayMode.class, dModeStr, s.getDisplayMode()));
        
        String oModeStr = storage.getString(SettingsKeys.OVERLAY_MODE, s.getOverlayMode().name());
        s.setOverlayMode(parseEnum(OverlayMode.class, oModeStr, s.getOverlayMode()));
        
        String pModeStr = storage.getString(SettingsKeys.PLAYBACK_MODE, s.getPlaybackMode().name());
        s.setPlaybackMode(parseEnum(PlaybackMode.class, pModeStr, s.getPlaybackMode()));
        
        // 3. 播放行为与背诵进度
        s.setIntervalSeconds(storage.getInt(SettingsKeys.INTERVAL_SECONDS, s.getIntervalSeconds()));
        PlaybackProgress defaults = s.getPlaybackProgress();
        s.setPlaybackProgress(new PlaybackProgress(
                storage.getInt(SettingsKeys.NEXT_WORD_INDEX, defaults.nextWordIndex()),
                storage.getString(SettingsKeys.SHUFFLE_ORDER, defaults.shuffleOrder()),
                storage.getInt(SettingsKeys.SHUFFLE_POSITION, defaults.shufflePosition()),
                storage.getInt(SettingsKeys.RANDOM_PLAYED_COUNT, defaults.randomPlayedCount())
        ));
        s.setStartingPrefix(storage.getString(SettingsKeys.STARTING_PREFIX, s.getStartingPrefix()));
        s.setLoopPlayback(storage.getBoolean(SettingsKeys.LOOP_PLAYBACK, s.isLoopPlayback()));
        
        // 4. 悬浮窗尺寸与位置 (主要针对桌面端)
        s.setX(storage.getDouble(SettingsKeys.X, s.getX()));
        s.setY(storage.getDouble(SettingsKeys.Y, s.getY()));
        s.setWidth(storage.getDouble(SettingsKeys.WIDTH, s.getWidth()));
        s.setHeight(storage.getDouble(SettingsKeys.HEIGHT, s.getHeight()));
        s.setResizeMode(storage.getBoolean(SettingsKeys.RESIZE_MODE, s.isResizeMode()));
        
        // 5. 颜色与外观风格
        s.setOpacity(storage.getDouble(SettingsKeys.OPACITY, s.getOpacity()));
        s.setWordColor(storage.getString(SettingsKeys.WORD_COLOR, s.getWordColor()));
        s.setTypeColor(storage.getString(SettingsKeys.TYPE_COLOR, s.getTypeColor()));
        s.setTranslationColor(storage.getString(SettingsKeys.TRANSLATION_COLOR, s.getTranslationColor()));
        s.setPhraseColor(storage.getString(SettingsKeys.PHRASE_COLOR, s.getPhraseColor()));
        s.setWordFontSize(storage.getInt(SettingsKeys.WORD_FONT_SIZE, s.getWordFontSize()));
        s.setDetailFontSize(storage.getInt(SettingsKeys.DETAIL_FONT_SIZE, s.getDetailFontSize()));
        
        // 6. 填空模式设置
        s.setFillBlankMode(storage.getBoolean(SettingsKeys.FILL_BLANK_MODE, s.isFillBlankMode()));
        s.setFillBlankIntervalSeconds(storage.getInt(SettingsKeys.FILL_BLANK_INTERVAL_SECONDS, s.getFillBlankIntervalSeconds()));
        s.setFillBlankHidePhrases(storage.getBoolean(SettingsKeys.FILL_BLANK_HIDE_PHRASES, s.isFillBlankHidePhrases()));
        s.setFillBlankShowTranslation(storage.getBoolean(SettingsKeys.FILL_BLANK_SHOW_TRANSLATION, s.isFillBlankShowTranslation()));
        
        return s;
    }

    /**
     * 将配置对象中的变更保存到本地存储中。
     *
     * @param storage 键值对存储对象
     * @param s       被修改过的应用设置对象
     */
    public static void save(SettingsStorage storage, AppSettings s) {
        // 基础配置
        storage.putString(SettingsKeys.VOCABULARY_PATH, s.getVocabularyPath());
        storage.putString(SettingsKeys.VOCABULARY_FILE_NAME, s.getVocabularyFileName());
        
        // 枚举转换
        storage.putString(SettingsKeys.DISPLAY_MODE, s.getDisplayMode().name());
        storage.putString(SettingsKeys.OVERLAY_MODE, s.getOverlayMode().name());
        storage.putString(SettingsKeys.PLAYBACK_MODE, s.getPlaybackMode().name());
        
        // 状态相关
        storage.putInt(SettingsKeys.INTERVAL_SECONDS, s.getIntervalSeconds());
        PlaybackProgress progress = s.getPlaybackProgress();
        storage.putInt(SettingsKeys.NEXT_WORD_INDEX, progress.nextWordIndex());
        storage.putString(SettingsKeys.SHUFFLE_ORDER, progress.shuffleOrder());
        storage.putInt(SettingsKeys.SHUFFLE_POSITION, progress.shufflePosition());
        storage.putInt(SettingsKeys.RANDOM_PLAYED_COUNT, progress.randomPlayedCount());
        storage.putString(SettingsKeys.STARTING_PREFIX, s.getStartingPrefix());
        storage.putBoolean(SettingsKeys.LOOP_PLAYBACK, s.isLoopPlayback());
        
        // 尺寸位置
        storage.putDouble(SettingsKeys.X, s.getX());
        storage.putDouble(SettingsKeys.Y, s.getY());
        storage.putDouble(SettingsKeys.WIDTH, s.getWidth());
        storage.putDouble(SettingsKeys.HEIGHT, s.getHeight());
        storage.putBoolean(SettingsKeys.RESIZE_MODE, s.isResizeMode());
        
        // 外观样式
        storage.putDouble(SettingsKeys.OPACITY, s.getOpacity());
        storage.putString(SettingsKeys.WORD_COLOR, s.getWordColor());
        storage.putString(SettingsKeys.TYPE_COLOR, s.getTypeColor());
        storage.putString(SettingsKeys.TRANSLATION_COLOR, s.getTranslationColor());
        storage.putString(SettingsKeys.PHRASE_COLOR, s.getPhraseColor());
        storage.putInt(SettingsKeys.WORD_FONT_SIZE, s.getWordFontSize());
        storage.putInt(SettingsKeys.DETAIL_FONT_SIZE, s.getDetailFontSize());
        
        // 填空配置
        storage.putBoolean(SettingsKeys.FILL_BLANK_MODE, s.isFillBlankMode());
        storage.putInt(SettingsKeys.FILL_BLANK_INTERVAL_SECONDS, s.getFillBlankIntervalSeconds());
        storage.putBoolean(SettingsKeys.FILL_BLANK_HIDE_PHRASES, s.isFillBlankHidePhrases());
        storage.putBoolean(SettingsKeys.FILL_BLANK_SHOW_TRANSLATION, s.isFillBlankShowTranslation());
        
        // 提交变更
        storage.commit();
    }

    /**
     * 从本地存储中读取指定词库的背诵进度。
     *
     * @param storage       存储对象
     * @param s             接收进度的设置对象
     * @param vocabularyKey 用于区分词库的唯一键名（如文件名）
     */
    public static void loadPlaybackProgress(SettingsStorage storage, AppSettings s, String vocabularyKey) {
        PlaybackProgress defaults = s.getPlaybackProgress();
        s.setPlaybackProgress(new PlaybackProgress(
                storage.getInt(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX), defaults.nextWordIndex()),
                storage.getString(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_ORDER), defaults.shuffleOrder()),
                storage.getInt(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION), defaults.shufflePosition()),
                storage.getInt(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT), defaults.randomPlayedCount())
        ));
    }

    /**
     * 将当前配置对象中的进度保存到指定词库下。
     *
     * @param storage       存储对象
     * @param s             进度来源的设置对象
     * @param vocabularyKey 词库关联键名
     */
    public static void savePlaybackProgress(SettingsStorage storage, AppSettings s, String vocabularyKey) {
        PlaybackProgress progress = s.getPlaybackProgress();
        storage.putInt(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX), progress.nextWordIndex());
        storage.putString(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_ORDER), progress.shuffleOrder());
        storage.putInt(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION), progress.shufflePosition());
        storage.putInt(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT), progress.randomPlayedCount());

        storage.commit();
    }

    /**
     * 清除存储中记录的所有词库的背诵进度。
     *
     * @param storage 存储对象
     */
    public static void clearAllPlaybackProgress(SettingsStorage storage) {
        for (String key : storage.getAllKeys()) {
            if (key.startsWith("progress.")) {
                storage.remove(key);
            }
        }
        storage.commit();
    }

    /**
     * 为所有的内置及自定义词库生成学习进度文本摘要。
     *
     * @param storage             存储对象
     * @param hasCustomVocabulary 是否存在自定义词汇表文件
     * @return 中文进度描述行的数组
     */
    public static String[] playbackRecordLines(SettingsStorage storage, boolean hasCustomVocabulary) {
        List<String> lines = new ArrayList<>();
        
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            String line = playbackRecordLine(storage, item.fileName(), item.displayName());
            lines.add(line);
        }
        
        if (hasCustomVocabulary) {
            String line = playbackRecordLine(storage, CUSTOM_VOCABULARY_FILE_NAME, CUSTOM_VOCABULARY_FILE_NAME);
            lines.add(line);
        }
        
        return lines.toArray(new String[0]);
    }

    /**
     * 为单个词库生成一行学习进度描述。
     *
     * @param storage       存储对象
     * @param vocabularyKey 词库关联键名
     * @param label         展示的前缀标签名
     * @return 格式化后的进度字符串
     */
    public static String playbackRecordLine(SettingsStorage storage, String vocabularyKey, String label) {
        int nextWordIndex = storage.getInt(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX), 0);
        int shufflePosition = storage.getInt(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION), 0);
        int randomPlayedCount = storage.getInt(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT), 0);
        
        return label + "：顺序播放到第 " + (nextWordIndex + 1) + " 个；随机播放 " + randomPlayedCount + " 个；随机不重复 " + shufflePosition + " 个";
    }

    /**
     * 生成保存特定词库进度的键名字符串（如 "progress.1-初中-顺序.json.nextWordIndex"）。
     */
    private static String progressKey(String vocabularyKey, String key) {
        String safeNamespace = "";
        if (vocabularyKey != null) {
            safeNamespace = vocabularyKey.replace('\\', '/');
        }
        return "progress." + safeNamespace + "." + key;
    }

    /**
     * 解析枚举字符串，解析失败或不存在时返回默认值。
     */
    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    /**
     * 兼容旧版本词库路径，自动将旧路径格式转换为当前标准路径。
     */
    private static String migrateVocabularyPath(String value) {
        if (value == null) {
            return null;
        }
        String migrated = value.replace("english-vocabulary/json/", "vocabulary/");
        migrated = migrated.replace("english-vocabulary\\json\\", "vocabulary\\");
        return migrated;
    }
}

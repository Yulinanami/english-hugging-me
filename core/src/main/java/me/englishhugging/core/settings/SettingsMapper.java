package me.englishhugging.core.settings;

import me.englishhugging.core.vocabulary.VocabularyCatalog;
import java.util.ArrayList;
import java.util.List;

/**
 * 设置数据映射器（Settings Mapper）。
 *
 * <p>该类负责在具体的物理存储引擎（{@link SettingsStorage}）与
 * 内存领域模型（{@link AppSettings}）之间进行数据的双向序列化转换。
 * 集中在此处定义读写的逻辑可以彻底消除大量样板代码。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 加载全局配置
 * AppSettings config = SettingsMapper.load(storage);
 * 
 * // 读取并注入某个特定词库（如 CET-4）的专属播放进度
 * SettingsMapper.loadPlaybackProgress(storage, config, "CET-4");
 * </code></pre>
 */
public final class SettingsMapper {

    /** 标识自定义词汇表的统一虚拟名称常量 */
    public static final String CUSTOM_VOCABULARY_FILE_NAME = "自定义词汇";

    /**
     * 隐藏公共构造函数，确保工具类仅通过静态方法调用。
     */
    private SettingsMapper() {
        // 工具类不可实例化
    }

    /**
     * 将持久化的键值对反序列化成内存中的 {@link AppSettings} 对象。
     * 为提升代码易读性，将复杂的加载过程按逻辑分类处理。
     *
     * @param storage 实现了接口的具体物理存储引擎
     * @return 装载完成的应用程序设置对象
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
        
        // 3. 播放器行为进度
        s.setIntervalSeconds(storage.getInt(SettingsKeys.INTERVAL_SECONDS, s.getIntervalSeconds()));
        s.setNextWordIndex(storage.getInt(SettingsKeys.NEXT_WORD_INDEX, s.getNextWordIndex()));
        s.setShuffleOrder(storage.getString(SettingsKeys.SHUFFLE_ORDER, s.getShuffleOrder()));
        s.setShufflePosition(storage.getInt(SettingsKeys.SHUFFLE_POSITION, s.getShufflePosition()));
        s.setRandomPlayedCount(storage.getInt(SettingsKeys.RANDOM_PLAYED_COUNT, s.getRandomPlayedCount()));
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
        
        // 6. 填空考核模式专项配置
        s.setFillBlankMode(storage.getBoolean(SettingsKeys.FILL_BLANK_MODE, s.isFillBlankMode()));
        s.setFillBlankIntervalSeconds(storage.getInt(SettingsKeys.FILL_BLANK_INTERVAL_SECONDS, s.getFillBlankIntervalSeconds()));
        s.setFillBlankHidePhrases(storage.getBoolean(SettingsKeys.FILL_BLANK_HIDE_PHRASES, s.isFillBlankHidePhrases()));
        s.setFillBlankShowTranslation(storage.getBoolean(SettingsKeys.FILL_BLANK_SHOW_TRANSLATION, s.isFillBlankShowTranslation()));
        
        return s;
    }

    /**
     * 将内存对象中的变更持久化到存储引擎中。
     *
     * @param storage 物理存储引擎
     * @param s       被修改过配置的设置对象
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
        storage.putInt(SettingsKeys.NEXT_WORD_INDEX, s.getNextWordIndex());
        storage.putString(SettingsKeys.SHUFFLE_ORDER, s.getShuffleOrder());
        storage.putInt(SettingsKeys.SHUFFLE_POSITION, s.getShufflePosition());
        storage.putInt(SettingsKeys.RANDOM_PLAYED_COUNT, s.getRandomPlayedCount());
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
     * 单独从存储中提取属于某个具体词库文件的进度状态。
     * 因为每个词库都保留了各自独立的一套进度快照。
     *
     * @param storage       存储引擎
     * @param s             目标写入的配置对象
     * @param vocabularyKey 用于区分词库的唯一键名（如文件名）
     */
    public static void loadPlaybackProgress(SettingsStorage storage, AppSettings s, String vocabularyKey) {
        String keyNextWordIndex = progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX);
        s.setNextWordIndex(storage.getInt(keyNextWordIndex, s.getNextWordIndex()));
        
        String keyShuffleOrder = progressKey(vocabularyKey, SettingsKeys.SHUFFLE_ORDER);
        s.setShuffleOrder(storage.getString(keyShuffleOrder, s.getShuffleOrder()));
        
        String keyShufflePosition = progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION);
        s.setShufflePosition(storage.getInt(keyShufflePosition, s.getShufflePosition()));
        
        String keyRandomCount = progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT);
        s.setRandomPlayedCount(storage.getInt(keyRandomCount, s.getRandomPlayedCount()));
    }

    /**
     * 将当前配置对象中的进度状态单独持久化绑定到某个特定词库下。
     *
     * @param storage       存储引擎
     * @param s             进度来源的配置对象
     * @param vocabularyKey 词库关联键名
     */
    public static void savePlaybackProgress(SettingsStorage storage, AppSettings s, String vocabularyKey) {
        storage.putInt(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX), s.getNextWordIndex());
        storage.putString(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_ORDER), s.getShuffleOrder());
        storage.putInt(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION), s.getShufflePosition());
        storage.putInt(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT), s.getRandomPlayedCount());
        
        storage.commit();
    }

    /**
     * 一键清除用户存储中记录的所有词汇表播放历史与进度。
     *
     * @param storage 存储引擎
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
     * 为所有的内置及自定义词库生成一条表示它们各自学习进度详情的记录摘要。
     *
     * @param storage             存储引擎
     * @param hasCustomVocabulary 是否存在自定义词汇表文件
     * @return 中文进度描述行的数组
     */
    public static String[] playbackRecordLines(SettingsStorage storage, boolean hasCustomVocabulary) {
        List<String> lines = new ArrayList<>();
        
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            String line = playbackRecordLine(storage, item.getFileName(), item.getDisplayName());
            lines.add(line);
        }
        
        if (hasCustomVocabulary) {
            String line = playbackRecordLine(storage, CUSTOM_VOCABULARY_FILE_NAME, CUSTOM_VOCABULARY_FILE_NAME);
            lines.add(line);
        }
        
        return lines.toArray(new String[0]);
    }

    /**
     * 针对单个特定的词汇表生成一条学习进度字符串摘要。
     *
     * @param storage       存储引擎
     * @param vocabularyKey 关联的独立键名
     * @param label         展示的前缀标签名
     * @return 格式化后的信息字符串
     */
    public static String playbackRecordLine(SettingsStorage storage, String vocabularyKey, String label) {
        int nextWordIndex = storage.getInt(progressKey(vocabularyKey, SettingsKeys.NEXT_WORD_INDEX), 0);
        int shufflePosition = storage.getInt(progressKey(vocabularyKey, SettingsKeys.SHUFFLE_POSITION), 0);
        int randomPlayedCount = storage.getInt(progressKey(vocabularyKey, SettingsKeys.RANDOM_PLAYED_COUNT), 0);
        
        return label + "：顺序播放到第 " + (nextWordIndex + 1) + " 个；随机播放 " + randomPlayedCount + " 个；随机不重复 " + shufflePosition + " 个";
    }

    /**
     * 辅助方法：生成一个带命名空间的复合键，用于存储该特定词库的进度。
     */
    private static String progressKey(String vocabularyKey, String key) {
        String safeNamespace = "";
        if (vocabularyKey != null) {
            safeNamespace = vocabularyKey.replace('\\', '/');
        }
        return "progress." + safeNamespace + "." + key;
    }

    /**
     * 辅助方法：容错型地将字符串反向解析为强类型枚举，失败时自动返回后备值。
     */
    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    /**
     * 辅助方法：提供向下兼容，迁移旧版本中路径名称带有冗余文件夹的脏数据。
     */
    private static String migrateVocabularyPath(String value) {
        if (value == null) {
            return null;
        }
        // 清理老旧配置的冗余前缀
        String migrated = value.replace("english-vocabulary/json/", "vocabulary/");
        migrated = migrated.replace("english-vocabulary\\json\\", "vocabulary\\");
        return migrated;
    }
}

package me.englishhugging.core.settings;

/**
 * 跨平台配置存储的统一读写接口。
 *
 * <p>这个接口是对底层物理存储（如 Android 的 {@code SharedPreferences} 或 Desktop 的 {@code Properties} 文件）的抽象。
 * 通过提供一套统一的键值对存取方法，使得核心业务逻辑层（Core）完全无需关心运行平台的存储细节。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 在 Core 模块读取配置
 * SettingsStorage storage = getPlatformStorage();
 * int fontSize = storage.getInt("wordFontSize", 24);
 * 
 * // 更新并持久化
 * storage.putInt("wordFontSize", 32);
 * storage.commit();
 * </code></pre>
 */
public interface SettingsStorage {

    /**
     * 读取字符串类型的配置值。
     *
     * @param key          配置的唯一键名
     * @param defaultValue 如果键名不存在时返回的默认后备值
     * @return 存储的字符串，或默认值
     */
    String getString(String key, String defaultValue);

    /**
     * 读取整型数字的配置值。
     *
     * @param key          配置的唯一键名
     * @param defaultValue 默认后备值
     * @return 存储的整型数值，或默认值
     */
    int getInt(String key, int defaultValue);

    /**
     * 读取双精度浮点型的配置值。
     *
     * @param key          配置的唯一键名
     * @param defaultValue 默认后备值
     * @return 存储的浮点数值，或默认值
     */
    double getDouble(String key, double defaultValue);

    /**
     * 读取布尔类型的配置值。
     *
     * @param key          配置的唯一键名
     * @param defaultValue 默认后备值
     * @return 存储的布尔值，或默认值
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * 写入或更新一个字符串配置项。
     *
     * @param key   配置的唯一键名
     * @param value 将要保存的字符串内容
     */
    void putString(String key, String value);

    /**
     * 写入或更新一个整型配置项。
     *
     * @param key   配置的唯一键名
     * @param value 将要保存的整型数值
     */
    void putInt(String key, int value);

    /**
     * 写入或更新一个双精度浮点型配置项。
     *
     * @param key   配置的唯一键名
     * @param value 将要保存的浮点数值
     */
    void putDouble(String key, double value);

    /**
     * 写入或更新一个布尔类型配置项。
     *
     * @param key   配置的唯一键名
     * @param value 将要保存的布尔值
     */
    void putBoolean(String key, boolean value);

    /**
     * 移除存储中指定的键值对。
     *
     * @param key 需要被删除的配置键名
     */
    void remove(String key);

    /**
     * 获取当前存储中的所有键名。
     * 这主要用于批量清理（如清除所有动态生成的进度 key）。
     *
     * @return 一个可迭代的字符串集合，包含所有的 Keys
     */
    Iterable<String> getAllKeys();

    /**
     * 提交所有在 {@code put} 或 {@code remove} 操作中产生的修改。
     * 这个方法确保数据被实际写入到底层持久化物理介质中。
     * 如果在多次 put 之后没有调用此方法，修改可能会丢失。
     */
    void commit();
}

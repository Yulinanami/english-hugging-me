package me.englishhugging.core.vocabulary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 单词本目录，包含预定义的内置英语词汇表列表。
 *
 * <p>这个类充当词库的注册表，枚举了程序自带的 JSON 词库文件以及其面向用户展示的名字。
 * UI 可以直接获取这里的列表去展示下拉框选项。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 获取所有内置词汇表并在控制台打印
 * for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
 *     System.out.println(item.getDisplayName() + " -> " + item.getFileName());
 * }
 * </code></pre>
 */
public final class VocabularyCatalog {

    /** 词库文件所在的基础目录名称（相对于项目的 resources 文件夹或 assets） */
    public static final String BASE_DIRECTORY = "vocabulary";

    /** 内部静态数组，硬编码了所有可用的词库文件及其显示名称 */
    private static final VocabularyItem[] ITEMS = {
            new VocabularyItem("1-初中-顺序.json", "1-初中-顺序.json"),
            new VocabularyItem("2-高中-顺序.json", "2-高中-顺序.json"),
            new VocabularyItem("3-CET4-顺序.json", "3-CET4-顺序.json"),
            new VocabularyItem("4-CET6-顺序.json", "4-CET6-顺序.json"),
            new VocabularyItem("5-考研-顺序.json", "5-考研-顺序.json"),
            new VocabularyItem("6-托福-顺序.json", "6-托福-顺序.json"),
            new VocabularyItem("7-SAT-顺序.json", "7-SAT-顺序.json")
    };

    /**
     * 私有构造函数，防止被实例化，因为这是一个纯静态工具类。
     */
    private VocabularyCatalog() {
        // 工具类不需要被实例化
    }

    /**
     * 获取所有内置词库条目的不可变列表。
     *
     * @return 包含 {@link VocabularyItem} 的不可变 {@link List}
     */
    public static List<VocabularyItem> items() {
        return Collections.unmodifiableList(Arrays.asList(ITEMS));
    }

    /**
     * 获取所有内置词库文件的文件名数组。
     * 适用于只需要提取文件路径而不需要显示名称的底层逻辑。
     *
     * @return 字符串数组，内容为词库的文件名
     */
    public static String[] fileNames() {
        String[] fileNames = new String[ITEMS.length];
        for (int i = 0; i < ITEMS.length; i++) {
            fileNames[i] = ITEMS[i].getFileName();
        }
        return fileNames;
    }

    /**
     * 内部静态不可变类，用于封装单个词库的信息（显示名和文件名）。
     */
    public static final class VocabularyItem {
        private final String displayName;
        private final String fileName;

        /**
         * 构造一个新的词库信息节点。
         *
         * @param displayName 面向用户展示的名字
         * @param fileName    实际存储在文件系统中的文件名
         */
        private VocabularyItem(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        /**
         * 获取人类可读的显示名称。
         *
         * @return 显示名字符串
         */
        public String getDisplayName() {
            return this.displayName;
        }

        /**
         * 获取真实的文件名。
         *
         * @return 文件名字符串
         */
        public String getFileName() {
            return this.fileName;
        }
    }
}

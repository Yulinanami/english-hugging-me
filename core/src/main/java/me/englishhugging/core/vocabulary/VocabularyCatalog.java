package me.englishhugging.core.vocabulary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 内置词库目录。
 *
 * <p>定义程序内置的 JSON 词库文件列表及其显示名称。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 获取所有内置词汇表并在控制台打印
 * for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
 *     System.out.println(item.displayName() + " -> " + item.fileName());
 * }
 * </code></pre>
 */
public final class VocabularyCatalog {

    /** 词库文件所在的基础目录名称 */
    public static final String BASE_DIRECTORY = "vocabulary";

    /** 内置词库列表 */
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
     * 私有构造函数，无需实例化。
     */
    private VocabularyCatalog() {
        // 无需实例化
    }

    /**
     * 获取所有内置词库条目列表。
     *
     * @return 包含所有 {@link VocabularyItem} 的只读列表
     */
    public static List<VocabularyItem> items() {
        return Collections.unmodifiableList(Arrays.asList(ITEMS));
    }

    /**
     * 获取所有内置词库文件的文件名数组。
     *
     * @return 字符串数组，内容为词库的文件名
     */
    public static String[] fileNames() {
        String[] fileNames = new String[ITEMS.length];
        for (int i = 0; i < ITEMS.length; i++) {
            fileNames[i] = ITEMS[i].fileName();
        }
        return fileNames;
    }

    /**
     * 内置词库信息。
     *
     * @param displayName 词库显示名称
     * @param fileName    词库文件名
     */
    public record VocabularyItem(String displayName, String fileName) {
    }
}

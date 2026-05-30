package me.englishhugging.core.vocabulary;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.englishhugging.core.model.WordEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 将 JSON 文件反序列化为 {@link WordEntry} 对象的加载工具类。
 *
 * <p>该类封装了 Gson 库的调用，专门用于从文件系统或输入流中安全地读取并解析 JSON 格式的词库。
 * 解析过程中会过滤掉不合法或空缺的单词，并进行防卫性拷贝。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 通过路径加载词库
 * VocabularyJsonLoader loader = new VocabularyJsonLoader();
 * List&lt;WordEntry&gt; words = loader.load(Paths.get("vocab.json"));
 * System.out.println("成功加载了 " + words.size() + " 个单词");
 * </code></pre>
 */
public final class VocabularyJsonLoader {

    /** 
     * Gson 解析时需要明确泛型集合的具体类型信息 
     */
    private static final Type WORD_LIST_TYPE = new TypeToken<List<WordEntry>>() { }.getType();

    /** 
     * JSON 解析核心对象 
     */
    private final Gson gson = new Gson();

    /**
     * 默认构造函数。
     */
    public VocabularyJsonLoader() {
        // 无需额外初始化
    }

    /**
     * 从指定的文件系统路径读取并解析 JSON 词库。
     *
     * @param path 本地文件路径
     * @return 解析后的 {@link WordEntry} 列表
     * @throws IOException 如果文件不存在或读取失败时抛出异常
     */
    public List<WordEntry> load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    /**
     * 从输入流（如 Android Assets 或 Desktop Resources）读取并解析 JSON 词库。
     *
     * @param inputStream 数据输入流
     * @return 解析后的 {@link WordEntry} 列表
     * @throws IOException 如果流读取失败时抛出异常
     */
    public List<WordEntry> load(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    /**
     * 核心加载逻辑，从 Reader 读取文本并反序列化，执行必要的清洗与拷贝操作。
     *
     * @param reader 数据读取器
     * @return 一个不可变的 {@link WordEntry} 集合
     * @throws IOException 如果 GSON 解析遇到 IO 异常
     */
    public List<WordEntry> load(Reader reader) throws IOException {
        List<WordEntry> parsed = this.gson.fromJson(reader, WORD_LIST_TYPE);
        
        // 提前返回：如果 JSON 文件为空或解析不出集合结构
        if (parsed == null) {
            return Collections.emptyList();
        }

        List<WordEntry> entries = new ArrayList<>();
        
        for (WordEntry entry : parsed) {
            // 过滤无效数据：单词对象为空，或单词字符串为空白
            if (entry == null) {
                continue;
            }
            if (entry.getWord() == null) {
                continue;
            }
            if (entry.getWord().trim().length() == 0) {
                continue;
            }
            
            // 加入集合前执行防卫性拷贝，阻断外部对原词条内部属性的意外篡改
            entries.add(entry.defensiveCopy());
        }
        
        return Collections.unmodifiableList(entries);
    }
}

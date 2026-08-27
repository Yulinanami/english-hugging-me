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
 * JSON 词库文件加载工具。
 *
 * <p>从本地文件或输入流中读取并解析 JSON 格式词库，自动过滤空白与无效条目。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 通过路径读取并解析词库
 * VocabularyJsonLoader loader = new VocabularyJsonLoader();
 * List&lt;WordEntry&gt; words = loader.load(Paths.get("vocab.json"));
 * System.out.println("成功加载了 " + words.size() + " 个单词");
 * </code></pre>
 */
public final class VocabularyJsonLoader {

    /** 单词列表类型标记 */
    private static final Type WORD_LIST_TYPE = new TypeToken<List<WordEntry>>() { }.getType();

    /** JSON 数据转换工具 */
    private final Gson gson = new Gson();

    /**
     * 创建词库加载工具。
     */
    public VocabularyJsonLoader() {
        // 无需额外初始化
    }

    /**
     * 从本地文件路径读取并解析词库。
     *
     * @param path 本地文件路径
     * @return 解析出的单词列表
     * @throws IOException 如果文件不存在或读取失败
     */
    public List<WordEntry> load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    /**
     * 从输入流（如资源文件或应用内置资源）读取并解析词库。
     *
     * @param inputStream 数据输入流
     * @return 解析出的单词列表
     * @throws IOException 如果读取流失败
     */
    public List<WordEntry> load(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    /**
     * 从 Reader 读取字符并解析为单词列表，同时过滤掉无效数据。
     *
     * @param reader 字符输入流读取对象
     * @return 只读的单词列表
     */
    public List<WordEntry> load(Reader reader) {
        List<WordEntry> parsed = this.gson.fromJson(reader, WORD_LIST_TYPE);
        
        // 如果文件内容为空或无法解析为列表，返回空列表
        if (parsed == null) {
            return Collections.emptyList();
        }

        List<WordEntry> entries = new ArrayList<>();
        
        for (WordEntry entry : parsed) {
            // 过滤掉 null 或空白单词
            if (entry == null) {
                continue;
            }
            if (entry.word() == null) {
                continue;
            }
            if (entry.word().trim().isEmpty()) {
                continue;
            }

            // 重新构造一次，确保内部的释义和短语列表不为 null
            entries.add(new WordEntry(entry.word(), entry.translations(), entry.phrases()));
        }
        
        return Collections.unmodifiableList(entries);
    }
}

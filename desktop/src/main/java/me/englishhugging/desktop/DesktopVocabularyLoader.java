package me.englishhugging.desktop;

import me.englishhugging.core.vocabulary.VocabularyJsonLoader;
import me.englishhugging.core.model.WordEntry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 桌面端词库加载工具。
 *
 * <p>根据传入的路径加载词库：优先从本地文件读取，若不存在则从安装包自带的资源中读取。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * List&lt;WordEntry&gt; words = DesktopVocabularyLoader.load("vocabulary/1-初中-顺序.json");
 * </code></pre>
 */
final class DesktopVocabularyLoader {

    /**
     * 私有构造函数，无需实例化。
     */
    private DesktopVocabularyLoader() {
        // 无需实例化
    }

    /**
     * 加载指定路径的词库。
     *
     * @param vocabularyPath 用户配置或默认提供的相对路径
     * @return 成功解析出的词条集合
     * @throws IOException 当本地文件和程序内置资源都找不到目标文件，或解析失败时抛出
     */
    static List<WordEntry> load(String vocabularyPath) throws IOException {
        Path path = Paths.get(vocabularyPath);
        
        // 1. 如果指定的是本地文件路径，优先直接读取本地文件
        if (Files.exists(path)) {
            VocabularyJsonLoader loader = new VocabularyJsonLoader();
            return loader.load(path);
        }
        
        // 2. 如果本地文件不存在，则从程序内置资源（jar 包内 resources）中读取
        String resourcePath = "/" + vocabularyPath.replace('\\', '/');
        try (InputStream in = DesktopVocabularyLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("找不到词库：" + vocabularyPath);
            }
            VocabularyJsonLoader loader = new VocabularyJsonLoader();
            return loader.load(in);
        }
    }
}

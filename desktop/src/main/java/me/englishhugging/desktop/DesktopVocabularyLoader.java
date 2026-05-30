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
 * 桌面端的词库加载桥接器。
 *
 * <p>这个类针对桌面端运行环境（如直接在 IDE 中运行，或打包成 fat jar）的特性，
 * 提供了一个能够智能判断路径来源的词库加载策略。
 * 它会优先尝试从物理文件系统读取词库；如果文件不存在，则回退去 classpath 内部的 resources 中寻找。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 传入路径，自动区分是外部物理文件还是内部自带文件
 * List&lt;WordEntry&gt; words = DesktopVocabularyLoader.load("vocabulary/1-初中-顺序.json");
 * </code></pre>
 */
final class DesktopVocabularyLoader {

    /**
     * 阻止工具类被实例化。
     */
    private DesktopVocabularyLoader() {
        // 无需实例化
    }

    /**
     * 智能加载指定路径的词库。
     *
     * @param vocabularyPath 用户配置或默认提供的相对路径
     * @return 成功解析出的词条集合
     * @throws IOException 当物理文件和包内资源都找不到目标文件，或解析失败时抛出
     */
    static List<WordEntry> load(String vocabularyPath) throws IOException {
        Path path = Paths.get(vocabularyPath);
        
        // 策略 1：如果是开发环境下或被用户导出的纯外置物理文件，优先走系统 IO
        if (Files.exists(path)) {
            VocabularyJsonLoader loader = new VocabularyJsonLoader();
            return loader.load(path);
        }
        
        // 策略 2：针对生产环境打包后的情况，去 jar 包内的 resources 下寻找
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

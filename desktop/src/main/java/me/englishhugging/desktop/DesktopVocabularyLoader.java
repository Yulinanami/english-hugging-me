package me.englishhugging.desktop;

import me.englishhugging.core.vocabulary.VocabularyJsonLoader;
import me.englishhugging.core.model.WordEntry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

final class DesktopVocabularyLoader {
    private DesktopVocabularyLoader() {}

    static List<WordEntry> load(String vocabularyPath) throws IOException {
        Path path = Paths.get(vocabularyPath);
        if (Files.exists(path)) return new VocabularyJsonLoader().load(path);
        try (InputStream in = DesktopVocabularyLoader.class.getResourceAsStream("/" + vocabularyPath.replace('\\', '/'))) {
            if (in == null) throw new IOException("找不到词库：" + vocabularyPath);
            return new VocabularyJsonLoader().load(in);
        }
    }
}

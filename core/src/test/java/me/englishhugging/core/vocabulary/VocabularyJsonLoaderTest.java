package me.englishhugging.core.vocabulary;

import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.DisplayMode;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VocabularyJsonLoaderTest {
    @Test
    void loadsDefaultJuniorVocabulary() throws Exception {
        List<WordEntry> entries = new VocabularyJsonLoader().load(
                Paths.get("vocabulary", "1-初中-顺序.json")
        );

        assertEquals(3223, entries.size());
        assertEquals("ability", entries.get(0).getWord());
        assertNotNull(entries.get(0).getTranslations());
        assertFalse(entries.get(0).getTranslations().isEmpty());
    }
}

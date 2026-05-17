package me.englishhugging.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VocabularyJsonLoaderTest {
    @Test
    void loadsDefaultJuniorVocabulary() throws Exception {
        List<WordEntry> entries = new VocabularyJsonLoader().load(
                Paths.get("english-vocabulary", "json", "1-初中-顺序.json")
        );

        assertEquals(3223, entries.size());
        assertEquals("ability", entries.get(0).getWord());
        assertFalse(entries.get(0).toDisplayText(DisplayMode.WORD_WITH_TRANSLATION).isEmpty());
    }
}

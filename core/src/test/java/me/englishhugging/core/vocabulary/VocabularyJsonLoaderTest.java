package me.englishhugging.core.vocabulary;

import me.englishhugging.core.model.WordEntry;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VocabularyJsonLoaderTest {
    @Test
    void loadsDefaultJuniorVocabulary() throws Exception {
        List<WordEntry> entries = new VocabularyJsonLoader().load(
                Paths.get("vocabulary", "1-初中-顺序.json")
        );

        assertEquals(3223, entries.size());
        assertEquals("ability", entries.get(0).word());
        assertNotNull(entries.get(0).translations());
        assertFalse(entries.get(0).translations().isEmpty());
    }

    @Test
    void normalizesMissingPhraseList() throws Exception {
        List<WordEntry> entries = new VocabularyJsonLoader().load(new StringReader(
                "[{\"word\":\"abandonment\",\"translations\":[{\"translation\":\"放弃\",\"type\":\"n\"}]}]"
        ));

        assertEquals(1, entries.size());
        assertNotNull(entries.get(0).phrases());
        assertTrue(entries.get(0).phrases().isEmpty());
    }
}

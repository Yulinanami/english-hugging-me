package me.englishhugging.core.display;

import me.englishhugging.core.model.WordDisplaySegment;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.DisplayMode;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import me.englishhugging.core.model.Translation;
import me.englishhugging.core.model.Phrase;

class WordDisplayFormatterTest {

    private final WordDisplayFormatter formatter = new WordDisplayFormatter();
    private final WordEntry wordEntry = new WordEntry("apple", 
            Collections.singletonList(new Translation("苹果", "n")), 
            Collections.singletonList(new Phrase("an apple a day", "每天一个苹果")));

    @Test
    void testFormatWordOnly() {
        List<WordDisplaySegment> segments = formatter.format(wordEntry, DisplayMode.WORD_ONLY);
        assertEquals(1, segments.size());
        assertEquals("apple", segments.get(0).getText());
        assertEquals(WordDisplaySegment.Type.WORD, segments.get(0).getType());
    }

    @Test
    void testFormatWordAndTranslation() {
        List<WordDisplaySegment> segments = formatter.format(wordEntry, DisplayMode.WORD_WITH_TRANSLATION);
        // Expected: apple \n n.  苹果
        assertEquals(4, segments.size());
        assertEquals("apple", segments.get(0).getText());
        assertEquals(WordDisplaySegment.Type.LINE_BREAK, segments.get(1).getType());
        assertEquals("n. ", segments.get(2).getText());
        assertEquals(WordDisplaySegment.Type.TYPE, segments.get(2).getType());
        assertEquals("苹果", segments.get(3).getText());
        assertEquals(WordDisplaySegment.Type.TRANSLATION, segments.get(3).getType());
    }

    @Test
    void testFormatHideTranslation() {
        List<WordDisplaySegment> segments = formatter.format(wordEntry, DisplayMode.WORD_WITH_TRANSLATION, false, true);
        assertEquals(1, segments.size());
        assertEquals("apple", segments.get(0).getText());
    }
}

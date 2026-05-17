package me.englishhugging.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WordDisplayFormatter {
    private static final int PHRASE_DISPLAY_LIMIT = 2;

    public List<WordDisplaySegment> format(WordEntry wordEntry, DisplayMode displayMode) {
        if (wordEntry == null) {
            return Collections.emptyList();
        }

        DisplayMode safeMode = displayMode == null ? DisplayMode.WORD_WITH_TRANSLATION : displayMode;
        List<WordDisplaySegment> segments = new ArrayList<>();
        segments.add(new WordDisplaySegment(WordDisplaySegment.Type.WORD, safe(wordEntry.getWord())));

        if (safeMode == DisplayMode.WORD_ONLY) {
            return segments;
        }

        for (Translation translation : wordEntry.getTranslations()) {
            if (translation == null) {
                continue;
            }
            String type = safe(translation.getType());
            String meaning = safe(translation.getTranslation());
            if (type.length() == 0 && meaning.length() == 0) {
                continue;
            }
            segments.add(new WordDisplaySegment(WordDisplaySegment.Type.LINE_BREAK, "\n"));
            if (type.length() > 0) {
                segments.add(new WordDisplaySegment(WordDisplaySegment.Type.TYPE, type + ". "));
            }
            segments.add(new WordDisplaySegment(WordDisplaySegment.Type.TRANSLATION, meaning));
        }

        if (safeMode == DisplayMode.WORD_WITH_TRANSLATION_AND_PHRASE) {
            int displayed = 0;
            for (Phrase phrase : wordEntry.getPhrases()) {
                if (phrase == null) {
                    continue;
                }
                String phraseText = safe(phrase.getPhrase());
                String phraseTranslation = safe(phrase.getTranslation());
                if (phraseText.length() == 0 && phraseTranslation.length() == 0) {
                    continue;
                }
                segments.add(new WordDisplaySegment(WordDisplaySegment.Type.LINE_BREAK, "\n"));
                segments.add(new WordDisplaySegment(WordDisplaySegment.Type.PHRASE, phraseText));
                if (phraseTranslation.length() > 0) {
                    segments.add(new WordDisplaySegment(WordDisplaySegment.Type.PHRASE_TRANSLATION, "： " + phraseTranslation));
                }
                displayed++;
                if (displayed >= PHRASE_DISPLAY_LIMIT) {
                    break;
                }
            }
        }

        return segments;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

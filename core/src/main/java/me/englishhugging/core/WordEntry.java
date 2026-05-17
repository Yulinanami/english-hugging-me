package me.englishhugging.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WordEntry {
    private static final int PHRASE_DISPLAY_LIMIT = 2;

    private String word;
    private List<Translation> translations;
    private List<Phrase> phrases;

    public WordEntry() {
    }

    public WordEntry(String word, List<Translation> translations, List<Phrase> phrases) {
        this.word = word;
        this.translations = copyTranslations(translations);
        this.phrases = copyPhrases(phrases);
    }

    public WordEntry normalized() {
        return new WordEntry(word, translations, phrases);
    }

    public String getWord() {
        return word;
    }

    public List<Translation> getTranslations() {
        if (translations == null) {
            return Collections.emptyList();
        }
        return translations;
    }

    public List<Phrase> getPhrases() {
        if (phrases == null) {
            return Collections.emptyList();
        }
        return phrases;
    }

    public String toDisplayText(DisplayMode displayMode) {
        DisplayMode safeMode = displayMode == null ? DisplayMode.WORD_WITH_TRANSLATION : displayMode;
        StringBuilder builder = new StringBuilder();
        builder.append(word == null ? "" : word.trim());

        if (safeMode == DisplayMode.WORD_ONLY) {
            return builder.toString();
        }

        String translationText = translationsText();
        if (translationText.length() > 0) {
            builder.append('\n').append(translationText);
        }

        if (safeMode == DisplayMode.WORD_WITH_TRANSLATION_AND_PHRASE) {
            String phraseText = phrasesText();
            if (phraseText.length() > 0) {
                builder.append('\n').append(phraseText);
            }
        }

        return builder.toString();
    }

    private String translationsText() {
        List<String> parts = new ArrayList<>();
        for (Translation translation : getTranslations()) {
            if (translation == null) {
                continue;
            }
            String text = translation.toDisplayText();
            if (text.trim().length() > 0) {
                parts.add(text);
            }
        }
        return join(parts, "；");
    }

    private String phrasesText() {
        List<String> parts = new ArrayList<>();
        for (Phrase phrase : getPhrases()) {
            if (phrase == null) {
                continue;
            }
            String text = phrase.toDisplayText();
            if (text.trim().length() > 0) {
                parts.add(text);
            }
            if (parts.size() >= PHRASE_DISPLAY_LIMIT) {
                break;
            }
        }
        return join(parts, "\n");
    }

    private static String join(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static List<Translation> copyTranslations(List<Translation> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<Phrase> copyPhrases(List<Phrase> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}

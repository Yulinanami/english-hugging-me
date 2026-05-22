package me.englishhugging.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WordEntry {
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

    public WordEntry defensiveCopy() {
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

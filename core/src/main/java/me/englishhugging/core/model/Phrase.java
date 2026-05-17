package me.englishhugging.core.model;

public final class Phrase {
    private String phrase;
    private String translation;

    public Phrase() {
    }

    public Phrase(String phrase, String translation) {
        this.phrase = phrase;
        this.translation = translation;
    }

    public String getPhrase() {
        return phrase;
    }

    public String getTranslation() {
        return translation;
    }

    public String toDisplayText() {
        String safePhrase = phrase == null ? "" : phrase.trim();
        String safeTranslation = translation == null ? "" : translation.trim();
        if (safeTranslation.length() == 0) {
            return safePhrase;
        }
        if (safePhrase.length() == 0) {
            return safeTranslation;
        }
        return safePhrase + "：" + safeTranslation;
    }
}

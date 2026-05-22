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
}

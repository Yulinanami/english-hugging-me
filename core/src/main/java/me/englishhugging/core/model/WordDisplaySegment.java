package me.englishhugging.core.model;

public final class WordDisplaySegment {
    public enum Type {
        WORD,
        TYPE,
        TRANSLATION,
        PHRASE,
        PHRASE_TRANSLATION,
        LINE_BREAK
    }

    private final Type type;
    private final String text;

    public WordDisplaySegment(Type type, String text) {
        this.type = type;
        this.text = text == null ? "" : text;
    }

    public Type getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}

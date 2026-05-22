package me.englishhugging.core.model;

public final class Translation {
    private String translation;
    private String type;

    public Translation() {
    }

    public Translation(String translation, String type) {
        this.translation = translation;
        this.type = type;
    }

    public String getTranslation() {
        return translation;
    }

    public String getType() {
        return type;
    }
}

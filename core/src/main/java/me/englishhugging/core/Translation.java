package me.englishhugging.core;

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

    public String toDisplayText() {
        String safeTranslation = translation == null ? "" : translation.trim();
        String safeType = type == null ? "" : type.trim();
        if (safeType.length() == 0) {
            return safeTranslation;
        }
        if (safeTranslation.length() == 0) {
            return safeType;
        }
        return safeType + ". " + safeTranslation;
    }
}

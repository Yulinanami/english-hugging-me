package me.englishhugging.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class VocabularyCatalog {
    public static final String BASE_DIRECTORY = "vocabulary";

    private static final VocabularyItem[] ITEMS = {
            new VocabularyItem("1-初中-顺序.json", "1-初中-顺序.json"),
            new VocabularyItem("2-高中-顺序.json", "2-高中-顺序.json"),
            new VocabularyItem("3-CET4-顺序.json", "3-CET4-顺序.json"),
            new VocabularyItem("4-CET6-顺序.json", "4-CET6-顺序.json"),
            new VocabularyItem("5-考研-顺序.json", "5-考研-顺序.json"),
            new VocabularyItem("6-托福-顺序.json", "6-托福-顺序.json"),
            new VocabularyItem("7-SAT-顺序.json", "7-SAT-顺序.json")
    };

    private VocabularyCatalog() {
    }

    public static List<VocabularyItem> items() {
        return Collections.unmodifiableList(Arrays.asList(ITEMS));
    }

    public static String[] fileNames() {
        String[] fileNames = new String[ITEMS.length];
        for (int i = 0; i < ITEMS.length; i++) {
            fileNames[i] = ITEMS[i].getFileName();
        }
        return fileNames;
    }

    public static int indexOfFileName(String fileName) {
        for (int i = 0; i < ITEMS.length; i++) {
            if (ITEMS[i].getFileName().equals(fileName)) {
                return i;
            }
        }
        return 0;
    }

    public static final class VocabularyItem {
        private final String displayName;
        private final String fileName;

        private VocabularyItem(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getFileName() {
            return fileName;
        }
    }
}

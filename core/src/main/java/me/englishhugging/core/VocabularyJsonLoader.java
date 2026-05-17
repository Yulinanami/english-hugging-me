package me.englishhugging.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VocabularyJsonLoader {
    private static final Type WORD_LIST_TYPE = new TypeToken<List<WordEntry>>() { }.getType();

    private final Gson gson = new Gson();

    public List<WordEntry> load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    public List<WordEntry> load(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    public List<WordEntry> load(Reader reader) throws IOException {
        List<WordEntry> parsed = gson.fromJson(reader, WORD_LIST_TYPE);
        if (parsed == null) {
            return Collections.emptyList();
        }

        List<WordEntry> entries = new ArrayList<>();
        for (WordEntry entry : parsed) {
            if (entry == null || entry.getWord() == null || entry.getWord().trim().length() == 0) {
                continue;
            }
            entries.add(entry.normalized());
        }
        return Collections.unmodifiableList(entries);
    }
}

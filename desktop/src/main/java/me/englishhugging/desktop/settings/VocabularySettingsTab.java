package me.englishhugging.desktop.settings;

import com.google.gson.GsonBuilder;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.englishhugging.core.model.Phrase;
import me.englishhugging.core.model.Translation;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.vocabulary.VocabularyCatalog;
import me.englishhugging.core.vocabulary.VocabularyJsonLoader;
import me.englishhugging.desktop.ui.DesktopUi;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class VocabularySettingsTab {
    static final String CUSTOM_VOCABULARY_LABEL = "自定义词汇";

    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final Stage owner;
    private final Runnable onVocabularyChanged;
    private ComboBox<String> vocabularyChoice;

    VocabularySettingsTab(AppSettings settings, DesktopSettingsStore settingsStore, Stage owner, Runnable onVocabularyChanged) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.owner = owner;
        this.onVocabularyChanged = onVocabularyChanged;
    }

    ComboBox<String> getVocabularyChoice() { return vocabularyChoice; }

    Node createContent() {
        vocabularyChoice = new ComboBox<>();
        vocabularyChoice.getItems().addAll(VocabularyCatalog.fileNames());
        if (Files.exists(customVocabularyPath())) vocabularyChoice.getItems().add(CUSTOM_VOCABULARY_LABEL);
        String currentChoice = vocabularyChoiceForPath(settings.getVocabularyPath());
        if (!vocabularyChoice.getItems().contains(currentChoice)) vocabularyChoice.getItems().add(currentChoice);
        vocabularyChoice.setValue(currentChoice);
        vocabularyChoice.setPrefWidth(300);
        DesktopUi.styleModernControl(vocabularyChoice);
        vocabularyChoice.setOnAction(e -> applyVocabularyChoice(vocabularyChoice.getValue()));

        Button importBtn = DesktopUi.compactButton("导入");
        importBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("导入 JSON 词库");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File selected = fc.showOpenDialog(owner);
            if (selected != null) {
                String path = selected.getAbsolutePath();
                if (!vocabularyChoice.getItems().contains(path)) vocabularyChoice.getItems().add(path);
                vocabularyChoice.setValue(path);
                applyVocabularyChoice(path);
            }
        });

        Button reloadBtn = DesktopUi.compactButton("重新加载");
        reloadBtn.setOnAction(e -> applyVocabularyChoice(vocabularyChoice.getValue()));

        GridPane vocabGrid = DesktopUi.settingsGrid();
        vocabGrid.add(new Label("词汇本："), 0, 0);
        vocabGrid.add(new HBox(6, vocabularyChoice, importBtn), 1, 0);
        vocabGrid.add(reloadBtn, 1, 1);

        TextField customWord = DesktopUi.compactTextField();
        TextField customType = DesktopUi.compactTextField();
        TextField customMeaning = DesktopUi.compactTextField();
        TextField customPhrase = DesktopUi.compactTextField();
        TextField customPhraseMeaning = DesktopUi.compactTextField();
        TextField customExample = DesktopUi.compactTextField();
        Button addBtn = DesktopUi.compactButton("添加");
        addBtn.setOnAction(e -> addCustomWord(customWord, customType, customMeaning, customPhrase, customPhraseMeaning, customExample));

        GridPane customGrid = DesktopUi.settingsGrid();
        customGrid.add(new Label("单词："), 0, 0); customGrid.add(customWord, 1, 0);
        customGrid.add(new Label("词性："), 0, 1); customGrid.add(customType, 1, 1);
        customGrid.add(new Label("意思："), 0, 2); customGrid.add(customMeaning, 1, 2);
        customGrid.add(new Label("词组："), 0, 3); customGrid.add(customPhrase, 1, 3);
        customGrid.add(new Label("词组意思："), 0, 4); customGrid.add(customPhraseMeaning, 1, 4);
        customGrid.add(new Label("例句："), 0, 5); customGrid.add(customExample, 1, 5);
        customGrid.add(addBtn, 1, 6);

        VBox page = new VBox(10, DesktopUi.groupBox("词汇本", vocabGrid), DesktopUi.groupBox("自定义词汇", customGrid));
        page.setPadding(new Insets(10));
        
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        return scroll;
    }

    void applyVocabularyChoice(String choice) {
        if (choice == null || choice.trim().isEmpty()) return;
        String previousPath = settings.getVocabularyPath();
        String nextPath = vocabularyPathForChoice(choice);
        if (!previousPath.equals(nextPath)) {
            settingsStore.savePlaybackProgress(settings, previousPath);
            settings.setVocabularyPath(nextPath);
            settings.setVocabularyFileName(vocabularyFileNameForChoice(choice));
            settings.resetPlaybackProgress();
            settingsStore.loadPlaybackProgress(settings, nextPath);
        }
        settingsStore.save(settings);
        onVocabularyChanged.run();
    }

    private String vocabularyPathForChoice(String choice) {
        if (CUSTOM_VOCABULARY_LABEL.equals(choice)) return customVocabularyPath().toString();
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (item.getFileName().equals(choice)) return VocabularyCatalog.BASE_DIRECTORY + "/" + item.getFileName();
        }
        return choice;
    }

    private String vocabularyFileNameForChoice(String choice) {
        if (CUSTOM_VOCABULARY_LABEL.equals(choice)) return CUSTOM_VOCABULARY_LABEL;
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (item.getFileName().equals(choice)) return item.getFileName();
        }
        return Paths.get(choice).getFileName().toString();
    }

    String vocabularyChoiceForPath(String value) {
        if (value == null || value.trim().isEmpty()) return AppSettings.DEFAULT_VOCABULARY_FILE_NAME;
        String normalized = value.replace('\\', '/');
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (normalized.equals(item.getFileName())
                    || normalized.equals(VocabularyCatalog.BASE_DIRECTORY + "/" + item.getFileName())
                    || normalized.endsWith("/" + VocabularyCatalog.BASE_DIRECTORY + "/" + item.getFileName())) {
                return item.getFileName();
            }
        }
        if (normalized.equals(customVocabularyPath().toString().replace('\\', '/'))) return CUSTOM_VOCABULARY_LABEL;
        return value;
    }

    static Path customVocabularyPath() {
        return Paths.get(System.getProperty("user.home"), ".english-hugging-me", "custom-vocabulary.json");
    }

    static boolean isBuiltInVocabularyChoice(String choice) {
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (item.getFileName().equals(choice)) return true;
        }
        return false;
    }

    private void addCustomWord(TextField wordField, TextField typeField, TextField meaningField, TextField phraseField, TextField phraseMeaningField, TextField exampleField) {
        String word = wordField.getText().trim();
        if (word.isEmpty()) return;
        try {
            Path path = customVocabularyPath();
            List<WordEntry> words = new ArrayList<>();
            if (Files.exists(path)) words.addAll(new VocabularyJsonLoader().load(path));

            String type = typeField.getText().trim();
            String meaning = meaningField.getText().trim();
            String phrase = phraseField.getText().trim();
            String phraseMeaning = phraseMeaningField.getText().trim();
            String example = exampleField.getText().trim();

            List<Translation> translations = meaning.isEmpty() && type.isEmpty()
                    ? Collections.emptyList()
                    : Collections.singletonList(new Translation(meaning, type));
            List<Phrase> phrases = new ArrayList<>();
            if (!phrase.isEmpty()) phrases.add(new Phrase(phrase, phraseMeaning));
            if (!example.isEmpty()) phrases.add(new Phrase(example, ""));
            words.add(new WordEntry(word, translations, phrases));

            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(words, writer);
            }

            if (!vocabularyChoice.getItems().contains(CUSTOM_VOCABULARY_LABEL)) vocabularyChoice.getItems().add(CUSTOM_VOCABULARY_LABEL);
            vocabularyChoice.setValue(CUSTOM_VOCABULARY_LABEL);
            applyVocabularyChoice(CUSTOM_VOCABULARY_LABEL);
            wordField.clear(); typeField.clear(); meaningField.clear(); phraseField.clear(); phraseMeaningField.clear(); exampleField.clear();
        } catch (Exception e) {
            System.err.println("Failed to add custom word: " + e.getMessage());
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("添加失败");
            alert.setHeaderText("无法保存自定义词汇");
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }
}

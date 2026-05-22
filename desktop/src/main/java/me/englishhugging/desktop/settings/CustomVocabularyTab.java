package me.englishhugging.desktop.settings;

import com.google.gson.GsonBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import me.englishhugging.core.model.Phrase;
import me.englishhugging.core.model.Translation;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.vocabulary.VocabularyJsonLoader;
import me.englishhugging.desktop.ui.DesktopUi;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CustomVocabularyTab {
    private final Runnable onVocabularyChanged;
    private final ObservableList<WordItem> wordItems = FXCollections.observableArrayList();
    private TableView<WordItem> tableView;

    CustomVocabularyTab(Runnable onVocabularyChanged) {
        this.onVocabularyChanged = onVocabularyChanged;
    }

    Node createContent() {
        TextField customWord = DesktopUi.compactTextField();
        TextField customType = DesktopUi.compactTextField();
        TextField customMeaning = DesktopUi.compactTextField();
        TextField customPhrase = DesktopUi.compactTextField();
        TextField customPhraseMeaning = DesktopUi.compactTextField();
        TextField customExample = DesktopUi.compactTextField();
        Button addBtn = DesktopUi.compactButton("保存单词");
        addBtn.setOnAction(e -> addCustomWord(customWord, customType, customMeaning, customPhrase, customPhraseMeaning, customExample));

        GridPane customGrid = DesktopUi.settingsGrid();
        customGrid.add(new Label("单词："), 0, 0); customGrid.add(customWord, 1, 0);
        customGrid.add(new Label("词性："), 0, 1); customGrid.add(customType, 1, 1);
        customGrid.add(new Label("意思："), 0, 2); customGrid.add(customMeaning, 1, 2);
        customGrid.add(new Label("词组："), 0, 3); customGrid.add(customPhrase, 1, 3);
        customGrid.add(new Label("词组意思："), 0, 4); customGrid.add(customPhraseMeaning, 1, 4);
        customGrid.add(new Label("例句："), 0, 5); customGrid.add(customExample, 1, 5);
        customGrid.add(addBtn, 1, 6);

        tableView = new TableView<>();
        tableView.setItems(wordItems);
        TableColumn<WordItem, String> wordCol = new TableColumn<>("单词");
        wordCol.setCellValueFactory(new PropertyValueFactory<>("word"));
        wordCol.setPrefWidth(120);
        
        TableColumn<WordItem, String> meaningCol = new TableColumn<>("释义");
        meaningCol.setCellValueFactory(new PropertyValueFactory<>("meaning"));
        meaningCol.setPrefWidth(120);

        TableColumn<WordItem, String> phraseCol = new TableColumn<>("词组");
        phraseCol.setCellValueFactory(new PropertyValueFactory<>("phrase"));
        phraseCol.setPrefWidth(100);

        TableColumn<WordItem, String> phraseMeaningCol = new TableColumn<>("词组意思");
        phraseMeaningCol.setCellValueFactory(new PropertyValueFactory<>("phraseMeaning"));
        phraseMeaningCol.setPrefWidth(100);

        TableColumn<WordItem, String> exampleCol = new TableColumn<>("例句");
        exampleCol.setCellValueFactory(new PropertyValueFactory<>("example"));
        exampleCol.setPrefWidth(120);

        tableView.getColumns().add(wordCol);
        tableView.getColumns().add(meaningCol);
        tableView.getColumns().add(phraseCol);
        tableView.getColumns().add(phraseMeaningCol);
        tableView.getColumns().add(exampleCol);
        tableView.setPrefHeight(200);
        DesktopUi.styleModernControl(tableView);

        Button editBtn = DesktopUi.compactButton("编辑选中");
        editBtn.setOnAction(e -> {
            WordItem selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                customWord.setText(selected.getEntry().getWord());
                if (!selected.getEntry().getTranslations().isEmpty()) {
                    customType.setText(selected.getEntry().getTranslations().get(0).getType());
                    customMeaning.setText(selected.getEntry().getTranslations().get(0).getTranslation());
                } else { customType.clear(); customMeaning.clear(); }
                
                customPhrase.clear(); customPhraseMeaning.clear(); customExample.clear();
                for (Phrase p : selected.getEntry().getPhrases()) {
                    if (p.getTranslation().isEmpty()) { customExample.setText(p.getPhrase()); }
                    else { customPhrase.setText(p.getPhrase()); customPhraseMeaning.setText(p.getTranslation()); }
                }
            }
        });

        Button deleteBtn = DesktopUi.compactButton("删除选中");
        deleteBtn.setOnAction(e -> {
            WordItem selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteCustomWord(selected);
            }
        });

        HBox tableControls = new HBox(8, editBtn, deleteBtn);
        tableControls.setPadding(new Insets(5, 0, 0, 0));

        VBox listContainer = new VBox(5, tableView, tableControls);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        VBox page = new VBox(10, DesktopUi.groupBox("添加自定义词汇", customGrid), DesktopUi.groupBox("已添加列表", listContainer));
        page.setPadding(new Insets(10));
        
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        
        loadCustomWords();
        return scroll;
    }

    private void loadCustomWords() {
        wordItems.clear();
        Path path = VocabularySettingsTab.customVocabularyPath();
        if (Files.exists(path)) {
            try {
                List<WordEntry> words = new VocabularyJsonLoader().load(path);
                for (WordEntry word : words) {
                    String meaning = "";
                    if (!word.getTranslations().isEmpty()) {
                        Translation t = word.getTranslations().get(0);
                        meaning = t.getType() + " " + t.getTranslation();
                    }
                    String phrase = ""; String phraseMeaning = ""; String example = "";
                    for (Phrase p : word.getPhrases()) {
                        if (p.getTranslation().isEmpty()) example = p.getPhrase();
                        else { phrase = p.getPhrase(); phraseMeaning = p.getTranslation(); }
                    }
                    wordItems.add(new WordItem(word.getWord(), meaning, phrase, phraseMeaning, example, word));
                }
            } catch (Exception e) {
                System.err.println("Failed to load custom words: " + e.getMessage());
            }
        }
    }

    private void addCustomWord(TextField wordField, TextField typeField, TextField meaningField, TextField phraseField, TextField phraseMeaningField, TextField exampleField) {
        String word = wordField.getText().trim();
        if (word.isEmpty()) return;
        try {
            Path path = VocabularySettingsTab.customVocabularyPath();
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
            WordEntry newEntry = new WordEntry(word, translations, phrases);
            words.removeIf(w -> w.getWord().equals(word));
            words.add(newEntry);

            saveCustomWords(path, words);
            onVocabularyChanged.run();
            loadCustomWords();
            wordField.clear(); typeField.clear(); meaningField.clear(); phraseField.clear(); phraseMeaningField.clear(); exampleField.clear();
        } catch (Exception e) {
            System.err.println("Failed to add custom word: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("添加失败");
            alert.setHeaderText("无法保存自定义词汇");
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }

    private void deleteCustomWord(WordItem item) {
        try {
            Path path = VocabularySettingsTab.customVocabularyPath();
            if (!Files.exists(path)) return;
            List<WordEntry> words = new ArrayList<>(new VocabularyJsonLoader().load(path));
            words.removeIf(w -> w.getWord().equals(item.getWord()));
            saveCustomWords(path, words);
            onVocabularyChanged.run();
            loadCustomWords();
        } catch (Exception e) {
            System.err.println("Failed to delete custom word: " + e.getMessage());
        }
    }

    private void saveCustomWords(Path path, List<WordEntry> words) throws Exception {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(words, writer);
        }
    }

    public static class WordItem {
        private final String word;
        private final String meaning;
        private final String phrase;
        private final String phraseMeaning;
        private final String example;
        private final WordEntry entry;

        public WordItem(String word, String meaning, String phrase, String phraseMeaning, String example, WordEntry entry) {
            this.word = word;
            this.meaning = meaning;
            this.phrase = phrase;
            this.phraseMeaning = phraseMeaning;
            this.example = example;
            this.entry = entry;
        }

        public String getWord() { return word; }
        public String getMeaning() { return meaning; }
        public String getPhrase() { return phrase; }
        public String getPhraseMeaning() { return phraseMeaning; }
        public String getExample() { return example; }
        public WordEntry getEntry() { return entry; }
    }
}

package me.englishhugging.desktop.settings;

import com.google.gson.GsonBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
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

/** 自定义词库 FXML 页面的表单、表格和本地文件控制器。 */
final class CustomVocabularyTab {
    private final Runnable onVocabularyChanged;
    private final ObservableList<WordItem> wordItems = FXCollections.observableArrayList();

    @FXML
    private TextField customWord;
    @FXML
    private TextField customType;
    @FXML
    private TextField customMeaning;
    @FXML
    private TextField customPhrase;
    @FXML
    private TextField customPhraseMeaning;
    @FXML
    private TextField customExample;
    @FXML
    private TableView<WordItem> tableView;
    @FXML
    private TableColumn<WordItem, String> wordColumn;
    @FXML
    private TableColumn<WordItem, String> meaningColumn;
    @FXML
    private TableColumn<WordItem, String> phraseColumn;
    @FXML
    private TableColumn<WordItem, String> phraseMeaningColumn;
    @FXML
    private TableColumn<WordItem, String> exampleColumn;

    CustomVocabularyTab(Runnable onVocabularyChanged) {
        this.onVocabularyChanged = onVocabularyChanged;
    }

    /** 加载由 FXML 声明的自定义词库页面。 */
    Node createContent() {
        return DesktopUi.loadFxml("/fxml/custom-vocabulary.fxml", this);
    }

    /** FXML 字段注入完成后配置表格列并读取现有自定义词汇。 */
    @FXML
    private void initialize() {
        this.wordColumn.setCellValueFactory(new PropertyValueFactory<>("word"));
        this.meaningColumn.setCellValueFactory(new PropertyValueFactory<>("meaning"));
        this.phraseColumn.setCellValueFactory(new PropertyValueFactory<>("phrase"));
        this.phraseMeaningColumn.setCellValueFactory(
                new PropertyValueFactory<>("phraseMeaning")
        );
        this.exampleColumn.setCellValueFactory(new PropertyValueFactory<>("example"));
        this.tableView.setItems(this.wordItems);
        loadCustomWords();
    }

    /** 把选中的词汇回填到上方表单。 */
    @FXML
    private void editSelectedWord() {
        WordItem selected = this.tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        this.customWord.setText(selected.getEntry().word());
        if (!selected.getEntry().translations().isEmpty()) {
            this.customType.setText(selected.getEntry().translations().get(0).type());
            this.customMeaning.setText(selected.getEntry().translations().get(0).translation());
        } else {
            this.customType.clear();
            this.customMeaning.clear();
        }

        this.customPhrase.clear();
        this.customPhraseMeaning.clear();
        this.customExample.clear();
        for (Phrase phrase : selected.getEntry().phrases()) {
            if (phrase.translation().isEmpty()) {
                this.customExample.setText(phrase.phrase());
            } else {
                this.customPhrase.setText(phrase.phrase());
                this.customPhraseMeaning.setText(phrase.translation());
            }
        }
    }

    /** 删除表格当前选中的词汇。 */
    @FXML
    private void deleteSelectedWord() {
        WordItem selected = this.tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            deleteCustomWord(selected);
        }
    }

    private void loadCustomWords() {
        this.wordItems.clear();
        Path path = VocabularySettingsTab.customVocabularyPath();
        if (!Files.exists(path)) {
            return;
        }

        try {
            List<WordEntry> words = new VocabularyJsonLoader().load(path);
            for (WordEntry word : words) {
                String meaning = "";
                if (!word.translations().isEmpty()) {
                    Translation translation = word.translations().get(0);
                    meaning = translation.type() + " " + translation.translation();
                }

                String phrase = "";
                String phraseMeaning = "";
                String example = "";
                for (Phrase currentPhrase : word.phrases()) {
                    if (currentPhrase.translation().isEmpty()) {
                        example = currentPhrase.phrase();
                    } else {
                        phrase = currentPhrase.phrase();
                        phraseMeaning = currentPhrase.translation();
                    }
                }
                this.wordItems.add(new WordItem(
                        word.word(),
                        meaning,
                        phrase,
                        phraseMeaning,
                        example,
                        word
                ));
            }
        } catch (Exception exception) {
            System.err.println("Failed to load custom words: " + exception.getMessage());
        }
    }

    /** 校验表单并保存或覆盖同名单词。 */
    @FXML
    private void addCustomWord() {
        String word = this.customWord.getText().trim();
        if (word.isEmpty()) {
            return;
        }

        try {
            Path path = VocabularySettingsTab.customVocabularyPath();
            List<WordEntry> words = new ArrayList<>();
            if (Files.exists(path)) {
                words.addAll(new VocabularyJsonLoader().load(path));
            }

            String type = this.customType.getText().trim();
            String meaning = this.customMeaning.getText().trim();
            String phrase = this.customPhrase.getText().trim();
            String phraseMeaning = this.customPhraseMeaning.getText().trim();
            String example = this.customExample.getText().trim();

            List<Translation> translations = meaning.isEmpty() && type.isEmpty()
                    ? Collections.emptyList()
                    : Collections.singletonList(new Translation(meaning, type));
            List<Phrase> phrases = new ArrayList<>();
            if (!phrase.isEmpty()) {
                phrases.add(new Phrase(phrase, phraseMeaning));
            }
            if (!example.isEmpty()) {
                phrases.add(new Phrase(example, ""));
            }

            WordEntry newEntry = new WordEntry(word, translations, phrases);
            words.removeIf(entry -> entry.word().equals(word));
            words.add(newEntry);
            saveCustomWords(path, words);
            this.onVocabularyChanged.run();
            loadCustomWords();
            clearForm();
        } catch (Exception exception) {
            System.err.println("Failed to add custom word: " + exception.getMessage());
            exception.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("添加失败");
            alert.setHeaderText("无法保存自定义词汇");
            alert.setContentText(exception.getMessage());
            alert.show();
        }
    }

    private void clearForm() {
        this.customWord.clear();
        this.customType.clear();
        this.customMeaning.clear();
        this.customPhrase.clear();
        this.customPhraseMeaning.clear();
        this.customExample.clear();
    }

    private void deleteCustomWord(WordItem item) {
        try {
            Path path = VocabularySettingsTab.customVocabularyPath();
            if (!Files.exists(path)) {
                return;
            }
            List<WordEntry> words = new ArrayList<>(new VocabularyJsonLoader().load(path));
            words.removeIf(entry -> entry.word().equals(item.getWord()));
            saveCustomWords(path, words);
            this.onVocabularyChanged.run();
            loadCustomWords();
        } catch (Exception exception) {
            System.err.println("Failed to delete custom word: " + exception.getMessage());
        }
    }

    private void saveCustomWords(Path path, List<WordEntry> words) throws Exception {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(words, writer);
        }
    }

    /** TableView 使用的只读行模型。 */
    public static class WordItem {
        private final String word;
        private final String meaning;
        private final String phrase;
        private final String phraseMeaning;
        private final String example;
        private final WordEntry entry;

        public WordItem(
                String word,
                String meaning,
                String phrase,
                String phraseMeaning,
                String example,
                WordEntry entry
        ) {
            this.word = word;
            this.meaning = meaning;
            this.phrase = phrase;
            this.phraseMeaning = phraseMeaning;
            this.example = example;
            this.entry = entry;
        }

        public String getWord() {
            return this.word;
        }

        public String getMeaning() {
            return this.meaning;
        }

        public String getPhrase() {
            return this.phrase;
        }

        public String getPhraseMeaning() {
            return this.phraseMeaning;
        }

        public String getExample() {
            return this.example;
        }

        public WordEntry getEntry() {
            return this.entry;
        }
    }
}

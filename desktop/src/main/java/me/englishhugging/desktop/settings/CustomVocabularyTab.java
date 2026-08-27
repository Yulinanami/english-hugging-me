package me.englishhugging.desktop.settings;

import atlantafx.base.theme.Styles;
import com.google.gson.GsonBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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

/**
 * 桌面端“自定义词库”页面，支持添加、修改和删除生词。
 *
 * <p>用户录入的生词会自动保存到本地文件（`~/.english-hugging-me/custom-vocabulary.json`）。
 */
public final class CustomVocabularyTab {
    /** 词库变更时的刷新回调 */
    private final Runnable onVocabularyChanged;
    /** 表格数据列表 */
    private final ObservableList<WordItem> wordItems = FXCollections.observableArrayList();

    /** 单词输入框 */
    @FXML
    private TextField customWord;
    /** 词性输入框（如 n. / v. / adj.） */
    @FXML
    private TextField customType;
    /** 中文释义输入框 */
    @FXML
    private TextField customMeaning;
    /** 常用短语输入框 */
    @FXML
    private TextField customPhrase;
    /** 短语释义输入框 */
    @FXML
    private TextField customPhraseMeaning;
    /** 英文例句输入框 */
    @FXML
    private TextField customExample;
    /** 保存生词按钮 */
    @FXML
    private Button saveButton;
    /** 删除选中生词按钮 */
    @FXML
    private Button deleteButton;
    /** 自定义生词列表表格 */
    @FXML
    private TableView<WordItem> tableView;

    /**
     * 创建自定义词库页面。
     *
     * @param onVocabularyChanged 词库变更时的刷新回调
     */
    CustomVocabularyTab(Runnable onVocabularyChanged) {
        this.onVocabularyChanged = onVocabularyChanged;
    }

    /**
     * 加载自定义词库界面。
     *
     * @return 自定义词库界面的根节点
     */
    Node createContent() {
        return DesktopUi.loadFxml("/fxml/custom-vocabulary.fxml", this);
    }

    /**
     * 初始化界面，绑定表格数据并加载已保存的生词。
     */
    @FXML
    private void initialize() {
        this.saveButton.getStyleClass().add(Styles.ACCENT);
        this.deleteButton.getStyleClass().add(Styles.DANGER);
        this.tableView.setItems(this.wordItems);
        loadCustomWords();
    }

    /**
     * 将表格中选中的单词填入上方输入框，方便编辑。
     */
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

    /**
     * 删除表格中当前选中的单词。
     */
    @FXML
    private void deleteSelectedWord() {
        WordItem selected = this.tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            deleteCustomWord(selected);
        }
    }

    /**
     * 从本地文件加载自定义生词列表并显示在表格中。
     */
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

    /**
     * 从输入框读取内容并保存为自定义生词。
     */
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

            WordEntry newEntry = buildWordEntry(word);
            words.removeIf(entry -> entry.word().equals(word));
            words.add(newEntry);
            saveCustomWords(path, words);
            this.onVocabularyChanged.run();
            loadCustomWords();
            clearForm();
        } catch (Exception exception) {
            System.err.println("Failed to add custom word: " + exception.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("添加失败");
            alert.setHeaderText("无法保存自定义词汇");
            alert.setContentText(exception.getMessage());
            alert.show();
        }
    }

    /**
     * 从当前输入框表单中构建一个新的单词条目对象。
     */
    private WordEntry buildWordEntry(String word) {
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

        return new WordEntry(word, translations, phrases);
    }

    /**
     * 清空输入框内容。
     */
    private void clearForm() {
        this.customWord.clear();
        this.customType.clear();
        this.customMeaning.clear();
        this.customPhrase.clear();
        this.customPhraseMeaning.clear();
        this.customExample.clear();
    }

    /**
     * 删除指定的单词并保存到本地文件。
     *
     * @param item 要删除的单词条目
     */
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

    /**
     * 将单词列表保存到本地 JSON 文件。
     *
     * @param path  本地文件路径
     * @param words 单词列表
     * @throws Exception 保存失败时抛出异常
     */
    private void saveCustomWords(Path path, List<WordEntry> words) throws Exception {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(words, writer);
        }
    }

    /**
     * 表格中展示单行生词的数据类。
     */
    public static class WordItem {
        /** 英文单词拼写 */
        private final String word;

        /** 中文释义 */
        private final String meaning;

        /** 常用短语 */
        private final String phrase;

        /** 短语释义 */
        private final String phraseMeaning;

        /** 英文例句 */
        private final String example;

        /** 原始单词条目对象 */
        private final WordEntry entry;

        /**
         * 创建表格行数据。
         *
         * @param word          单词拼写
         * @param meaning       中文释义
         * @param phrase        短语
         * @param phraseMeaning 短语释义
         * @param example       例句
         * @param entry         单词条目对象
         */
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

        /**
         * 获取单词拼写。
         *
         * @return 单词拼写
         */
        public String getWord() {
            return this.word;
        }

        /**
         * 获取中文释义。
         *
         * @return 中文释义
         */
        public String getMeaning() {
            return this.meaning;
        }

        /**
         * 获取短语。
         *
         * @return 短语
         */
        public String getPhrase() {
            return this.phrase;
        }

        /**
         * 获取短语释义。
         *
         * @return 短语释义
         */
        public String getPhraseMeaning() {
            return this.phraseMeaning;
        }

        /**
         * 获取例句。
         *
         * @return 例句
         */
        public String getExample() {
            return this.example;
        }

        /**
         * 获取单词条目对象。
         *
         * @return 单词条目对象
         */
        public WordEntry getEntry() {
            return this.entry;
        }
    }
}

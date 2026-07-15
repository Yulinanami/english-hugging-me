package me.englishhugging.desktop.settings;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.vocabulary.VocabularyCatalog;
import me.englishhugging.desktop.ui.DesktopUi;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 词库设置 FXML 页面的选择、导入和重新加载控制器。 */
final class VocabularySettingsTab {
    static final String CUSTOM_VOCABULARY_LABEL = "自定义词汇";

    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final Stage owner;
    private final Runnable onVocabularyChanged;

    @FXML
    private ComboBox<String> vocabularyChoice;

    VocabularySettingsTab(
            AppSettings settings,
            DesktopSettingsStore settingsStore,
            Stage owner,
            Runnable onVocabularyChanged
    ) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.owner = owner;
        this.onVocabularyChanged = onVocabularyChanged;
    }

    ComboBox<String> getVocabularyChoice() {
        return this.vocabularyChoice;
    }

    /** 加载由 FXML 声明的词库设置页面。 */
    Node createContent() {
        return DesktopUi.loadFxml("/fxml/vocabulary-settings.fxml", this);
    }

    /** FXML 字段注入完成后填充可选词库并绑定选择事件。 */
    @FXML
    private void initialize() {
        this.vocabularyChoice.getItems().addAll(VocabularyCatalog.fileNames());
        if (Files.exists(customVocabularyPath())) {
            this.vocabularyChoice.getItems().add(CUSTOM_VOCABULARY_LABEL);
        }

        String currentChoice = vocabularyChoiceForPath(this.settings.getVocabularyPath());
        if (!this.vocabularyChoice.getItems().contains(currentChoice)) {
            this.vocabularyChoice.getItems().add(currentChoice);
        }
        this.vocabularyChoice.setValue(currentChoice);
        this.vocabularyChoice.setOnAction(
                event -> applyVocabularyChoice(this.vocabularyChoice.getValue())
        );
    }

    /** 打开文件选择器并把用户选中的 JSON 文件切换为当前词库。 */
    @FXML
    private void importVocabulary() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导入 JSON 词库");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON", "*.json")
        );
        File selected = fileChooser.showOpenDialog(this.owner);
        if (selected == null) {
            return;
        }

        String path = selected.getAbsolutePath();
        if (!this.vocabularyChoice.getItems().contains(path)) {
            this.vocabularyChoice.getItems().add(path);
        }
        this.vocabularyChoice.setValue(path);
        applyVocabularyChoice(path);
    }

    /** 使用当前选择重新加载词库。 */
    @FXML
    private void reloadVocabulary() {
        applyVocabularyChoice(this.vocabularyChoice.getValue());
    }

    void applyVocabularyChoice(String choice) {
        if (choice == null || choice.trim().isEmpty()) {
            return;
        }
        String previousPath = this.settings.getVocabularyPath();
        String nextPath = vocabularyPathForChoice(choice);
        if (!previousPath.equals(nextPath)) {
            this.settingsStore.savePlaybackProgress(this.settings, previousPath);
            this.settings.setVocabularyPath(nextPath);
            this.settings.setVocabularyFileName(vocabularyFileNameForChoice(choice));
            this.settings.resetPlaybackProgress();
            this.settingsStore.loadPlaybackProgress(this.settings, nextPath);
        }
        this.settingsStore.save(this.settings);
        this.onVocabularyChanged.run();
    }

    private String vocabularyPathForChoice(String choice) {
        if (CUSTOM_VOCABULARY_LABEL.equals(choice)) {
            return customVocabularyPath().toString();
        }
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (item.fileName().equals(choice)) {
                return VocabularyCatalog.BASE_DIRECTORY + "/" + item.fileName();
            }
        }
        return choice;
    }

    private String vocabularyFileNameForChoice(String choice) {
        if (CUSTOM_VOCABULARY_LABEL.equals(choice)) {
            return CUSTOM_VOCABULARY_LABEL;
        }
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (item.fileName().equals(choice)) {
                return item.fileName();
            }
        }
        return Paths.get(choice).getFileName().toString();
    }

    String vocabularyChoiceForPath(String value) {
        if (value == null || value.trim().isEmpty()) {
            return AppSettings.DEFAULT_VOCABULARY_FILE_NAME;
        }
        String normalized = value.replace('\\', '/');
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (normalized.equals(item.fileName())
                    || normalized.equals(VocabularyCatalog.BASE_DIRECTORY + "/" + item.fileName())
                    || normalized.endsWith(
                            "/" + VocabularyCatalog.BASE_DIRECTORY + "/" + item.fileName()
                    )) {
                return item.fileName();
            }
        }
        if (normalized.equals(customVocabularyPath().toString().replace('\\', '/'))) {
            return CUSTOM_VOCABULARY_LABEL;
        }
        return value;
    }

    static Path customVocabularyPath() {
        return Paths.get(
                System.getProperty("user.home"),
                ".english-hugging-me",
                "custom-vocabulary.json"
        );
    }

    static boolean isBuiltInVocabularyChoice(String choice) {
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (item.fileName().equals(choice)) {
                return true;
            }
        }
        return false;
    }
}

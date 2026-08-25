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

/**
 * 桌面端“词库设置”页面，负责选择内置词库或导入外部词库文件。
 *
 * <p>支持选择内置词库、自定义生词库或导入外部 JSON 词库文件。
 */
final class VocabularySettingsTab {
    /** 自定义词库在下拉选择框中的专用显示名称 */
    static final String CUSTOM_VOCABULARY_LABEL = "自定义词汇";

    /** 应用配置 */
    private final AppSettings settings;
    /** 配置存储 */
    private final DesktopSettingsStore settingsStore;
    /** 设置窗口，用于弹出文件选择框 */
    private final Stage owner;
    /** 词库变更时的刷新回调 */
    private final Runnable onVocabularyChanged;

    /** 词库选择下拉框 */
    @FXML
    private ComboBox<String> vocabularyChoice;

    /**
     * 创建词库设置页面。
     *
     * @param settings            应用配置
     * @param settingsStore       配置存储
     * @param owner               所属窗口
     * @param onVocabularyChanged 词库切换回调
     */
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

    /**
     * 获取词库下拉框。
     *
     * @return 词库下拉框
     */
    ComboBox<String> getVocabularyChoice() {
        return this.vocabularyChoice;
    }

    /**
     * 加载词库设置界面。
     *
     * @return 词库设置界面的根节点
     */
    Node createContent() {
        return DesktopUi.loadFxml("/fxml/vocabulary-settings.fxml", this);
    }

    /**
     * 初始化界面控件，填充内置词库列表并绑定选择事件。
     */
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

    /**
     * 弹出文件选择框，让用户导入外部 JSON 词库文件。
     */
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

    /**
     * 重新加载当前选中的词库。
     */
    @FXML
    private void reloadVocabulary() {
        applyVocabularyChoice(this.vocabularyChoice.getValue());
    }

    /**
     * 将选中的词库应用到当前设置中，保存并通知应用重新加载。
     *
     * @param choice 下拉框中选中的词库名称或外部文件路径
     */
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

    /**
     * 根据下拉框选项获取对应的文件路径。
     *
     * @param choice 下拉框选中的词库名称
     * @return 对应的文件路径
     */
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

    /**
     * 根据下拉框选项获取对应的词库显示文件名。
     *
     * @param choice 下拉框选中的词库名称
     * @return 词库文件名
     */
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

    /**
     * 把保存的文件路径转换成在下拉框中显示的名字。
     *
     * @param value 配置文件中记录的路径字符串
     * @return 匹配到的下拉框选项名称
     */
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

    /**
     * 获取桌面端自定义词库 JSON 文件的保存路径：`~/.english-hugging-me/custom-vocabulary.json`。
     *
     * @return 自定义词库文件的本地保存路径
     */
    static Path customVocabularyPath() {
        return Paths.get(
                System.getProperty("user.home"),
                ".english-hugging-me",
                "custom-vocabulary.json"
        );
    }

    /**
     * 判断某个名字是不是系统内置的官方词库。
     *
     * @param choice 词库名称
     * @return 如果是内置词库之一则返回 true
     */
    static boolean isBuiltInVocabularyChoice(String choice) {
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (item.fileName().equals(choice)) {
                return true;
            }
        }
        return false;
    }
}

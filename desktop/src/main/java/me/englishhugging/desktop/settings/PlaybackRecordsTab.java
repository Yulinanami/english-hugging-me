package me.englishhugging.desktop.settings;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.vocabulary.VocabularyCatalog;
import me.englishhugging.desktop.ui.DesktopUi;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 桌面端“学习记录”页面，展示各词库的背诵进度与清除记录功能。
 *
 * <p>用户可以在此查看每个词库已播放的单词数，并支持一键重置所有进度。
 */
final class PlaybackRecordsTab {
    /** 应用配置 */
    private final AppSettings settings;
    /** 配置存储 */
    private final DesktopSettingsStore settingsStore;
    /** 词库设置页面，用于查询当前选中的词库 */
    private final VocabularySettingsTab vocabularySettingsTab;

    /** 记录列表垂直布局 */
    @FXML
    private VBox recordsBox;

    /**
     * 创建学习记录页面。
     *
     * @param settings              应用配置
     * @param settingsStore         配置存储
     * @param vocabularySettingsTab 词库设置页面
     */
    PlaybackRecordsTab(
            AppSettings settings,
            DesktopSettingsStore settingsStore,
            VocabularySettingsTab vocabularySettingsTab
    ) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.vocabularySettingsTab = vocabularySettingsTab;
    }

    /**
     * 加载学习记录界面。
     *
     * @return 学习记录界面的根节点
     */
    Node createContent() {
        return DesktopUi.loadFxml("/fxml/playback-records.fxml", this);
    }

    /**
     * 初始化界面，加载并显示播放记录。
     */
    @FXML
    private void initialize() {
        refresh();
    }

    /**
     * 弹窗确认后清除所有词库的播放记录。
     */
    @FXML
    private void clearRecords() {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "确定要清除所有播放记录吗？这将使所有词汇本从头开始播放。"
        );
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK) {
                this.settings.resetPlaybackProgress();
                this.settingsStore.clearAllPlaybackProgress();
                refresh();
            }
        });
    }

    /**
     * 重新读取各词库进度并更新列表显示。
     */
    void refresh() {
        if (this.recordsBox == null) {
            return;
        }
        this.recordsBox.getChildren().clear();
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            String key = VocabularyCatalog.BASE_DIRECTORY + "/" + item.fileName();
            this.recordsBox.getChildren().add(new Label(
                    this.settingsStore.playbackRecordLine(key, item.displayName())
            ));
        }

        Path customPath = VocabularySettingsTab.customVocabularyPath();
        if (Files.exists(customPath)) {
            this.recordsBox.getChildren().add(new Label(
                    this.settingsStore.playbackRecordLine(
                            customPath.toString(),
                            VocabularySettingsTab.CUSTOM_VOCABULARY_LABEL
                    )
            ));
        }

        String currentChoice = this.vocabularySettingsTab.vocabularyChoiceForPath(
                this.settings.getVocabularyPath()
        );
        boolean isExternalVocabulary = !VocabularySettingsTab.CUSTOM_VOCABULARY_LABEL.equals(
                currentChoice
        ) && !VocabularySettingsTab.isBuiltInVocabularyChoice(currentChoice);
        if (isExternalVocabulary) {
            this.recordsBox.getChildren().add(new Label(
                    this.settingsStore.playbackRecordLine(
                            this.settings.getVocabularyPath(),
                            currentChoice
                    )
            ));
        }
    }
}

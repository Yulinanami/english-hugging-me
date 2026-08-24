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

/** 播放记录 FXML 页面的内容刷新和清除操作控制器。 */
final class PlaybackRecordsTab {
    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final VocabularySettingsTab vocabularySettingsTab;

    @FXML
    private VBox recordsBox;

    PlaybackRecordsTab(
            AppSettings settings,
            DesktopSettingsStore settingsStore,
            VocabularySettingsTab vocabularySettingsTab
    ) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.vocabularySettingsTab = vocabularySettingsTab;
    }

    /** 加载由 FXML 声明的播放记录页面。 */
    Node createContent() {
        return DesktopUi.loadFxml("/fxml/playback-records.fxml", this);
    }

    /** FXML 字段注入完成后展示当前播放记录。 */
    @FXML
    private void initialize() {
        refresh();
    }

    /** 二次确认后清除所有词库的播放记录。 */
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

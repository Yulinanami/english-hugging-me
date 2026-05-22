package me.englishhugging.desktop.settings;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.vocabulary.VocabularyCatalog;
import me.englishhugging.desktop.ui.DesktopUi;

import java.nio.file.Files;
import java.nio.file.Path;

final class PlaybackRecordsTab {
    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final VocabularySettingsTab vocabularySettingsTab;
    private VBox recordsBox;

    PlaybackRecordsTab(AppSettings settings, DesktopSettingsStore settingsStore, VocabularySettingsTab vocabularySettingsTab) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.vocabularySettingsTab = vocabularySettingsTab;
    }

    Node createContent() {
        recordsBox = new VBox(6);
        recordsBox.setPadding(new Insets(4, 0, 0, 0));
        refresh();
        VBox group = DesktopUi.groupBox("播放记录", recordsBox);

        Button clearButton = DesktopUi.compactButton("清除所有记录");
        clearButton.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-cursor: hand;");
        clearButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定要清除所有播放记录吗？这将使所有词汇本从头开始播放。");
            alert.setHeaderText(null);
            alert.showAndWait().ifPresent(type -> {
                if (type == ButtonType.OK) {
                    settings.resetPlaybackProgress();
                    settingsStore.clearAllPlaybackProgress();
                    refresh();
                }
            });
        });

        VBox page = new VBox(14, group, clearButton);
        page.setPadding(new Insets(10));
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        return scroll;
    }

    void refresh() {
        if (recordsBox == null) return;
        recordsBox.getChildren().clear();
        recordsBox.getChildren().add(new Label("记录各词汇本当前顺序位置和随机播放数量。"));
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            String key = VocabularyCatalog.BASE_DIRECTORY + "/" + item.getFileName();
            recordsBox.getChildren().add(new Label(settingsStore.playbackRecordLine(key, item.getDisplayName())));
        }
        Path customPath = VocabularySettingsTab.customVocabularyPath();
        if (Files.exists(customPath)) {
            recordsBox.getChildren().add(new Label(settingsStore.playbackRecordLine(customPath.toString(), VocabularySettingsTab.CUSTOM_VOCABULARY_LABEL)));
        }
        String currentChoice = vocabularySettingsTab.vocabularyChoiceForPath(settings.getVocabularyPath());
        if (!VocabularySettingsTab.CUSTOM_VOCABULARY_LABEL.equals(currentChoice) && !VocabularySettingsTab.isBuiltInVocabularyChoice(currentChoice)) {
            recordsBox.getChildren().add(new Label(settingsStore.playbackRecordLine(settings.getVocabularyPath(), currentChoice)));
        }
    }
}

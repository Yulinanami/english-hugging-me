package me.englishhugging.desktop;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import me.englishhugging.core.WordScheduler;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.settings.DesktopSettingsPanel;
import me.englishhugging.desktop.settings.DesktopSettingsStore;
import me.englishhugging.desktop.ui.DesktopTrayController;

import java.util.List;

public final class FloatingWordsDesktopApp extends Application {
    private final DesktopSettingsStore settingsStore = new DesktopSettingsStore();

    private AppSettings settings;
    private DesktopOverlayController overlayController;
    private DesktopSettingsPanel settingsPanel;
    private DesktopTrayController trayController;
    private WordScheduler scheduler;

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Platform.setImplicitExit(false);

        settings = settingsStore.load();
        settingsStore.loadPlaybackProgress(settings, settings.getVocabularyPath());

        overlayController = new DesktopOverlayController(settings, settingsStore);
        overlayController.init();

        settingsPanel = new DesktopSettingsPanel(
                settings, settingsStore, overlayController,
                () -> { if (scheduler != null) scheduler.updateIntervalSeconds(settings.getIntervalSeconds()); },
                this::reloadVocabulary
        );
        settingsPanel.init();

        installTrayIcon();
        reloadVocabulary();
        settingsPanel.show();
    }

    @Override
    public void stop() {
        if (scheduler != null) scheduler.stop();
        if (settings != null) settingsStore.save(settings);
        if (overlayController != null) overlayController.close();
        removeTrayIcon();
    }

    private void reloadVocabulary() {
        try {
            startScheduler(DesktopVocabularyLoader.load(settings.getVocabularyPath()));
        } catch (Exception e) {
            showError("词库加载失败", e.getMessage());
            overlayController.showLoadingError();
        }
    }

    private void startScheduler(List<WordEntry> words) {
        if (scheduler != null) scheduler.stop();
        scheduler = new WordScheduler(
                words, settings.getIntervalSeconds(), settings.getPlaybackMode(),
                settings.getNextWordIndex(), settings.getShuffleOrder(),
                settings.getShufflePosition(), settings.getRandomPlayedCount(),
                wordEntry -> Platform.runLater(() -> overlayController.updateCurrentWord(wordEntry)),
                (nextWordIndex, shuffleOrder, shufflePosition, randomPlayedCount) -> {
                    settings.setNextWordIndex(nextWordIndex);
                    settings.setShuffleOrder(shuffleOrder);
                    settings.setShufflePosition(shufflePosition);
                    settings.setRandomPlayedCount(randomPlayedCount);
                    settingsStore.save(settings);
                    settingsStore.savePlaybackProgress(settings, settings.getVocabularyPath());
                    Platform.runLater(() -> settingsPanel.refreshPlaybackRecords());
                }
        );
        scheduler.start();
    }

    private void installTrayIcon() {
        trayController = new DesktopTrayController(
                overlayController.getOverlayStage(), () -> settingsPanel.show(), this::exitApplication
        );
        if (!trayController.install()) settingsPanel.show();
    }

    private void removeTrayIcon() {
        if (trayController != null) { trayController.remove(); trayController = null; }
    }

    private void exitApplication() {
        removeTrayIcon();
        Platform.setImplicitExit(true);
        Platform.exit();
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message == null ? "未知错误" : message);
            alert.showAndWait();
        });
    }
}

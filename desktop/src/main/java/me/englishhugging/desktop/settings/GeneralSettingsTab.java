package me.englishhugging.desktop.settings;

import atlantafx.base.controls.ToggleSwitch;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.DisplayMode;
import me.englishhugging.core.settings.OverlayMode;
import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.ui.DesktopUi;

/** 常规设置 FXML 页面的数据初始化和交互控制器。 */
final class GeneralSettingsTab {
    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final DesktopOverlayController overlayController;
    private final Runnable onSettingsChanged;
    private final Runnable onVocabularyChanged;

    @FXML
    private ComboBox<DisplayMode> displayMode;
    @FXML
    private ComboBox<PlaybackMode> playbackMode;
    @FXML
    private ComboBox<OverlayMode> overlayMode;
    @FXML
    private Spinner<Integer> interval;
    @FXML
    private Slider opacity;
    @FXML
    private TextField startingPrefix;
    /** 使用 AtlantaFX 开关统一布尔设置的视觉效果和切换动画。 */
    @FXML
    private ToggleSwitch loopPlayback;
    @FXML
    private ToggleSwitch fillBlankMode;
    @FXML
    private Spinner<Integer> fillBlankInterval;
    @FXML
    private ToggleSwitch fillBlankHidePhrases;
    @FXML
    private ToggleSwitch fillBlankShowTranslation;

    GeneralSettingsTab(
            AppSettings settings,
            DesktopSettingsStore settingsStore,
            DesktopOverlayController overlayController,
            Runnable onSettingsChanged,
            Runnable onVocabularyChanged
    ) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.overlayController = overlayController;
        this.onSettingsChanged = onSettingsChanged;
        this.onVocabularyChanged = onVocabularyChanged;
    }

    /** 加载由 FXML 声明的常规设置页面。 */
    Node createContent() {
        return DesktopUi.loadFxml("/fxml/general-settings.fxml", this);
    }

    /** FXML 字段注入完成后加载当前设置并绑定即时保存事件。 */
    @FXML
    private void initialize() {
        this.displayMode.getItems().setAll(DisplayMode.values());
        this.displayMode.setValue(this.settings.getDisplayMode());
        this.playbackMode.getItems().setAll(PlaybackMode.values());
        this.playbackMode.setValue(this.settings.getPlaybackMode());
        this.overlayMode.getItems().setAll(OverlayMode.values());
        this.overlayMode.setValue(this.settings.getOverlayMode());

        this.interval.getValueFactory().setValue(this.settings.getIntervalSeconds());
        this.opacity.setValue(this.settings.getOpacity());
        this.startingPrefix.setText(this.settings.getStartingPrefix());
        this.loopPlayback.setSelected(this.settings.isLoopPlayback());
        this.fillBlankMode.setSelected(this.settings.isFillBlankMode());
        this.fillBlankInterval.getValueFactory().setValue(this.settings.getFillBlankIntervalSeconds());
        this.fillBlankHidePhrases.setSelected(this.settings.isFillBlankHidePhrases());
        this.fillBlankShowTranslation.setSelected(this.settings.isFillBlankShowTranslation());

        this.displayMode.setOnAction(event -> {
            this.settings.setDisplayMode(this.displayMode.getValue());
            this.settingsStore.save(this.settings);
            this.overlayController.refreshDisplay();
        });
        this.playbackMode.setOnAction(event -> {
            this.settings.setPlaybackMode(this.playbackMode.getValue());
            this.settings.resetPlaybackProgress();
            this.settingsStore.save(this.settings);
            this.settingsStore.savePlaybackProgress(
                    this.settings,
                    this.settings.getVocabularyPath()
            );
            this.onVocabularyChanged.run();
        });
        this.overlayMode.setOnAction(event -> {
            this.settings.setOverlayMode(this.overlayMode.getValue());
            this.settingsStore.save(this.settings);
            this.overlayController.applyOverlayMode();
        });
        // ToggleSwitch 通过选中属性通知状态变化，修改后立即保存并刷新词库播放状态。
        this.loopPlayback.selectedProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setLoopPlayback(newValue);
            this.settingsStore.save(this.settings);
            this.onVocabularyChanged.run();
        });
        this.startingPrefix.textProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setStartingPrefix(newValue);
            this.settingsStore.save(this.settings);
            this.onVocabularyChanged.run();
        });
        this.interval.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setIntervalSeconds(newValue);
            this.settingsStore.save(this.settings);
            this.onSettingsChanged.run();
        });
        this.opacity.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setOpacity(newValue.doubleValue());
            this.overlayController.getOverlayStage().setOpacity(this.settings.getOpacity());
            this.settingsStore.save(this.settings);
        });
        this.fillBlankMode.selectedProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setFillBlankMode(newValue);
            this.settingsStore.save(this.settings);
            this.onSettingsChanged.run();
        });
        this.fillBlankInterval.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setFillBlankIntervalSeconds(newValue);
            this.settingsStore.save(this.settings);
            this.onSettingsChanged.run();
        });
        this.fillBlankHidePhrases.selectedProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setFillBlankHidePhrases(newValue);
            this.settingsStore.save(this.settings);
            this.onSettingsChanged.run();
        });
        this.fillBlankShowTranslation.selectedProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setFillBlankShowTranslation(newValue);
            this.settingsStore.save(this.settings);
            this.onSettingsChanged.run();
        });
    }
}

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

/**
 * 桌面端“常规设置”页面，负责管理播放模式、间隔时间与填空设置。
 *
 * <p>用户在界面上调整设置后，会自动保存并实时刷新悬浮窗或重启播放。
 */
final class GeneralSettingsTab {
    /** 应用配置 */
    private final AppSettings settings;
    /** 配置存储 */
    private final DesktopSettingsStore settingsStore;
    /** 桌面悬浮窗 */
    private final DesktopOverlayController overlayController;
    /** 播放参数改变时的刷新回调 */
    private final Runnable onSettingsChanged;
    /** 词库或播放模式切换时的刷新回调 */
    private final Runnable onVocabularyChanged;

    /** 单词显示模式下拉框（纯单词/释义/短语） */
    @FXML
    private ComboBox<DisplayMode> displayMode;
    /** 播放顺序下拉框（顺序/随机/乱序） */
    @FXML
    private ComboBox<PlaybackMode> playbackMode;
    /** 悬浮窗交互模式下拉框（可拖拽/鼠标穿透） */
    @FXML
    private ComboBox<OverlayMode> overlayMode;
    /** 单词切换间隔微调框（秒） */
    @FXML
    private Spinner<Integer> interval;
    /** 悬浮窗背景不透明度滑块 */
    @FXML
    private Slider opacity;
    /** 单词首字母筛选前缀输入框 */
    @FXML
    private TextField startingPrefix;
    /** 循环播放开关 */
    @FXML
    private ToggleSwitch loopPlayback;
    /** 字母挖空模式开关 */
    @FXML
    private ToggleSwitch fillBlankMode;
    /** 填空提示恢复间隔微调框（秒） */
    @FXML
    private Spinner<Integer> fillBlankInterval;
    /** 填空时隐藏短语例句开关 */
    @FXML
    private ToggleSwitch fillBlankHidePhrases;
    /** 填空时显示中文释义开关 */
    @FXML
    private ToggleSwitch fillBlankShowTranslation;

    /**
     * 创建常规设置页面。
     *
     * @param settings            应用配置
     * @param settingsStore       配置存储
     * @param overlayController   桌面悬浮窗
     * @param onSettingsChanged   播放参数改变时的刷新回调
     * @param onVocabularyChanged 词库或播放模式切换时的刷新回调
     */
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

    /**
     * 加载常规设置界面。
     *
     * @return 常规设置界面的根节点
     */
    Node createContent() {
        return DesktopUi.loadFxml("/fxml/general-settings.fxml", this);
    }

    /**
     * 初始化界面控件，填充当前设置值并绑定修改事件。
     */
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
        // 开关状态变化后立即保存并刷新词库播放状态
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

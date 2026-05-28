package me.englishhugging.desktop.settings;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.DisplayMode;
import me.englishhugging.core.settings.OverlayMode;
import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.ui.DesktopUi;

final class GeneralSettingsTab {
    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final DesktopOverlayController overlayController;
    private final Runnable onSettingsChanged;
    private final Runnable onVocabularyChanged;

    GeneralSettingsTab(AppSettings settings, DesktopSettingsStore settingsStore,
                       DesktopOverlayController overlayController,
                       Runnable onSettingsChanged, Runnable onVocabularyChanged) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.overlayController = overlayController;
        this.onSettingsChanged = onSettingsChanged;
        this.onVocabularyChanged = onVocabularyChanged;
    }

    Node createContent() {
        ComboBox<DisplayMode> displayMode = enumCombo(DisplayMode.values(), settings.getDisplayMode());
        displayMode.setOnAction(e -> { settings.setDisplayMode(displayMode.getValue()); settingsStore.save(settings); overlayController.refreshDisplay(); });

        ComboBox<PlaybackMode> playbackMode = enumCombo(PlaybackMode.values(), settings.getPlaybackMode());
        playbackMode.setOnAction(e -> {
            settings.setPlaybackMode(playbackMode.getValue());
            settings.resetPlaybackProgress();
            settingsStore.save(settings);
            settingsStore.savePlaybackProgress(settings, settings.getVocabularyPath());
            onVocabularyChanged.run();
        });

        ComboBox<OverlayMode> overlayMode = enumCombo(OverlayMode.values(), settings.getOverlayMode());
        overlayMode.setOnAction(e -> { settings.setOverlayMode(overlayMode.getValue()); settingsStore.save(settings); overlayController.applyOverlayMode(); });

        CheckBox loopPlayback = new CheckBox("循环");
        loopPlayback.setSelected(settings.isLoopPlayback());
        loopPlayback.setOnAction(e -> {
            settings.setLoopPlayback(loopPlayback.isSelected());
            settingsStore.save(settings);
            onVocabularyChanged.run();
        });

        TextField startingPrefix = new TextField(settings.getStartingPrefix());
        startingPrefix.setPrefWidth(140);
        startingPrefix.setPromptText("留空表示播放全部");
        DesktopUi.styleModernControl(startingPrefix);
        startingPrefix.textProperty().addListener((o, ov, nv) -> {
            settings.setStartingPrefix(nv);
            settingsStore.save(settings);
            onVocabularyChanged.run();
        });

        Spinner<Integer> interval = new Spinner<>(2, 300, settings.getIntervalSeconds());
        interval.setEditable(true);
        interval.setPrefWidth(92);
        DesktopUi.styleModernControl(interval);
        interval.valueProperty().addListener((o, ov, nv) -> { settings.setIntervalSeconds(nv); settingsStore.save(settings); onSettingsChanged.run(); });

        Slider opacity = new Slider(0.2, 1.0, settings.getOpacity());
        opacity.setShowTickLabels(true);
        opacity.setShowTickMarks(true);
        opacity.setPrefWidth(240);
        opacity.valueProperty().addListener((o, ov, nv) -> {
            settings.setOpacity(nv.doubleValue());
            overlayController.getOverlayStage().setOpacity(settings.getOpacity());
            settingsStore.save(settings);
        });

        // Fill Blank Mode Settings
        CheckBox fillBlankMode = new CheckBox("开启挖空模式");
        fillBlankMode.setSelected(settings.isFillBlankMode());
        fillBlankMode.setOnAction(e -> { settings.setFillBlankMode(fillBlankMode.isSelected()); settingsStore.save(settings); onSettingsChanged.run(); });

        Spinner<Integer> fillBlankInterval = new Spinner<>(1, 30, settings.getFillBlankIntervalSeconds());
        fillBlankInterval.setEditable(true);
        fillBlankInterval.setPrefWidth(92);
        DesktopUi.styleModernControl(fillBlankInterval);
        fillBlankInterval.valueProperty().addListener((o, ov, nv) -> { settings.setFillBlankIntervalSeconds(nv); settingsStore.save(settings); onSettingsChanged.run(); });

        CheckBox fillBlankHidePhrases = new CheckBox("挖空时关闭短语");
        fillBlankHidePhrases.setSelected(settings.isFillBlankHidePhrases());
        fillBlankHidePhrases.setOnAction(e -> { settings.setFillBlankHidePhrases(fillBlankHidePhrases.isSelected()); settingsStore.save(settings); onSettingsChanged.run(); });

        CheckBox fillBlankShowTranslation = new CheckBox("挖空时显示释义");
        fillBlankShowTranslation.setSelected(settings.isFillBlankShowTranslation());
        fillBlankShowTranslation.setOnAction(e -> { settings.setFillBlankShowTranslation(fillBlankShowTranslation.isSelected()); settingsStore.save(settings); onSettingsChanged.run(); });

        GridPane grid1 = DesktopUi.settingsGrid();
        grid1.add(new Label("显示内容："), 0, 0); grid1.add(displayMode, 1, 0);
        grid1.add(new Label("播放顺序："), 0, 1); grid1.add(playbackMode, 1, 1);
        grid1.add(new Label("悬浮行为："), 0, 2); grid1.add(overlayMode, 1, 2);
        grid1.add(new Label("切换间隔："), 0, 3); grid1.add(new HBox(6, interval, new Label("秒")), 1, 3);
        grid1.add(new Label("透明度："), 0, 4); grid1.add(opacity, 1, 4);

        GridPane grid2 = DesktopUi.settingsGrid();
        grid2.add(new Label("特定前缀："), 0, 0); grid2.add(startingPrefix, 1, 0);
        
        Label loopHint = new Label("开启：无限循环；关闭：播完一遍即停");
        loopHint.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        VBox loopBox = new VBox(4, loopPlayback, loopHint);
        
        grid2.add(new Label("循环模式："), 0, 1); grid2.add(loopBox, 1, 1);

        GridPane grid3 = DesktopUi.settingsGrid();
        grid3.add(new Label("挖空模式："), 0, 0); grid3.add(fillBlankMode, 1, 0);
        grid3.add(new Label("填充间隔："), 0, 1); grid3.add(new HBox(6, fillBlankInterval, new Label("秒")), 1, 1);
        grid3.add(new Label("显示设置："), 0, 2); grid3.add(new VBox(6, fillBlankHidePhrases, fillBlankShowTranslation), 1, 2);

        VBox page = new VBox(14, DesktopUi.groupBox("基础设置", grid1), DesktopUi.groupBox("按前缀播放", grid2), DesktopUi.groupBox("挖空模式设置", grid3));
        page.setPadding(new Insets(10));
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        return scroll;
    }

    private <T extends Enum<T>> ComboBox<T> enumCombo(T[] values, T selected) {
        ComboBox<T> combo = new ComboBox<>();
        combo.getItems().addAll(values);
        combo.setValue(selected);
        combo.setPrefWidth(180);
        DesktopUi.styleModernControl(combo);
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(T v) { return v == null ? "" : labelOf(v); }
            @Override public T fromString(String s) { return null; }
        });
        return combo;
    }

    private static String labelOf(Enum<?> value) {
        if (value instanceof DisplayMode) return ((DisplayMode) value).getLabel();
        if (value instanceof PlaybackMode) return ((PlaybackMode) value).getLabel();
        if (value instanceof OverlayMode) return ((OverlayMode) value).getLabel();
        return value.name();
    }
}

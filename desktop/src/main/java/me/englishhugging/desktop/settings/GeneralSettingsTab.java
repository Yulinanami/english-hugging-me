package me.englishhugging.desktop.settings;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
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

        GridPane grid = DesktopUi.settingsGrid();
        grid.add(new Label("显示内容："), 0, 0); grid.add(displayMode, 1, 0);
        grid.add(new Label("播放顺序："), 0, 1); grid.add(playbackMode, 1, 1);
        grid.add(new Label("悬浮行为："), 0, 2); grid.add(overlayMode, 1, 2);
        grid.add(new Label("切换间隔："), 0, 3); grid.add(new HBox(6, interval, new Label("秒")), 1, 3);
        grid.add(new Label("透明度："), 0, 4); grid.add(opacity, 1, 4);

        VBox page = new VBox(10, DesktopUi.groupBox("常规", grid));
        page.setPadding(new Insets(10));
        return page;
    }

    @SuppressWarnings("unchecked")
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

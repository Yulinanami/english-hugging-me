package me.englishhugging.desktop.settings;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.ui.DesktopUi;

final class AppearanceSettingsTab {
    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final DesktopOverlayController overlayController;

    AppearanceSettingsTab(AppSettings settings, DesktopSettingsStore settingsStore, DesktopOverlayController overlayController) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.overlayController = overlayController;
    }

    Node createContent() {
        ColorPicker wordColor = colorPicker(settings.getWordColor());
        ColorPicker typeColor = colorPicker(settings.getTypeColor());
        ColorPicker translationColor = colorPicker(settings.getTranslationColor());
        ColorPicker phraseColor = colorPicker(settings.getPhraseColor());

        wordColor.setOnAction(e -> { settings.setWordColor(toHex(wordColor.getValue())); save(); });
        typeColor.setOnAction(e -> { settings.setTypeColor(toHex(typeColor.getValue())); save(); });
        translationColor.setOnAction(e -> { settings.setTranslationColor(toHex(translationColor.getValue())); save(); });
        phraseColor.setOnAction(e -> { settings.setPhraseColor(toHex(phraseColor.getValue())); save(); });

        Spinner<Integer> wordFontSize = fontSpinner(16, 72, settings.getWordFontSize());
        wordFontSize.valueProperty().addListener((o, ov, nv) -> { settings.setWordFontSize(nv); save(); });

        Spinner<Integer> detailFontSize = fontSpinner(12, 60, settings.getDetailFontSize());
        detailFontSize.valueProperty().addListener((o, ov, nv) -> { settings.setDetailFontSize(nv); save(); });

        GridPane grid = DesktopUi.settingsGrid();
        grid.add(new Label("单词颜色："), 0, 0); grid.add(wordColor, 1, 0);
        grid.add(new Label("词性颜色："), 0, 1); grid.add(typeColor, 1, 1);
        grid.add(new Label("释义颜色："), 0, 2); grid.add(translationColor, 1, 2);
        grid.add(new Label("短语颜色："), 0, 3); grid.add(phraseColor, 1, 3);
        grid.add(new Label("单词字号："), 0, 4); grid.add(wordFontSize, 1, 4);
        grid.add(new Label("释义字号："), 0, 5); grid.add(detailFontSize, 1, 5);

        VBox page = new VBox(10, DesktopUi.groupBox("外观", grid));
        page.setPadding(new Insets(10));
        return page;
    }

    private void save() {
        settingsStore.save(settings);
        overlayController.refreshDisplay();
    }

    private ColorPicker colorPicker(String hex) {
        ColorPicker picker = new ColorPicker(Color.web(hex));
        DesktopUi.styleModernControl(picker);
        return picker;
    }

    private Spinner<Integer> fontSpinner(int min, int max, int value) {
        Spinner<Integer> spinner = new Spinner<>(min, max, value);
        spinner.setEditable(true);
        spinner.setPrefWidth(92);
        DesktopUi.styleModernControl(spinner);
        return spinner;
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }
}

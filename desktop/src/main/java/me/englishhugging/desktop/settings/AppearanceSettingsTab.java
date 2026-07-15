package me.englishhugging.desktop.settings;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.paint.Color;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.ui.DesktopUi;

/** 外观设置 FXML 页面的数据初始化和即时预览控制器。 */
final class AppearanceSettingsTab {
    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final DesktopOverlayController overlayController;

    @FXML
    private ColorPicker wordColor;
    @FXML
    private ColorPicker typeColor;
    @FXML
    private ColorPicker translationColor;
    @FXML
    private ColorPicker phraseColor;
    @FXML
    private Spinner<Integer> wordFontSize;
    @FXML
    private Spinner<Integer> detailFontSize;

    AppearanceSettingsTab(
            AppSettings settings,
            DesktopSettingsStore settingsStore,
            DesktopOverlayController overlayController
    ) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.overlayController = overlayController;
    }

    /** 加载由 FXML 声明的外观设置页面。 */
    Node createContent() {
        return DesktopUi.loadFxml("/fxml/appearance-settings.fxml", this);
    }

    /** FXML 字段注入完成后设置当前值并绑定即时预览事件。 */
    @FXML
    private void initialize() {
        this.wordColor.setValue(Color.web(this.settings.getWordColor()));
        this.typeColor.setValue(Color.web(this.settings.getTypeColor()));
        this.translationColor.setValue(Color.web(this.settings.getTranslationColor()));
        this.phraseColor.setValue(Color.web(this.settings.getPhraseColor()));
        configureSpinner(this.wordFontSize, 16, 72, this.settings.getWordFontSize());
        configureSpinner(this.detailFontSize, 12, 60, this.settings.getDetailFontSize());

        this.wordColor.setOnAction(event -> {
            this.settings.setWordColor(toHex(this.wordColor.getValue()));
            save();
        });
        this.typeColor.setOnAction(event -> {
            this.settings.setTypeColor(toHex(this.typeColor.getValue()));
            save();
        });
        this.translationColor.setOnAction(event -> {
            this.settings.setTranslationColor(toHex(this.translationColor.getValue()));
            save();
        });
        this.phraseColor.setOnAction(event -> {
            this.settings.setPhraseColor(toHex(this.phraseColor.getValue()));
            save();
        });
        this.wordFontSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setWordFontSize(newValue);
            save();
        });
        this.detailFontSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setDetailFontSize(newValue);
            save();
        });
    }

    private void configureSpinner(Spinner<Integer> spinner, int min, int max, int value) {
        spinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, value)
        );
    }

    private void save() {
        this.settingsStore.save(this.settings);
        this.overlayController.refreshDisplay();
    }

    private static String toHex(Color color) {
        return String.format(
                "#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255)
        );
    }
}

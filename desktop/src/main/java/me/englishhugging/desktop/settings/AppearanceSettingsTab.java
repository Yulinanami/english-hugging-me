package me.englishhugging.desktop.settings;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Spinner;
import javafx.scene.paint.Color;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.ui.DesktopUi;

/**
 * 桌面端“外观设置”页面，负责调节单词颜色和字号大小。
 *
 * <p>用户在界面上修改任意颜色或字号后，会自动保存并实时刷新桌面悬浮窗。
 */
final class AppearanceSettingsTab {
    /** 应用配置 */
    private final AppSettings settings;
    /** 配置存储 */
    private final DesktopSettingsStore settingsStore;
    /** 桌面悬浮窗，用于在修改外观后实时刷新界面 */
    private final DesktopOverlayController overlayController;

    /** 单词颜色选择框 */
    @FXML
    private ColorPicker wordColor;
    /** 词性颜色选择框 */
    @FXML
    private ColorPicker typeColor;
    /** 中文释义颜色选择框 */
    @FXML
    private ColorPicker translationColor;
    /** 例句短语颜色选择框 */
    @FXML
    private ColorPicker phraseColor;
    /** 单词字号微调框（像素） */
    @FXML
    private Spinner<Integer> wordFontSize;
    /** 释义与短语字号微调框（像素） */
    @FXML
    private Spinner<Integer> detailFontSize;

    /**
     * 创建外观设置页面。
     *
     * @param settings          应用配置
     * @param settingsStore     配置存储
     * @param overlayController 桌面悬浮窗
     */
    AppearanceSettingsTab(
            AppSettings settings,
            DesktopSettingsStore settingsStore,
            DesktopOverlayController overlayController
    ) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.overlayController = overlayController;
    }

    /**
     * 加载外观设置界面。
     *
     * @return 外观设置界面的根节点
     */
    Node createContent() {
        return DesktopUi.loadFxml("/fxml/appearance-settings.fxml", this);
    }

    /**
     * 初始化界面控件，填充当前设置值并绑定修改事件。
     */
    @FXML
    private void initialize() {
        // 填充当前设置的值
        this.wordColor.setValue(Color.web(this.settings.getWordColor()));
        this.typeColor.setValue(Color.web(this.settings.getTypeColor()));
        this.translationColor.setValue(Color.web(this.settings.getTranslationColor()));
        this.phraseColor.setValue(Color.web(this.settings.getPhraseColor()));
        this.wordFontSize.getValueFactory().setValue(this.settings.getWordFontSize());
        this.detailFontSize.getValueFactory().setValue(this.settings.getDetailFontSize());

        // 颜色修改后立即保存并刷新悬浮窗
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

        // 字号修改后立即保存并刷新悬浮窗
        this.wordFontSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setWordFontSize(newValue);
            save();
        });
        this.detailFontSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.setDetailFontSize(newValue);
            save();
        });
    }

    /**
     * 保存设置并刷新悬浮窗界面。
     */
    private void save() {
        this.settingsStore.save(this.settings);
        this.overlayController.refreshDisplay();
    }

    /**
     * 将颜色对象转换为十六进制颜色字符串（如 "#FFFFFF"）。
     *
     * @param color 颜色对象
     * @return 十六进制颜色字符串
     */
    private static String toHex(Color color) {
        return String.format(
                "#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255)
        );
    }
}

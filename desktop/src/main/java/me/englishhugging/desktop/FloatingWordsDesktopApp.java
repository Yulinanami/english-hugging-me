package me.englishhugging.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.Cursor;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import me.englishhugging.core.AppSettings;
import me.englishhugging.core.DisplayMode;
import me.englishhugging.core.OverlayMode;
import me.englishhugging.core.Phrase;
import me.englishhugging.core.Translation;
import me.englishhugging.core.VocabularyJsonLoader;
import me.englishhugging.core.WordEntry;
import me.englishhugging.core.WordScheduler;

import java.io.File;
import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public final class FloatingWordsDesktopApp extends Application {
    private final DesktopSettingsStore settingsStore = new DesktopSettingsStore();
    private final String overlayTitle = "English Hugging Me Overlay " + UUID.randomUUID();

    private AppSettings settings;
    private Stage overlayStage;
    private Stage settingsStage;
    private StackPane overlayRoot;
    private TextFlow wordFlow;
    private WordScheduler scheduler;
    private WordEntry currentWord;
    private double dragOffsetX;
    private double dragOffsetY;
    private double resizeStartScreenX;
    private double resizeStartScreenY;
    private double resizeStartWidth;
    private double resizeStartHeight;
    private TrayIcon trayIcon;
    private Popup trayMenu;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        settings = settingsStore.load();
        overlayStage = createOverlayStage();
        settingsStage = createSettingsStage();

        overlayStage.show();
        WindowsClickThrough.hideFromTaskbar(overlayStage);
        installTrayIcon();
        applyOverlayMode();
        reloadVocabulary();
    }

    @Override
    public void stop() {
        if (scheduler != null) {
            scheduler.stop();
        }
        if (settings != null) {
            settingsStore.save(settings);
        }
        removeTrayIcon();
    }

    private Stage createOverlayStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.setTitle(overlayTitle);
        stage.setAlwaysOnTop(true);
        stage.setX(settings.getX());
        stage.setY(settings.getY());
        stage.setOpacity(settings.getOpacity());

        wordFlow = new TextFlow();
        wordFlow.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        renderMessage("正在加载...");

        overlayRoot = new StackPane(wordFlow);
        overlayRoot.setPadding(new Insets(14, 22, 14, 22));
        overlayRoot.setStyle("-fx-background-color: rgba(0,0,0,0.58); -fx-background-radius: 18;");
        wordFlow.maxWidthProperty().bind(overlayRoot.widthProperty().subtract(60));
        wordFlow.maxHeightProperty().bind(overlayRoot.heightProperty().subtract(40));

        Label resizeHandle = new Label("◢");
        resizeHandle.setTextFill(Color.rgb(220, 220, 220, 0.9));
        resizeHandle.setStyle("-fx-cursor: se-resize;");
        StackPane.setAlignment(resizeHandle, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(resizeHandle, new Insets(0, 0, -6, 0));
        overlayRoot.getChildren().add(resizeHandle);
        resizeHandle.setOnMousePressed(event -> {
            resizeStartScreenX = event.getScreenX();
            resizeStartScreenY = event.getScreenY();
            resizeStartWidth = stage.getWidth();
            resizeStartHeight = stage.getHeight();
            event.consume();
        });
        resizeHandle.setOnMouseDragged(event -> {
            double nextWidth = Math.max(260, resizeStartWidth + event.getScreenX() - resizeStartScreenX);
            double nextHeight = Math.max(80, resizeStartHeight + event.getScreenY() - resizeStartScreenY);
            stage.setWidth(nextWidth);
            stage.setHeight(nextHeight);
            settings.setWidth(nextWidth);
            settings.setHeight(nextHeight);
            settingsStore.save(settings);
            event.consume();
        });
        overlayRoot.setOnMousePressed(event -> {
            if (settings.getOverlayMode() != OverlayMode.DRAGGABLE) {
                return;
            }
            dragOffsetX = stage.getX() - event.getScreenX();
            dragOffsetY = stage.getY() - event.getScreenY();
        });
        overlayRoot.setOnMouseDragged(event -> {
            if (settings.getOverlayMode() != OverlayMode.DRAGGABLE) {
                return;
            }
            stage.setX(event.getScreenX() + dragOffsetX);
            stage.setY(event.getScreenY() + dragOffsetY);
            settings.setX(stage.getX());
            settings.setY(stage.getY());
            settingsStore.save(settings);
        });

        Scene scene = new Scene(overlayRoot, settings.getWidth(), settings.getHeight());
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setMinWidth(260);
        stage.setMinHeight(80);
        return stage;
    }

    private Stage createSettingsStage() {
        Stage stage = new Stage();
        stage.setTitle("悬浮背词设置");
        stage.setOnCloseRequest(event -> {
            event.consume();
            stage.hide();
        });
        stage.iconifiedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                stage.setIconified(false);
                stage.hide();
            }
        });

        TextField vocabularyPath = new TextField(settings.getVocabularyPath());
        vocabularyPath.setPrefColumnCount(36);
        Button chooseVocabulary = new Button("选择...");
        chooseVocabulary.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择 JSON 词库");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File selected = fileChooser.showOpenDialog(stage);
            if (selected != null) {
                vocabularyPath.setText(selected.getAbsolutePath());
                settings.setVocabularyPath(selected.getAbsolutePath());
                settingsStore.save(settings);
                reloadVocabulary();
            }
        });

        Button reloadVocabulary = new Button("重新加载词库");
        reloadVocabulary.setOnAction(event -> {
            settings.setVocabularyPath(vocabularyPath.getText());
            settingsStore.save(settings);
            reloadVocabulary();
        });

        ComboBox<DisplayMode> displayMode = new ComboBox<>();
        displayMode.getItems().addAll(DisplayMode.values());
        displayMode.setConverter(new StringConverter<>() {
            @Override
            public String toString(DisplayMode value) {
                return displayModeLabel(value);
            }

            @Override
            public DisplayMode fromString(String value) {
                return null;
            }
        });
        displayMode.setValue(settings.getDisplayMode());
        displayMode.setOnAction(event -> {
            settings.setDisplayMode(displayMode.getValue());
            settingsStore.save(settings);
            updateCurrentWord();
        });

        ComboBox<OverlayMode> overlayMode = new ComboBox<>();
        overlayMode.getItems().addAll(OverlayMode.values());
        overlayMode.setConverter(new StringConverter<>() {
            @Override
            public String toString(OverlayMode value) {
                return overlayModeLabel(value);
            }

            @Override
            public OverlayMode fromString(String value) {
                return null;
            }
        });
        overlayMode.setValue(settings.getOverlayMode());
        overlayMode.setOnAction(event -> {
            settings.setOverlayMode(overlayMode.getValue());
            settingsStore.save(settings);
            applyOverlayMode();
        });

        Spinner<Integer> intervalSeconds = new Spinner<>(2, 300, settings.getIntervalSeconds());
        intervalSeconds.setEditable(true);
        intervalSeconds.valueProperty().addListener((observable, oldValue, newValue) -> {
            settings.setIntervalSeconds(newValue);
            settingsStore.save(settings);
            if (scheduler != null) {
                scheduler.updateIntervalSeconds(settings.getIntervalSeconds());
            }
        });

        Slider opacity = new Slider(0.2, 1.0, settings.getOpacity());
        opacity.setShowTickLabels(true);
        opacity.setShowTickMarks(true);
        ChangeListener<Number> opacityListener = (observable, oldValue, newValue) -> {
            settings.setOpacity(newValue.doubleValue());
            overlayStage.setOpacity(settings.getOpacity());
            settingsStore.save(settings);
        };
        opacity.valueProperty().addListener(opacityListener);

        ColorPicker wordColor = new ColorPicker(Color.web(settings.getWordColor()));
        wordColor.setOnAction(event -> {
            settings.setWordColor(toHex(wordColor.getValue()));
            settingsStore.save(settings);
            updateCurrentWord();
        });

        ColorPicker typeColor = new ColorPicker(Color.web(settings.getTypeColor()));
        typeColor.setOnAction(event -> {
            settings.setTypeColor(toHex(typeColor.getValue()));
            settingsStore.save(settings);
            updateCurrentWord();
        });

        ColorPicker translationColor = new ColorPicker(Color.web(settings.getTranslationColor()));
        translationColor.setOnAction(event -> {
            settings.setTranslationColor(toHex(translationColor.getValue()));
            settingsStore.save(settings);
            updateCurrentWord();
        });

        ColorPicker phraseColor = new ColorPicker(Color.web(settings.getPhraseColor()));
        phraseColor.setOnAction(event -> {
            settings.setPhraseColor(toHex(phraseColor.getValue()));
            settingsStore.save(settings);
            updateCurrentWord();
        });

        Spinner<Integer> wordFontSize = new Spinner<>(16, 72, settings.getWordFontSize());
        wordFontSize.setEditable(true);
        wordFontSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            settings.setWordFontSize(newValue);
            settingsStore.save(settings);
            updateCurrentWord();
        });

        Spinner<Integer> detailFontSize = new Spinner<>(12, 60, settings.getDetailFontSize());
        detailFontSize.setEditable(true);
        detailFontSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            settings.setDetailFontSize(newValue);
            settingsStore.save(settings);
            updateCurrentWord();
        });

        Button exit = new Button("退出");
        exit.setOnAction(event -> exitApplication());

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.add(new Label("词库："), 0, 0);
        grid.add(vocabularyPath, 1, 0);
        grid.add(chooseVocabulary, 2, 0);
        grid.add(reloadVocabulary, 1, 1);
        grid.add(new Label("显示内容："), 0, 2);
        grid.add(displayMode, 1, 2);
        grid.add(new Label("悬浮行为："), 0, 3);
        grid.add(overlayMode, 1, 3);
        grid.add(new Label("切换间隔（秒）："), 0, 4);
        grid.add(intervalSeconds, 1, 4);
        grid.add(new Label("透明度："), 0, 5);
        grid.add(opacity, 1, 5);
        grid.add(new Label("单词颜色："), 0, 6);
        grid.add(wordColor, 1, 6);
        grid.add(new Label("词性颜色："), 0, 7);
        grid.add(typeColor, 1, 7);
        grid.add(new Label("释义颜色："), 0, 8);
        grid.add(translationColor, 1, 8);
        grid.add(new Label("短语颜色："), 0, 9);
        grid.add(phraseColor, 1, 9);
        grid.add(new Label("单词字号："), 0, 10);
        grid.add(wordFontSize, 1, 10);
        grid.add(new Label("释义字号："), 0, 11);
        grid.add(detailFontSize, 1, 11);
        grid.add(exit, 1, 12);

        VBox root = new VBox(12, new Label("程序默认隐藏在右下角托盘；需要设置时，从托盘菜单打开此窗口。"), grid);
        root.setPadding(new Insets(16));
        stage.setScene(new Scene(root));
        return stage;
    }

    private void reloadVocabulary() {
        try {
            Path vocabulary = Paths.get(settings.getVocabularyPath());
            List<WordEntry> words = new VocabularyJsonLoader().load(vocabulary);
            startScheduler(words);
        } catch (Exception exception) {
            showError("词库加载失败", exception.getMessage());
            renderMessage("词库加载失败\n请在设置中选择 JSON 词库");
        }
    }

    private void startScheduler(List<WordEntry> words) {
        if (scheduler != null) {
            scheduler.stop();
        }
        scheduler = new WordScheduler(words, settings.getIntervalSeconds(), wordEntry -> Platform.runLater(() -> {
            currentWord = wordEntry;
            updateCurrentWord();
        }));
        scheduler.start();
    }

    private void updateCurrentWord() {
        if (currentWord != null) {
            renderWord(currentWord);
            ensureOverlayFitsText();
        }
    }

    private void ensureOverlayFitsText() {
        Platform.runLater(() -> {
            double contentWidth = Math.max(200, overlayStage.getWidth() - 60);
            double requiredHeight = wordFlow.prefHeight(contentWidth) + 42;
            if (requiredHeight > overlayStage.getHeight()) {
                overlayStage.setHeight(requiredHeight);
                settings.setHeight(requiredHeight);
                settingsStore.save(settings);
            }
        });
    }

    private void renderWord(WordEntry wordEntry) {
        wordFlow.getChildren().clear();
        appendText(safe(wordEntry.getWord()), settings.getWordColor(), settings.getWordFontSize(), FontWeight.BOLD);

        if (settings.getDisplayMode() == DisplayMode.WORD_ONLY) {
            return;
        }

        for (Translation translation : wordEntry.getTranslations()) {
            if (translation == null) {
                continue;
            }
            String type = safe(translation.getType());
            String meaning = safe(translation.getTranslation());
            if (type.length() == 0 && meaning.length() == 0) {
                continue;
            }
            appendText("\n", settings.getTranslationColor(), settings.getDetailFontSize(), FontWeight.NORMAL);
            if (type.length() > 0) {
                appendText(type + ". ", settings.getTypeColor(), settings.getDetailFontSize(), FontWeight.BOLD);
            }
            appendText(meaning, settings.getTranslationColor(), settings.getDetailFontSize(), FontWeight.NORMAL);
        }

        if (settings.getDisplayMode() == DisplayMode.WORD_WITH_TRANSLATION_AND_PHRASE) {
            int displayed = 0;
            for (Phrase phrase : wordEntry.getPhrases()) {
                if (phrase == null) {
                    continue;
                }
                String phraseText = safe(phrase.getPhrase());
                String phraseTranslation = safe(phrase.getTranslation());
                if (phraseText.length() == 0 && phraseTranslation.length() == 0) {
                    continue;
                }
                appendText("\n" + phraseText, settings.getPhraseColor(), settings.getDetailFontSize(), FontWeight.BOLD);
                if (phraseTranslation.length() > 0) {
                    appendText("： " + phraseTranslation, settings.getTranslationColor(), settings.getDetailFontSize(), FontWeight.NORMAL);
                }
                displayed++;
                if (displayed >= 2) {
                    break;
                }
            }
        }
    }

    private void renderMessage(String message) {
        wordFlow.getChildren().clear();
        appendText(message, settings.getWordColor(), settings.getDetailFontSize(), FontWeight.NORMAL);
    }

    private void appendText(String value, String color, int fontSize, FontWeight fontWeight) {
        Text text = new Text(value);
        text.setFill(Color.web(color));
        text.setFont(Font.font("Microsoft YaHei", fontWeight, fontSize));
        wordFlow.getChildren().add(text);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String toHex(Color color) {
        return String.format(
                "#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255)
        );
    }

    private void applyOverlayMode() {
        boolean clickThrough = settings.getOverlayMode() == OverlayMode.CLICK_THROUGH;
        overlayRoot.setMouseTransparent(clickThrough);
        WindowsClickThrough.apply(overlayStage, clickThrough);
    }

    private void installTrayIcon() {
        if (!SystemTray.isSupported()) {
            showSettingsWindow();
            return;
        }

        trayIcon = new TrayIcon(createTrayImage(), "悬浮背词");
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(event -> Platform.runLater(this::showSettingsWindow));
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.isPopupTrigger() || event.getButton() == MouseEvent.BUTTON3) {
                    Platform.runLater(FloatingWordsDesktopApp.this::showTrayMenu);
                }
            }
        });
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException exception) {
            trayIcon = null;
            showSettingsWindow();
        }
    }

    private void showTrayMenu() {
        if (trayMenu != null) {
            trayMenu.hide();
        }

        Label openSettings = trayMenuItem("打开设置");
        openSettings.setOnMouseClicked(event -> {
            trayMenu.hide();
            showSettingsWindow();
        });
        Label exit = trayMenuItem("退出");
        exit.setOnMouseClicked(event -> {
            trayMenu.hide();
            exitApplication();
        });

        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: #c8c8c8;");

        VBox menuContent = new VBox(openSettings, separator, exit);
        menuContent.setStyle(
                "-fx-background-color: white;"
                        + "-fx-border-color: #9e9e9e;"
                        + "-fx-border-width: 1;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0, 2, 2);"
                        + "-fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', 'SimSun';"
                        + "-fx-font-size: 13px;"
        );

        Point pointer = MouseInfo.getPointerInfo().getLocation();
        trayMenu = new Popup();
        trayMenu.setAutoHide(true);
        trayMenu.setHideOnEscape(true);
        trayMenu.getContent().add(menuContent);
        trayMenu.show(overlayStage, pointer.x + 8, pointer.y - 8);
    }

    private Label trayMenuItem(String text) {
        Label item = new Label(text);
        item.setMinWidth(88);
        item.setPadding(new Insets(8, 16, 8, 12));
        item.setCursor(Cursor.HAND);
        item.setStyle("-fx-text-fill: #111111;");
        item.setOnMouseEntered(event -> item.setStyle("-fx-background-color: #e5f1fb; -fx-text-fill: #111111;"));
        item.setOnMouseExited(event -> item.setStyle("-fx-text-fill: #111111;"));
        return item;
    }

    private BufferedImage createTrayImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(47, 111, 237));
        graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        graphics.drawString("E", 4, 12);
        graphics.dispose();
        return image;
    }

    private void removeTrayIcon() {
        if (trayMenu != null) {
            trayMenu.hide();
            trayMenu = null;
        }
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    private void showSettingsWindow() {
        settingsStage.show();
        settingsStage.setIconified(false);
        settingsStage.toFront();
    }

    private void exitApplication() {
        removeTrayIcon();
        Platform.setImplicitExit(true);
        Platform.exit();
    }

    private String displayModeLabel(DisplayMode displayMode) {
        if (displayMode == DisplayMode.WORD_ONLY) {
            return "只显示单词";
        }
        if (displayMode == DisplayMode.WORD_WITH_TRANSLATION_AND_PHRASE) {
            return "单词 + 释义 + 短语";
        }
        return "单词 + 释义";
    }

    private String overlayModeLabel(OverlayMode overlayMode) {
        if (overlayMode == OverlayMode.LOCKED) {
            return "锁定位置";
        }
        if (overlayMode == OverlayMode.CLICK_THROUGH) {
            return "点击穿透";
        }
        return "可拖动";
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "未知错误" : message);
        alert.showAndWait();
    }
}

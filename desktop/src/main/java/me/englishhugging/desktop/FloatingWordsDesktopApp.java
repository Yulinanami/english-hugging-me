package me.englishhugging.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import me.englishhugging.core.AppSettings;
import me.englishhugging.core.DisplayMode;
import me.englishhugging.core.OverlayMode;
import me.englishhugging.core.PlaybackMode;
import me.englishhugging.core.VocabularyJsonLoader;
import me.englishhugging.core.WordDisplayFormatter;
import me.englishhugging.core.WordDisplaySegment;
import me.englishhugging.core.WordEntry;
import me.englishhugging.core.WordScheduler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public final class FloatingWordsDesktopApp extends Application {
    private static final int MOVE_HANDLE_SIZE = 42;
    private static final int RESIZE_HANDLE_SIZE = 42;
    private static final String APP_ICON_RESOURCE = "/icons/app.png";

    private final DesktopSettingsStore settingsStore = new DesktopSettingsStore();
    private final WordDisplayFormatter wordDisplayFormatter = new WordDisplayFormatter();
    private final String overlayTitle = "English Hugging Me Overlay " + UUID.randomUUID();
    private final String moveHandleTitle = "English Hugging Me Move Handle " + UUID.randomUUID();
    private final String resizeHandleTitle = "English Hugging Me Resize Handle " + UUID.randomUUID();

    private AppSettings settings;
    private Stage overlayStage;
    private Stage moveHandleStage;
    private Stage resizeHandleStage;
    private Stage settingsStage;
    private StackPane overlayRoot;
    private TextFlow wordFlow;
    private WordScheduler scheduler;
    private WordEntry currentWord;
    private double dragOffsetX;
    private double dragOffsetY;
    private double moveHandleDragOffsetX;
    private double moveHandleDragOffsetY;
    private double resizeStartScreenX;
    private double resizeStartScreenY;
    private double resizeStartWidth;
    private double resizeStartHeight;
    private TrayIcon trayIcon;
    private Popup trayMenu;
    private Timeline trayMenuWatcher;
    private Image appIcon;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        settings = settingsStore.load();
        overlayStage = createOverlayStage();
        moveHandleStage = createMoveHandleStage();
        resizeHandleStage = createResizeHandleStage();
        settingsStage = createSettingsStage();

        overlayStage.show();
        moveHandleStage.show();
        resizeHandleStage.show();
        WindowsClickThrough.hideFromTaskbar(overlayStage);
        WindowsClickThrough.hideFromTaskbar(moveHandleStage);
        WindowsClickThrough.hideFromTaskbar(resizeHandleStage);
        syncControlHandlePositions();
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
        if (moveHandleStage != null) {
            moveHandleStage.close();
        }
        if (resizeHandleStage != null) {
            resizeHandleStage.close();
        }
        removeTrayIcon();
    }

    private Stage createOverlayStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.setTitle(overlayTitle);
        applyStageIcon(stage);
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
            syncControlHandlePositions();
        });

        Scene scene = new Scene(overlayRoot, settings.getWidth(), settings.getHeight());
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setMinWidth(260);
        stage.setMinHeight(80);
        return stage;
    }

    private Stage createMoveHandleStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.setTitle(moveHandleTitle);
        stage.setAlwaysOnTop(true);

        StackPane moveHandle = createMoveHandleNode();
        moveHandle.setOnMousePressed(event -> {
            moveHandleDragOffsetX = overlayStage.getX() - event.getScreenX();
            moveHandleDragOffsetY = overlayStage.getY() - event.getScreenY();
            event.consume();
        });
        moveHandle.setOnMouseDragged(event -> {
            double nextX = event.getScreenX() + moveHandleDragOffsetX;
            double nextY = event.getScreenY() + moveHandleDragOffsetY;
            overlayStage.setX(nextX);
            overlayStage.setY(nextY);
            settings.setX(nextX);
            settings.setY(nextY);
            settingsStore.save(settings);
            syncControlHandlePositions();
            event.consume();
        });

        Scene scene = new Scene(moveHandle, MOVE_HANDLE_SIZE, MOVE_HANDLE_SIZE);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        overlayStage.xProperty().addListener((observable, oldValue, newValue) -> syncControlHandlePositions());
        overlayStage.yProperty().addListener((observable, oldValue, newValue) -> syncControlHandlePositions());
        overlayStage.widthProperty().addListener((observable, oldValue, newValue) -> syncControlHandlePositions());
        overlayStage.heightProperty().addListener((observable, oldValue, newValue) -> syncControlHandlePositions());
        return stage;
    }

    private StackPane createMoveHandleNode() {
        GridPane dots = new GridPane();
        dots.setHgap(4);
        dots.setVgap(4);
        dots.setAlignment(Pos.CENTER);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                dots.add(new Circle(2, Color.rgb(255, 255, 255, 0.72)), col, row);
            }
        }

        StackPane handle = new StackPane(dots);
        handle.setPickOnBounds(true);
        handle.setMinSize(MOVE_HANDLE_SIZE, MOVE_HANDLE_SIZE);
        handle.setPrefSize(MOVE_HANDLE_SIZE, MOVE_HANDLE_SIZE);
        handle.setMaxSize(MOVE_HANDLE_SIZE, MOVE_HANDLE_SIZE);
        handle.setStyle("-fx-background-color: rgba(255,255,255,0.01); -fx-cursor: move;");
        return handle;
    }

    private Stage createResizeHandleStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.setTitle(resizeHandleTitle);
        stage.setAlwaysOnTop(true);

        Pane resizeHandle = createResizeHandleNode();
        resizeHandle.setOnMousePressed(event -> {
            resizeStartScreenX = event.getScreenX();
            resizeStartScreenY = event.getScreenY();
            resizeStartWidth = overlayStage.getWidth();
            resizeStartHeight = overlayStage.getHeight();
            event.consume();
        });
        resizeHandle.setOnMouseDragged(event -> {
            double nextWidth = Math.max(260, resizeStartWidth + event.getScreenX() - resizeStartScreenX);
            double nextHeight = Math.max(80, resizeStartHeight + event.getScreenY() - resizeStartScreenY);
            overlayStage.setWidth(nextWidth);
            overlayStage.setHeight(nextHeight);
            settings.setWidth(nextWidth);
            settings.setHeight(nextHeight);
            settingsStore.save(settings);
            syncControlHandlePositions();
            event.consume();
        });

        Scene scene = new Scene(resizeHandle, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        return stage;
    }

    private Pane createResizeHandleNode() {
        Pane handle = new Pane();
        handle.setPickOnBounds(true);
        handle.setMinSize(RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
        handle.setPrefSize(RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
        handle.setMaxSize(RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
        handle.setStyle("-fx-background-color: rgba(255,255,255,0.01); -fx-cursor: se-resize;");
        for (int i = 0; i < 3; i++) {
            Line line = new Line(23 + i * 4, 36, 36, 23 + i * 4);
            line.setStroke(Color.rgb(255, 255, 255, 0.68));
            line.setStrokeWidth(1.4);
            handle.getChildren().add(line);
        }
        return handle;
    }

    private void syncControlHandlePositions() {
        if (overlayStage == null) {
            return;
        }
        if (moveHandleStage != null) {
            moveHandleStage.setX(overlayStage.getX() + overlayStage.getWidth() - MOVE_HANDLE_SIZE - 8);
            moveHandleStage.setY(overlayStage.getY() + 4);
        }
        if (resizeHandleStage != null) {
            resizeHandleStage.setX(overlayStage.getX() + overlayStage.getWidth() - RESIZE_HANDLE_SIZE);
            resizeHandleStage.setY(overlayStage.getY() + overlayStage.getHeight() - RESIZE_HANDLE_SIZE);
        }
    }

    private Stage createSettingsStage() {
        Stage stage = new Stage();
        stage.setTitle("悬浮背词设置");
        applyStageIcon(stage);
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
                String previousPath = settings.getVocabularyPath();
                settings.setVocabularyPath(selected.getAbsolutePath());
                if (!previousPath.equals(settings.getVocabularyPath())) {
                    settings.resetPlaybackProgress();
                }
                settingsStore.save(settings);
                reloadVocabulary();
            }
        });

        Button reloadVocabulary = new Button("重新加载词库");
        reloadVocabulary.setOnAction(event -> {
            String previousPath = settings.getVocabularyPath();
            settings.setVocabularyPath(vocabularyPath.getText());
            if (!previousPath.equals(settings.getVocabularyPath())) {
                settings.resetPlaybackProgress();
            }
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

        ComboBox<PlaybackMode> playbackMode = new ComboBox<>();
        playbackMode.getItems().addAll(PlaybackMode.values());
        playbackMode.setConverter(new StringConverter<>() {
            @Override
            public String toString(PlaybackMode value) {
                return playbackModeLabel(value);
            }

            @Override
            public PlaybackMode fromString(String value) {
                return null;
            }
        });
        playbackMode.setValue(settings.getPlaybackMode());
        playbackMode.setOnAction(event -> {
            settings.setPlaybackMode(playbackMode.getValue());
            settings.resetPlaybackProgress();
            settingsStore.save(settings);
            reloadVocabulary();
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
        grid.add(new Label("播放顺序："), 0, 3);
        grid.add(playbackMode, 1, 3);
        grid.add(new Label("悬浮行为："), 0, 4);
        grid.add(overlayMode, 1, 4);
        grid.add(new Label("切换间隔（秒）："), 0, 5);
        grid.add(intervalSeconds, 1, 5);
        grid.add(new Label("透明度："), 0, 6);
        grid.add(opacity, 1, 6);
        grid.add(new Label("单词颜色："), 0, 7);
        grid.add(wordColor, 1, 7);
        grid.add(new Label("词性颜色："), 0, 8);
        grid.add(typeColor, 1, 8);
        grid.add(new Label("释义颜色："), 0, 9);
        grid.add(translationColor, 1, 9);
        grid.add(new Label("短语颜色："), 0, 10);
        grid.add(phraseColor, 1, 10);
        grid.add(new Label("单词字号："), 0, 11);
        grid.add(wordFontSize, 1, 11);
        grid.add(new Label("释义字号："), 0, 12);
        grid.add(detailFontSize, 1, 12);
        grid.add(exit, 1, 13);

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
        scheduler = new WordScheduler(
                words,
                settings.getIntervalSeconds(),
                settings.getPlaybackMode(),
                settings.getNextWordIndex(),
                settings.getShuffleOrder(),
                settings.getShufflePosition(),
                wordEntry -> Platform.runLater(() -> {
                    currentWord = wordEntry;
                    updateCurrentWord();
                }),
                (nextWordIndex, shuffleOrder, shufflePosition) -> {
                    settings.setNextWordIndex(nextWordIndex);
                    settings.setShuffleOrder(shuffleOrder);
                    settings.setShufflePosition(shufflePosition);
                    settingsStore.save(settings);
                }
        );
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
                syncControlHandlePositions();
            }
        });
    }

    private void renderWord(WordEntry wordEntry) {
        wordFlow.getChildren().clear();
        for (WordDisplaySegment segment : wordDisplayFormatter.format(wordEntry, settings.getDisplayMode())) {
            if (segment.getType() == WordDisplaySegment.Type.WORD) {
                appendText(segment.getText(), settings.getWordColor(), settings.getWordFontSize(), FontWeight.BOLD);
            } else if (segment.getType() == WordDisplaySegment.Type.TYPE) {
                appendText(segment.getText(), settings.getTypeColor(), settings.getDetailFontSize(), FontWeight.BOLD);
            } else if (segment.getType() == WordDisplaySegment.Type.PHRASE) {
                appendText(segment.getText(), settings.getPhraseColor(), settings.getDetailFontSize(), FontWeight.BOLD);
            } else {
                appendText(segment.getText(), settings.getTranslationColor(), settings.getDetailFontSize(), FontWeight.NORMAL);
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
        syncControlHandlePositions();
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
        stopTrayMenuWatcher();

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
        separator.setStyle("-fx-background-color: #E5E7EB;");
        VBox.setMargin(separator, new Insets(4, 0, 4, 0));

        VBox menuContent = new VBox(openSettings, separator, exit);
        menuContent.setStyle(
                "-fx-background-color: rgba(255,255,255,0.98);"
                        + "-fx-background-radius: 9;"
                        + "-fx-border-color: #DADDE3;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 9;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 18, 0, 0, 6);"
                        + "-fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', 'SimSun';"
                        + "-fx-font-size: 13px;"
        );
        menuContent.setPadding(new Insets(6, 0, 6, 0));

        Point pointer = MouseInfo.getPointerInfo().getLocation();
        boolean openedFromOverflow = WindowsClickThrough.isNotifyIconOverflowVisible();
        trayMenu = new Popup();
        trayMenu.setAutoHide(true);
        trayMenu.setHideOnEscape(true);
        trayMenu.getContent().add(menuContent);
        trayMenu.setOnHidden(event -> stopTrayMenuWatcher());
        trayMenu.show(overlayStage, pointer.x + 8, pointer.y - 8);
        startTrayMenuWatcher(openedFromOverflow);
    }

    private Label trayMenuItem(String text) {
        Label item = new Label(text);
        item.setMinWidth(132);
        item.setMinHeight(30);
        item.setPadding(new Insets(7, 18, 7, 18));
        item.setCursor(Cursor.HAND);
        item.setStyle("-fx-text-fill: #1F2328; -fx-background-radius: 6;");
        item.setOnMouseEntered(event -> item.setStyle("-fx-background-color: #F3F6FA; -fx-text-fill: #1F2328; -fx-background-radius: 6;"));
        item.setOnMouseExited(event -> item.setStyle("-fx-text-fill: #1F2328; -fx-background-radius: 6;"));
        return item;
    }

    private void startTrayMenuWatcher(boolean openedFromOverflow) {
        if (!openedFromOverflow) {
            return;
        }
        trayMenuWatcher = new Timeline(new KeyFrame(Duration.millis(150), event -> {
            if (trayMenu != null && trayMenu.isShowing() && !WindowsClickThrough.isNotifyIconOverflowVisible()) {
                trayMenu.hide();
            }
        }));
        trayMenuWatcher.setCycleCount(Timeline.INDEFINITE);
        trayMenuWatcher.play();
    }

    private void stopTrayMenuWatcher() {
        if (trayMenuWatcher != null) {
            trayMenuWatcher.stop();
            trayMenuWatcher = null;
        }
    }

    private BufferedImage createTrayImage() {
        try (InputStream inputStream = FloatingWordsDesktopApp.class.getResourceAsStream(APP_ICON_RESOURCE)) {
            if (inputStream != null) {
                BufferedImage image = ImageIO.read(inputStream);
                if (image != null) {
                    return image;
                }
            }
        } catch (IOException ignored) {
        }

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

    private void applyStageIcon(Stage stage) {
        Image icon = appIcon();
        if (icon != null) {
            stage.getIcons().add(icon);
        }
    }

    private Image appIcon() {
        if (appIcon == null) {
            try (InputStream inputStream = FloatingWordsDesktopApp.class.getResourceAsStream(APP_ICON_RESOURCE)) {
                if (inputStream != null) {
                    appIcon = new Image(inputStream);
                }
            } catch (IOException ignored) {
            }
        }
        return appIcon;
    }

    private void removeTrayIcon() {
        stopTrayMenuWatcher();
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

    private String playbackModeLabel(PlaybackMode playbackMode) {
        if (playbackMode == PlaybackMode.SEQUENTIAL) {
            return "顺序播放";
        }
        if (playbackMode == PlaybackMode.RANDOM) {
            return "随机播放";
        }
        return "随机不重复";
    }

    private String overlayModeLabel(OverlayMode overlayMode) {
        if (overlayMode == OverlayMode.LOCKED) {
            return "锁定位置";
        }
        if (overlayMode == OverlayMode.CLICK_THROUGH) {
            return "鼠标点击穿透";
        }
        return "全局可拖动";
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "未知错误" : message);
        alert.showAndWait();
    }
}

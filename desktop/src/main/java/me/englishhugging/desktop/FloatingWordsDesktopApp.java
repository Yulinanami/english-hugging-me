package me.englishhugging.desktop;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
import me.englishhugging.core.Phrase;
import me.englishhugging.core.PlaybackMode;
import me.englishhugging.core.Translation;
import me.englishhugging.core.VocabularyCatalog;
import me.englishhugging.core.VocabularyJsonLoader;
import me.englishhugging.core.WordDisplayFormatter;
import me.englishhugging.core.WordDisplaySegment;
import me.englishhugging.core.WordEntry;
import me.englishhugging.core.WordScheduler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
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
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class FloatingWordsDesktopApp extends Application {
    private static final int MOVE_HANDLE_SIZE = 42;
    private static final int RESIZE_HANDLE_SIZE = 42;
    private static final String APP_ICON_RESOURCE = "/icons/app.png";
    private static final String CUSTOM_VOCABULARY_LABEL = "自定义词汇";

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
    private ComboBox<String> vocabularyChoice;
    private VBox playbackRecordsBox;
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
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Platform.setImplicitExit(false);
        settings = settingsStore.load();
        overlayStage = createOverlayStage();
        moveHandleStage = createMoveHandleStage();
        resizeHandleStage = createResizeHandleStage();
        settingsStage = createSettingsStage();
        settingsStore.loadPlaybackProgress(settings, settings.getVocabularyPath());

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
        showSettingsWindow();
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
        stage.setTitle("English Hugging Me 首选项");
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

        vocabularyChoice = new ComboBox<>();
        vocabularyChoice.getItems().addAll(VocabularyCatalog.fileNames());
        if (Files.exists(customVocabularyPath())) {
            vocabularyChoice.getItems().add(CUSTOM_VOCABULARY_LABEL);
        }
        String currentChoice = vocabularyChoiceForPath(settings.getVocabularyPath());
        if (!vocabularyChoice.getItems().contains(currentChoice)) {
            vocabularyChoice.getItems().add(currentChoice);
        }
        vocabularyChoice.setValue(currentChoice);
        vocabularyChoice.setPrefWidth(300);
        styleModernControl(vocabularyChoice);
        vocabularyChoice.setOnAction(event -> applyVocabularyChoice(vocabularyChoice.getValue(), true));

        Button importVocabulary = compactButton("导入");
        importVocabulary.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("导入 JSON 词库");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File selected = fileChooser.showOpenDialog(stage);
            if (selected != null) {
                String selectedPath = selected.getAbsolutePath();
                if (!vocabularyChoice.getItems().contains(selectedPath)) {
                    vocabularyChoice.getItems().add(selectedPath);
                }
                vocabularyChoice.setValue(selectedPath);
                applyVocabularyChoice(selectedPath, true);
            }
        });

        Button reloadVocabulary = compactButton("重新加载");
        reloadVocabulary.setOnAction(event -> applyVocabularyChoice(vocabularyChoice.getValue(), true));

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
        displayMode.setPrefWidth(180);
        styleModernControl(displayMode);
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
        playbackMode.setPrefWidth(180);
        styleModernControl(playbackMode);
        playbackMode.setOnAction(event -> {
            settings.setPlaybackMode(playbackMode.getValue());
            settings.resetPlaybackProgress();
            settingsStore.save(settings);
            settingsStore.savePlaybackProgress(settings, settings.getVocabularyPath());
            reloadVocabulary();
            refreshPlaybackRecords();
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
        overlayMode.setPrefWidth(180);
        styleModernControl(overlayMode);
        overlayMode.setOnAction(event -> {
            settings.setOverlayMode(overlayMode.getValue());
            settingsStore.save(settings);
            applyOverlayMode();
        });

        Spinner<Integer> intervalSeconds = new Spinner<>(2, 300, settings.getIntervalSeconds());
        intervalSeconds.setEditable(true);
        intervalSeconds.setPrefWidth(92);
        styleModernControl(intervalSeconds);
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
        opacity.setPrefWidth(240);
        ChangeListener<Number> opacityListener = (observable, oldValue, newValue) -> {
            settings.setOpacity(newValue.doubleValue());
            overlayStage.setOpacity(settings.getOpacity());
            settingsStore.save(settings);
        };
        opacity.valueProperty().addListener(opacityListener);

        ColorPicker wordColor = new ColorPicker(Color.web(settings.getWordColor()));
        ColorPicker typeColor = new ColorPicker(Color.web(settings.getTypeColor()));
        ColorPicker translationColor = new ColorPicker(Color.web(settings.getTranslationColor()));
        ColorPicker phraseColor = new ColorPicker(Color.web(settings.getPhraseColor()));
        styleModernControl(wordColor);
        styleModernControl(typeColor);
        styleModernControl(translationColor);
        styleModernControl(phraseColor);
        wordColor.setOnAction(event -> {
            settings.setWordColor(toHex(wordColor.getValue()));
            settingsStore.save(settings);
            updateCurrentWord();
        });
        typeColor.setOnAction(event -> {
            settings.setTypeColor(toHex(typeColor.getValue()));
            settingsStore.save(settings);
            updateCurrentWord();
        });
        translationColor.setOnAction(event -> {
            settings.setTranslationColor(toHex(translationColor.getValue()));
            settingsStore.save(settings);
            updateCurrentWord();
        });
        phraseColor.setOnAction(event -> {
            settings.setPhraseColor(toHex(phraseColor.getValue()));
            settingsStore.save(settings);
            updateCurrentWord();
        });

        Spinner<Integer> wordFontSize = new Spinner<>(16, 72, settings.getWordFontSize());
        wordFontSize.setEditable(true);
        wordFontSize.setPrefWidth(92);
        styleModernControl(wordFontSize);
        wordFontSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            settings.setWordFontSize(newValue);
            settingsStore.save(settings);
            updateCurrentWord();
        });

        Spinner<Integer> detailFontSize = new Spinner<>(12, 60, settings.getDetailFontSize());
        detailFontSize.setEditable(true);
        detailFontSize.setPrefWidth(92);
        styleModernControl(detailFontSize);
        detailFontSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            settings.setDetailFontSize(newValue);
            settingsStore.save(settings);
            updateCurrentWord();
        });

        GridPane generalGrid = settingsGrid();
        generalGrid.add(new Label("显示内容："), 0, 0);
        generalGrid.add(displayMode, 1, 0);
        generalGrid.add(new Label("播放顺序："), 0, 1);
        generalGrid.add(playbackMode, 1, 1);
        generalGrid.add(new Label("悬浮行为："), 0, 2);
        generalGrid.add(overlayMode, 1, 2);
        generalGrid.add(new Label("切换间隔："), 0, 3);
        generalGrid.add(new HBox(6, intervalSeconds, new Label("秒")), 1, 3);
        generalGrid.add(new Label("透明度："), 0, 4);
        generalGrid.add(opacity, 1, 4);

        GridPane vocabularyGrid = settingsGrid();
        vocabularyGrid.add(new Label("词汇本："), 0, 0);
        vocabularyGrid.add(new HBox(6, vocabularyChoice, importVocabulary), 1, 0);
        vocabularyGrid.add(reloadVocabulary, 1, 1);

        TextField customWord = compactTextField();
        TextField customType = compactTextField();
        TextField customPhrase = compactTextField();
        TextField customExample = compactTextField();
        TextField customMeaning = compactTextField();
        Button addCustomWord = compactButton("添加");
        addCustomWord.setOnAction(event -> addCustomWord(customWord, customType, customPhrase, customExample, customMeaning));

        GridPane customGrid = settingsGrid();
        customGrid.add(new Label("单词："), 0, 0);
        customGrid.add(customWord, 1, 0);
        customGrid.add(new Label("词性："), 0, 1);
        customGrid.add(customType, 1, 1);
        customGrid.add(new Label("词组："), 0, 2);
        customGrid.add(customPhrase, 1, 2);
        customGrid.add(new Label("例句："), 0, 3);
        customGrid.add(customExample, 1, 3);
        customGrid.add(new Label("意思："), 0, 4);
        customGrid.add(customMeaning, 1, 4);
        customGrid.add(addCustomWord, 1, 5);

        GridPane appearanceGrid = settingsGrid();
        appearanceGrid.add(new Label("单词颜色："), 0, 0);
        appearanceGrid.add(wordColor, 1, 0);
        appearanceGrid.add(new Label("词性颜色："), 0, 1);
        appearanceGrid.add(typeColor, 1, 1);
        appearanceGrid.add(new Label("释义颜色："), 0, 2);
        appearanceGrid.add(translationColor, 1, 2);
        appearanceGrid.add(new Label("短语颜色："), 0, 3);
        appearanceGrid.add(phraseColor, 1, 3);
        appearanceGrid.add(new Label("单词字号："), 0, 4);
        appearanceGrid.add(wordFontSize, 1, 4);
        appearanceGrid.add(new Label("释义字号："), 0, 5);
        appearanceGrid.add(detailFontSize, 1, 5);

        playbackRecordsBox = new VBox(6);
        playbackRecordsBox.setPadding(new Insets(4, 0, 0, 0));
        refreshPlaybackRecords();

        VBox generalPage = new VBox(10, groupBox("常规", generalGrid));
        VBox vocabularyPage = new VBox(10, groupBox("词汇本", vocabularyGrid), groupBox("自定义词汇", customGrid));
        VBox appearancePage = new VBox(10, groupBox("外观", appearanceGrid));
        VBox recordsPage = new VBox(10, groupBox("播放记录", playbackRecordsBox));
        for (VBox page : new VBox[]{generalPage, vocabularyPage, appearancePage, recordsPage}) {
            page.setPadding(new Insets(10));
        }

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #F6F8FC; -fx-tab-min-height: 30px; -fx-tab-max-height: 30px;");
        tabs.getTabs().addAll(
                settingsTab("常规", generalPage),
                settingsTab("词库", vocabularyPage),
                settingsTab("外观", appearancePage),
                settingsTab("记录", recordsPage)
        );

        Button close = compactButton("关闭");
        close.setOnAction(event -> stage.hide());
        Region spacer = new Region();
        HBox bottom = new HBox(8, spacer, close);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(0, 8, 0, 8));

        VBox root = new VBox(8, tabs, bottom);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #F6F8FC; -fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', 'SimSun'; -fx-font-size: 13px;");
        stage.setScene(new Scene(root, 560, 460));
        return stage;
    }

    private GridPane settingsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        return grid;
    }

    private Button compactButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(32);
        button.setPrefHeight(32);
        button.setStyle("-fx-background-color: #EEF2FF; -fx-text-fill: #52699A; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #D8E0F3; -fx-padding: 5 14 5 14;");
        return button;
    }

    private TextField compactTextField() {
        TextField textField = new TextField();
        textField.setPrefWidth(330);
        textField.setPrefHeight(32);
        styleModernControl(textField);
        return textField;
    }

    private VBox groupBox(String title, Node content) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #52699A; -fx-font-weight: bold; -fx-padding: 0 0 2 0;");
        VBox box = new VBox(8, label, content);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #E4E8F2; -fx-border-radius: 14; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 12, 0, 0, 3);");
        return box;
    }

    private void styleModernControl(Node node) {
        node.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #D8E0F3; -fx-padding: 3 8 3 8;");
    }

    private Tab settingsTab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private void applyVocabularyChoice(String choice, boolean reload) {
        if (choice == null || choice.trim().length() == 0) {
            return;
        }
        String previousPath = settings.getVocabularyPath();
        String nextPath = vocabularyPathForChoice(choice);
        if (!previousPath.equals(nextPath)) {
            settingsStore.savePlaybackProgress(settings, previousPath);
            settings.setVocabularyPath(nextPath);
            settings.setVocabularyFileName(vocabularyFileNameForChoice(choice));
            settings.resetPlaybackProgress();
            settingsStore.loadPlaybackProgress(settings, nextPath);
        }
        settingsStore.save(settings);
        if (reload) {
            reloadVocabulary();
        }
        refreshPlaybackRecords();
    }

    private String vocabularyPathForChoice(String choice) {
        if (CUSTOM_VOCABULARY_LABEL.equals(choice)) {
            return customVocabularyPath().toString();
        }
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (item.getFileName().equals(choice)) {
                return VocabularyCatalog.BASE_DIRECTORY + "/" + item.getFileName();
            }
        }
        return choice;
    }

    private String vocabularyFileNameForChoice(String choice) {
        if (CUSTOM_VOCABULARY_LABEL.equals(choice)) {
            return CUSTOM_VOCABULARY_LABEL;
        }
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (item.getFileName().equals(choice)) {
                return item.getFileName();
            }
        }
        return Paths.get(choice).getFileName().toString();
    }

    private String vocabularyChoiceForPath(String value) {
        String normalized = value == null ? "" : value.replace('\\', '/');
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (normalized.equals(item.getFileName())
                    || normalized.equals(VocabularyCatalog.BASE_DIRECTORY + "/" + item.getFileName())
                    || normalized.endsWith("/" + VocabularyCatalog.BASE_DIRECTORY + "/" + item.getFileName())) {
                return item.getFileName();
            }
        }
        if (normalized.equals(customVocabularyPath().toString().replace('\\', '/'))) {
            return CUSTOM_VOCABULARY_LABEL;
        }
        return value == null || value.trim().length() == 0 ? AppSettings.DEFAULT_VOCABULARY_FILE_NAME : value;
    }

    private Path customVocabularyPath() {
        return Paths.get(System.getProperty("user.home"), ".english-hugging-me", "custom-vocabulary.json");
    }

    private void addCustomWord(
            TextField customWord,
            TextField customType,
            TextField customPhrase,
            TextField customExample,
            TextField customMeaning
    ) {
        String word = customWord.getText().trim();
        if (word.length() == 0) {
            showError("自定义词汇失败", "请先填写单词");
            return;
        }

        String type = customType.getText().trim();
        String phrase = customPhrase.getText().trim();
        String example = customExample.getText().trim();
        String meaning = customMeaning.getText().trim();

        try {
            Path path = customVocabularyPath();
            List<WordEntry> words = new ArrayList<>();
            if (Files.exists(path)) {
                words.addAll(new VocabularyJsonLoader().load(path));
            }

            List<Translation> translations = meaning.length() == 0 && type.length() == 0
                    ? Collections.emptyList()
                    : Collections.singletonList(new Translation(meaning, type));
            List<Phrase> phrases = new ArrayList<>();
            if (phrase.length() > 0) {
                phrases.add(new Phrase(phrase, ""));
            }
            if (example.length() > 0) {
                phrases.add(new Phrase(example, meaning));
            }
            words.add(new WordEntry(word, translations, phrases));

            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(words, writer);
            }

            if (!vocabularyChoice.getItems().contains(CUSTOM_VOCABULARY_LABEL)) {
                vocabularyChoice.getItems().add(CUSTOM_VOCABULARY_LABEL);
            }
            vocabularyChoice.setValue(CUSTOM_VOCABULARY_LABEL);
            applyVocabularyChoice(CUSTOM_VOCABULARY_LABEL, true);
            customWord.clear();
            customType.clear();
            customPhrase.clear();
            customExample.clear();
            customMeaning.clear();
        } catch (Exception exception) {
            showError("自定义词汇失败", exception.getMessage());
        }
    }

    private void refreshPlaybackRecords() {
        if (playbackRecordsBox == null) {
            return;
        }
        playbackRecordsBox.getChildren().clear();
        playbackRecordsBox.getChildren().add(new Label("记录各词汇本当前顺序位置和随机播放数量。"));
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            String key = VocabularyCatalog.BASE_DIRECTORY + "/" + item.getFileName();
            playbackRecordsBox.getChildren().add(new Label(settingsStore.playbackRecordLine(key, item.getDisplayName())));
        }
        Path customPath = customVocabularyPath();
        if (Files.exists(customPath)) {
            playbackRecordsBox.getChildren().add(new Label(settingsStore.playbackRecordLine(customPath.toString(), CUSTOM_VOCABULARY_LABEL)));
        }
        String currentChoice = vocabularyChoiceForPath(settings.getVocabularyPath());
        if (!CUSTOM_VOCABULARY_LABEL.equals(currentChoice) && !isBuiltInVocabularyChoice(currentChoice)) {
            playbackRecordsBox.getChildren().add(new Label(settingsStore.playbackRecordLine(settings.getVocabularyPath(), currentChoice)));
        }
    }

    private boolean isBuiltInVocabularyChoice(String choice) {
        for (VocabularyCatalog.VocabularyItem item : VocabularyCatalog.items()) {
            if (item.getFileName().equals(choice)) {
                return true;
            }
        }
        return false;
    }

    private void reloadVocabulary() {
        try {
            Path vocabulary = resolveVocabularyPath(settings.getVocabularyPath());
            List<WordEntry> words;
            if (Files.exists(vocabulary)) {
                words = new VocabularyJsonLoader().load(vocabulary);
            } else {
                try (InputStream inputStream = FloatingWordsDesktopApp.class.getResourceAsStream("/" + settings.getVocabularyPath().replace('\\', '/'))) {
                    if (inputStream == null) {
                        throw new IOException("找不到词库：" + settings.getVocabularyPath());
                    }
                    words = new VocabularyJsonLoader().load(inputStream);
                }
            }
            startScheduler(words);
        } catch (Exception exception) {
            showError("词库加载失败", exception.getMessage());
            renderMessage("词库加载失败\n请在设置中选择 JSON 词库");
        }
    }

    private Path resolveVocabularyPath(String value) {
        Path path = Paths.get(value);
        if (path.isAbsolute() || Files.exists(path)) {
            return path;
        }

        Path applicationPath = applicationPath();
        if (applicationPath != null) {
            Path applicationDir = Files.isRegularFile(applicationPath) ? applicationPath.getParent() : applicationPath;
            Path packagedPath = applicationDir.resolve(path);
            if (Files.exists(packagedPath)) {
                return packagedPath;
            }
            if (applicationDir.getParent() != null) {
                packagedPath = applicationDir.getParent().resolve(path);
                if (Files.exists(packagedPath)) {
                    return packagedPath;
                }
            }
        }
        return path;
    }

    private Path applicationPath() {
        try {
            return Paths.get(FloatingWordsDesktopApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception ignored) {
            return null;
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
                settings.getRandomPlayedCount(),
                wordEntry -> Platform.runLater(() -> {
                    currentWord = wordEntry;
                    updateCurrentWord();
                }),
                (nextWordIndex, shuffleOrder, shufflePosition, randomPlayedCount) -> {
                    settings.setNextWordIndex(nextWordIndex);
                    settings.setShuffleOrder(shuffleOrder);
                    settings.setShufflePosition(shufflePosition);
                    settings.setRandomPlayedCount(randomPlayedCount);
                    settingsStore.save(settings);
                    settingsStore.savePlaybackProgress(settings, settings.getVocabularyPath());
                    Platform.runLater(this::refreshPlaybackRecords);
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

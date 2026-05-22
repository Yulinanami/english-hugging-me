package me.englishhugging.desktop.overlay;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import me.englishhugging.core.display.WordDisplayFormatter;
import me.englishhugging.core.model.WordDisplaySegment;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.OverlayMode;
import me.englishhugging.desktop.settings.DesktopSettingsStore;

import java.util.UUID;

public final class DesktopOverlayController {
    private static final int MOVE_HANDLE_SIZE = 42;
    private static final int RESIZE_HANDLE_SIZE = 42;

    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final WordDisplayFormatter wordDisplayFormatter = new WordDisplayFormatter();
    private final String overlayTitle = "English Hugging Me Overlay " + UUID.randomUUID();
    private final String moveHandleTitle = "English Hugging Me Move Handle " + UUID.randomUUID();
    private final String resizeHandleTitle = "English Hugging Me Resize Handle " + UUID.randomUUID();

    private Stage overlayStage;
    private Stage moveHandleStage;
    private Stage resizeHandleStage;
    private StackPane overlayRoot;
    private TextFlow wordFlow;
    private WordEntry currentWord;
    private double dragOffsetX, dragOffsetY;
    private double moveHandleDragOffsetX, moveHandleDragOffsetY;
    private double resizeStartScreenX, resizeStartScreenY, resizeStartWidth, resizeStartHeight;

    public DesktopOverlayController(AppSettings settings, DesktopSettingsStore settingsStore) {
        this.settings = settings;
        this.settingsStore = settingsStore;
    }

    public void init() {
        overlayStage = createOverlayStage();
        overlayStage.show();

        moveHandleStage = createMoveHandleStage();
        resizeHandleStage = createResizeHandleStage();
        moveHandleStage.show();
        resizeHandleStage.show();
        WindowsClickThrough.hideFromTaskbar(overlayStage);
        syncControlHandlePositions();
        applyOverlayMode();
    }

    public void close() {
        if (moveHandleStage != null) moveHandleStage.close();
        if (resizeHandleStage != null) resizeHandleStage.close();
        if (overlayStage != null) overlayStage.close();
    }

    public Stage getOverlayStage() { return overlayStage; }

    public void updateCurrentWord(WordEntry wordEntry) {
        currentWord = wordEntry;
        if (currentWord != null) {
            renderWord(currentWord);
            ensureOverlayFitsText();
        }
    }

    public void refreshDisplay() {
        if (currentWord != null) {
            renderWord(currentWord);
            ensureOverlayFitsText();
        }
    }

    public void applyOverlayMode() {
        boolean clickThrough = settings.getOverlayMode() == OverlayMode.CLICK_THROUGH;
        overlayRoot.setMouseTransparent(clickThrough);
        WindowsClickThrough.apply(overlayStage, clickThrough);
        syncControlHandlePositions();
    }

    public void showLoadingError() {
        renderMessage("词库加载失败\n请在设置中选择 JSON 词库");
    }

    public void showPlaybackFinished() {
        renderMessage("播放结束");
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

        overlayRoot.setOnMousePressed(event -> {
            if (settings.getOverlayMode() != OverlayMode.DRAGGABLE) return;
            dragOffsetX = stage.getX() - event.getScreenX();
            dragOffsetY = stage.getY() - event.getScreenY();
        });
        overlayRoot.setOnMouseDragged(event -> {
            if (settings.getOverlayMode() != OverlayMode.DRAGGABLE) return;
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
        stage.initOwner(overlayStage);
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

        overlayStage.xProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        overlayStage.yProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        overlayStage.widthProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        overlayStage.heightProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
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
        stage.initOwner(overlayStage);
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
        if (overlayStage == null) return;
        if (moveHandleStage != null) {
            moveHandleStage.setX(overlayStage.getX() + overlayStage.getWidth() - MOVE_HANDLE_SIZE - 8);
            moveHandleStage.setY(overlayStage.getY() + 4);
        }
        if (resizeHandleStage != null) {
            resizeHandleStage.setX(overlayStage.getX() + overlayStage.getWidth() - RESIZE_HANDLE_SIZE);
            resizeHandleStage.setY(overlayStage.getY() + overlayStage.getHeight() - RESIZE_HANDLE_SIZE);
        }
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
}

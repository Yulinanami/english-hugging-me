package me.englishhugging.desktop.overlay;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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
import me.englishhugging.desktop.ui.DesktopUi;

import java.util.List;
import java.util.UUID;

/**
 * 桌面端透明悬浮窗，负责在屏幕上渲染单词卡片。
 *
 * <p>这个类在屏幕上展示无边框的透明悬浮窗，并在右上角和右下角提供移动与调整尺寸的把手。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 初始化并显示悬浮窗
 * DesktopOverlayController overlay = new DesktopOverlayController(settings, store);
 * overlay.init();
 * 
 * // 更新显示的单词
 * overlay.updateCurrentWord(wordEntry);
 * </code></pre>
 */
public final class DesktopOverlayController {
    
    /** 移动把手尺寸（像素） */
    private static final int MOVE_HANDLE_SIZE = 42;
    
    /** 缩放把手尺寸（像素） */
    private static final int RESIZE_HANDLE_SIZE = 42;

    /** 应用配置 */
    private final AppSettings settings;
    
    /** 配置存储，用于保存最新窗口坐标与尺寸 */
    private final DesktopSettingsStore settingsStore;
    
    /** 单词文本拆分工具 */
    private final WordDisplayFormatter wordDisplayFormatter = new WordDisplayFormatter();
    
    /** 随机生成的窗口标题，防止被外部窗口探测工具干扰 */
    private final String overlayTitle = "English Hugging Me Overlay " + UUID.randomUUID();
    /** 移动把手窗口的专属标题 */
    private final String moveHandleTitle = "English Hugging Me Move Handle " + UUID.randomUUID();
    /** 缩放把手窗口的专属标题 */
    private final String resizeHandleTitle = "English Hugging Me Resize Handle " + UUID.randomUUID();

    /** 主悬浮窗窗口对象 */
    private Stage overlayStage;
    /** 右上角移动把手窗口对象 */
    private Stage moveHandleStage;
    /** 右下角缩放把手窗口对象 */
    private Stage resizeHandleStage;
    
    /** 主悬浮窗根布局节点 */
    private StackPane overlayRoot;
    /** 承载富文本高亮单词内容的 TextFlow 控件 */
    @FXML
    private TextFlow wordFlow;
    
    /** 当前正在悬浮窗上显示的单词条目 */
    private WordEntry currentWord;
    
    /** 拖拽移动主窗口时的 X 轴鼠标偏移量（像素） */
    private double dragOffsetX;
    /** 拖拽移动主窗口时的 Y 轴鼠标偏移量（像素） */
    private double dragOffsetY;
    /** 拖拽移动把手时的 X 轴鼠标偏移量（像素） */
    private double moveHandleDragOffsetX;
    /** 拖拽移动把手时的 Y 轴鼠标偏移量（像素） */
    private double moveHandleDragOffsetY;
    /** 开始拖拽缩放时鼠标在屏幕上的起始 X 坐标（像素） */
    private double resizeStartScreenX;
    /** 开始拖拽缩放时鼠标在屏幕上的起始 Y 坐标（像素） */
    private double resizeStartScreenY;
    /** 开始缩放时窗口的初始宽度（像素） */
    private double resizeStartWidth;
    /** 开始缩放时窗口的初始高度（像素） */
    private double resizeStartHeight;

    /**
     * 创建桌面悬浮窗。
     *
     * @param settings      应用配置
     * @param settingsStore 配置存储
     */
    public DesktopOverlayController(AppSettings settings, DesktopSettingsStore settingsStore) {
        this.settings = settings;
        this.settingsStore = settingsStore;
    }

    /**
     * 初始化悬浮窗及控制把手。
     */
    public void init() {
        this.overlayStage = createOverlayStage();
        this.overlayStage.show();

        this.moveHandleStage = createMoveHandleStage();
        this.resizeHandleStage = createResizeHandleStage();
        this.moveHandleStage.show();
        this.resizeHandleStage.show();
        
        // 隐藏在任务栏和 Alt-Tab 中的图标
        WindowsClickThrough.hideFromTaskbar(this.overlayStage);
        
        // 同步把手位置并应用交互模式
        syncControlHandlePositions();
        applyOverlayMode();
    }

    /**
     * 关闭并释放所有窗口资源。
     */
    public void close() {
        if (this.moveHandleStage != null) {
            this.moveHandleStage.close();
        }
        if (this.resizeHandleStage != null) {
            this.resizeHandleStage.close();
        }
        if (this.overlayStage != null) {
            this.overlayStage.close();
        }
    }

    /**
     * 获取悬浮窗窗口对象。
     *
     * @return 悬浮窗窗口对象
     */
    public Stage getOverlayStage() {
        return this.overlayStage;
    }

    /**
     * 更新悬浮窗中显示的单词。
     *
     * @param wordEntry 要显示的单词条目
     */
    public void updateCurrentWord(WordEntry wordEntry) {
        this.currentWord = wordEntry;
        if (this.currentWord != null) {
            renderWord(this.currentWord);
            ensureOverlayFitsText();
        }
    }

    /**
     * 针对设置面板中的字体、颜色更改等 UI 重绘事件，对旧单词触发手动刷新。
     */
    public void refreshDisplay() {
        if (this.currentWord != null) {
            renderWord(this.currentWord);
            ensureOverlayFitsText();
        }
    }

    /**
     * 将当前配置的“鼠标穿透”还是“可拖拽”交互模式应用到 Windows 窗口。
     */
    public void applyOverlayMode() {
        boolean isClickThrough = this.settings.getOverlayMode() == OverlayMode.CLICK_THROUGH;
        
        // 在 JavaFX 层面禁止接收事件
        this.overlayRoot.setMouseTransparent(isClickThrough);
        
        // 设置 Windows 窗口样式 WS_EX_TRANSPARENT 实现鼠标穿透
        WindowsClickThrough.apply(this.overlayStage, isClickThrough);
        
        // 模式切换时同步调整缩放把手位置
        syncControlHandlePositions();
    }

    /**
     * 显示启动异常时的错误反馈。
     */
    public void showLoadingError() {
        renderMessage("词库加载失败\n请在设置中选择 JSON 词库");
    }

    /**
     * 显示单词已经全部循环完毕的状态提示。
     */
    public void showPlaybackFinished() {
        renderMessage("播放结束");
    }

    /**
     * 创建主透明悬浮窗窗口。
     *
     * @return 主窗口对象
     */
    private Stage createOverlayStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.setTitle(this.overlayTitle);
        stage.setAlwaysOnTop(true);
        stage.setX(this.settings.getX());
        stage.setY(this.settings.getY());
        stage.setOpacity(this.settings.getOpacity());

        this.overlayRoot = DesktopUi.loadFxml("/fxml/overlay-window.fxml", this);
        renderMessage("正在加载...");
        
        // 限制文本最大尺寸随窗口大小自适应
        this.wordFlow.maxWidthProperty().bind(this.overlayRoot.widthProperty().subtract(60));
        this.wordFlow.maxHeightProperty().bind(this.overlayRoot.heightProperty().subtract(40));

        // 如果用户选择了“拖拽”模式，这部分主窗口的拖拽监听才会生效
        this.overlayRoot.setOnMousePressed(event -> {
            if (this.settings.getOverlayMode() != OverlayMode.DRAGGABLE) {
                return;
            }
            this.dragOffsetX = stage.getX() - event.getScreenX();
            this.dragOffsetY = stage.getY() - event.getScreenY();
        });
        
        this.overlayRoot.setOnMouseDragged(event -> {
            if (this.settings.getOverlayMode() != OverlayMode.DRAGGABLE) {
                return;
            }
            stage.setX(event.getScreenX() + this.dragOffsetX);
            stage.setY(event.getScreenY() + this.dragOffsetY);
            this.settings.setX(stage.getX());
            this.settings.setY(stage.getY());
            syncControlHandlePositions();
        });
        
        this.overlayRoot.setOnMouseReleased(event -> {
            if (this.settings.getOverlayMode() != OverlayMode.DRAGGABLE) {
                return;
            }
            this.settingsStore.save(this.settings);
        });

        Scene scene = new Scene(this.overlayRoot, this.settings.getWidth(), this.settings.getHeight());
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setMinWidth(260);
        stage.setMinHeight(80);
        
        return stage;
    }

    /**
     * 创建右上角的移动把手窗口。该把手独立于主窗口，不受鼠标穿透影响，任何时候都可以拖拽移动。
     *
     * @return 移动把手窗口对象
     */
    private Stage createMoveHandleStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(this.overlayStage);
        stage.setTitle(this.moveHandleTitle);
        stage.setAlwaysOnTop(true);

        StackPane moveHandle = DesktopUi.loadFxml("/fxml/overlay-move-handle.fxml");
        
        moveHandle.setOnMousePressed(event -> {
            this.moveHandleDragOffsetX = this.overlayStage.getX() - event.getScreenX();
            this.moveHandleDragOffsetY = this.overlayStage.getY() - event.getScreenY();
            event.consume();
        });
        
        // 拖拽把手时，同步移动主悬浮窗并更新配置中的坐标
        moveHandle.setOnMouseDragged(event -> {
            double nextX = event.getScreenX() + this.moveHandleDragOffsetX;
            double nextY = event.getScreenY() + this.moveHandleDragOffsetY;
            
            this.overlayStage.setX(nextX);
            this.overlayStage.setY(nextY);
            this.settings.setX(nextX);
            this.settings.setY(nextY);
            
            syncControlHandlePositions();
            event.consume();
        });
        
        // 松手时将坐标保存到本地文件
        moveHandle.setOnMouseReleased(event -> {
            this.settingsStore.save(this.settings);
            event.consume();
        });

        Scene scene = new Scene(moveHandle, MOVE_HANDLE_SIZE, MOVE_HANDLE_SIZE);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // 悬浮窗尺寸发生变化时，同步移动缩放把手位置
        this.overlayStage.xProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        this.overlayStage.yProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        this.overlayStage.widthProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        this.overlayStage.heightProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        
        return stage;
    }

    /**
     * 创建右下角的缩放把手窗口。
     */
    private Stage createResizeHandleStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(this.overlayStage);
        stage.setTitle(this.resizeHandleTitle);
        stage.setAlwaysOnTop(true);

        Pane resizeHandle = DesktopUi.loadFxml("/fxml/overlay-resize-handle.fxml");
        
        resizeHandle.setOnMousePressed(event -> {
            this.resizeStartScreenX = event.getScreenX();
            this.resizeStartScreenY = event.getScreenY();
            this.resizeStartWidth = this.overlayStage.getWidth();
            this.resizeStartHeight = this.overlayStage.getHeight();
            event.consume();
        });
        
        // 拖拽右下角把手时，根据鼠标移动的距离调整悬浮窗的宽和高
        resizeHandle.setOnMouseDragged(event -> {
            double nextWidth = Math.max(260, this.resizeStartWidth + event.getScreenX() - this.resizeStartScreenX);
            double nextHeight = Math.max(80, this.resizeStartHeight + event.getScreenY() - this.resizeStartScreenY);
            
            this.overlayStage.setWidth(nextWidth);
            this.overlayStage.setHeight(nextHeight);
            this.settings.setWidth(nextWidth);
            this.settings.setHeight(nextHeight);
            
            syncControlHandlePositions();
            event.consume();
        });
        
        resizeHandle.setOnMouseReleased(event -> {
            this.settingsStore.save(this.settings);
            event.consume();
        });

        Scene scene = new Scene(resizeHandle, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        
        return stage;
    }

    /**
     * 同步移动把手和缩放把手的位置，使其始终贴合主悬浮窗的右上角和右下角。
     */
    private void syncControlHandlePositions() {
        if (this.overlayStage == null) {
            return;
        }
        
        if (this.moveHandleStage != null) {
            this.moveHandleStage.setX(this.overlayStage.getX() + this.overlayStage.getWidth() - MOVE_HANDLE_SIZE - 8);
            this.moveHandleStage.setY(this.overlayStage.getY() + 4);
        }
        
        if (this.resizeHandleStage != null) {
            this.resizeHandleStage.setX(this.overlayStage.getX() + this.overlayStage.getWidth() - RESIZE_HANDLE_SIZE);
            this.resizeHandleStage.setY(this.overlayStage.getY() + this.overlayStage.getHeight() - RESIZE_HANDLE_SIZE);
        }
    }

    /**
     * 根据设置的颜色和字号，将单词各部分渲染到界面上。
     *
     * @param wordEntry 要渲染的单词条目
     */
    private void renderWord(WordEntry wordEntry) {
        renderSegments(this.wordDisplayFormatter.format(wordEntry, this.settings.getDisplayMode()));
    }

    /**
     * 渲染正在进行填空测试的单词。
     *
     * @param displayWord     当前填空状态下的单词字符串（包含挖空下划线）
     * @param originalEntry   原始单词条目
     * @param hidePhrases     是否隐藏例句短语
     * @param hideTranslation 是否隐藏中文释义
     */
    public void updateFillBlankWord(String displayWord, WordEntry originalEntry, boolean hidePhrases, boolean hideTranslation) {
        WordEntry tempEntry = new WordEntry(displayWord, originalEntry.translations(), originalEntry.phrases());
        renderSegments(this.wordDisplayFormatter.format(tempEntry, this.settings.getDisplayMode(), hidePhrases, hideTranslation));
        ensureOverlayFitsText();
    }

    /**
     * 将解析后的富文本片段逐一渲染并应用对应样式。
     */
    private void renderSegments(List<WordDisplaySegment> segments) {
        this.wordFlow.getChildren().clear();
        for (WordDisplaySegment segment : segments) {
            if (segment.type() == WordDisplaySegment.Type.WORD) {
                appendText(segment.text(), this.settings.getWordColor(), this.settings.getWordFontSize(), FontWeight.BOLD);
            } else if (segment.type() == WordDisplaySegment.Type.TYPE) {
                appendText(segment.text(), this.settings.getTypeColor(), this.settings.getDetailFontSize(), FontWeight.BOLD);
            } else if (segment.type() == WordDisplaySegment.Type.PHRASE) {
                appendText(segment.text(), this.settings.getPhraseColor(), this.settings.getDetailFontSize(), FontWeight.BOLD);
            } else {
                appendText(segment.text(), this.settings.getTranslationColor(), this.settings.getDetailFontSize(), FontWeight.NORMAL);
            }
        }
    }

    /**
     * 在悬浮窗中展示一行提示文本（如错误提示或加载中）。
     */
    private void renderMessage(String message) {
        this.wordFlow.getChildren().clear();
        appendText(message, this.settings.getWordColor(), this.settings.getDetailFontSize(), FontWeight.NORMAL);
    }

    /**
     * 向 TextFlow 中追加一段带样式的文本。
     *
     * @param value      显示的文字内容
     * @param color      十六进制颜色值（如 "#FFFFFF"）
     * @param fontSize   字号大小（像素）
     * @param fontWeight 字体加粗样式
     */
    private void appendText(String value, String color, int fontSize, FontWeight fontWeight) {
        Text text = new Text(value);
        text.setFill(Color.web(color));
        text.setFont(Font.font("Microsoft YaHei", fontWeight, fontSize));
        this.wordFlow.getChildren().add(text);
    }

    /**
     * 根据文本实际高度自动调整悬浮窗高度，防止文本被底部截断。
     */
    private void ensureOverlayFitsText() {
        Platform.runLater(() -> {
            double contentWidth = Math.max(200, this.overlayStage.getWidth() - 60);
            double requiredHeight = this.wordFlow.prefHeight(contentWidth) + 42;
            
            if (requiredHeight > this.overlayStage.getHeight()) {
                this.overlayStage.setHeight(requiredHeight);
                this.settings.setHeight(requiredHeight);
                this.settingsStore.save(this.settings);
                syncControlHandlePositions();
            }
        });
    }
}

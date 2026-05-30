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

/**
 * 桌面端透明悬浮窗的核心控制器。
 *
 * <p>这个类封装了所有 JavaFX 相关的 UI 构建逻辑，负责在屏幕上渲染透明背景的生词小卡片。
 * 它巧妙地利用了三个独立的无边框透明 {@link Stage}：一个用作主显示区，另外两个分别作为
 * “移动把手”和“缩放把手”，以此突破传统操作系统对于鼠标穿透（Click-Through）状态下无法拖拽的限制。
 * 
 * <p>所有的布局计算和刷新操作都确保安全地运行在 JavaFX 的 UI 线程（Platform.runLater）中。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 初始化与展示
 * DesktopOverlayController overlay = new DesktopOverlayController(settings, store);
 * overlay.init();
 * 
 * // 从调度引擎接收到新单词并渲染
 * overlay.updateCurrentWord(newWordEntry);
 * </code></pre>
 */
public final class DesktopOverlayController {
    
    /** 独立悬浮移动把手（右上方控制柄）的固定宽高 */
    private static final int MOVE_HANDLE_SIZE = 42;
    
    /** 独立悬浮缩放把手（右下方控制柄）的固定宽高 */
    private static final int RESIZE_HANDLE_SIZE = 42;

    /** 持有全局配置用于实时读取样式参数 */
    private final AppSettings settings;
    
    /** 持有存储引擎以便在拖拽完成后即时保存最新的坐标尺寸 */
    private final DesktopSettingsStore settingsStore;
    
    /** Core 模块提供的标准词条分片格式化器 */
    private final WordDisplayFormatter wordDisplayFormatter = new WordDisplayFormatter();
    
    // 生成随机的窗口标题以防止被恶意软件通过标题穷举捕捉
    private final String overlayTitle = "English Hugging Me Overlay " + UUID.randomUUID();
    private final String moveHandleTitle = "English Hugging Me Move Handle " + UUID.randomUUID();
    private final String resizeHandleTitle = "English Hugging Me Resize Handle " + UUID.randomUUID();

    // 核心 UI 窗口组件
    private Stage overlayStage;
    private Stage moveHandleStage;
    private Stage resizeHandleStage;
    
    // 主悬浮窗根布局与富文本流
    private StackPane overlayRoot;
    private TextFlow wordFlow;
    
    // 当前正在屏幕上驻留的词条数据模型
    private WordEntry currentWord;
    
    // 拖拽过程中的坐标偏移记录暂存器
    private double dragOffsetX;
    private double dragOffsetY;
    private double moveHandleDragOffsetX;
    private double moveHandleDragOffsetY;
    private double resizeStartScreenX;
    private double resizeStartScreenY;
    private double resizeStartWidth;
    private double resizeStartHeight;

    /**
     * 构造控制器实例。
     *
     * @param settings      包含所有 UI 尺寸、颜色偏好的运行时配置
     * @param settingsStore 用于落地修改（如缩放、拖拽后的状态）的持久化存储
     */
    public DesktopOverlayController(AppSettings settings, DesktopSettingsStore settingsStore) {
        this.settings = settings;
        this.settingsStore = settingsStore;
    }

    /**
     * 初始化三个透明窗口，并建立它们之间的绑定与同步联动关系。
     */
    public void init() {
        this.overlayStage = createOverlayStage();
        this.overlayStage.show();

        this.moveHandleStage = createMoveHandleStage();
        this.resizeHandleStage = createResizeHandleStage();
        this.moveHandleStage.show();
        this.resizeHandleStage.show();
        
        // 调用底层的 JNI/JNA 代码让这些窗口彻底从 Windows 的 Alt-Tab 任务栏和应用列表中消失
        WindowsClickThrough.hideFromTaskbar(this.overlayStage);
        
        // 初次同步把手位置并应用交互穿透模式
        syncControlHandlePositions();
        applyOverlayMode();
    }

    /**
     * 安全地销毁并释放所有相关的底层窗口资源。
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
     * 获取主显示窗口的引用，用于更高级别的生命周期控制。
     */
    public Stage getOverlayStage() {
        return this.overlayStage;
    }

    /**
     * 更新当前悬浮窗展示的内容为一枚全新的标准单词。
     *
     * @param wordEntry 将被显示的单词实体模型
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
     * 将当前配置的“鼠标穿透”还是“可拖拽”交互模式应用到底层 Windows 系统 API。
     */
    public void applyOverlayMode() {
        boolean isClickThrough = this.settings.getOverlayMode() == OverlayMode.CLICK_THROUGH;
        
        // 在 JavaFX 层面禁止接收事件
        this.overlayRoot.setMouseTransparent(isClickThrough);
        
        // 在 Windows 操作系统底层注入 WS_EX_TRANSPARENT 以实现绝对的鼠标穿透
        WindowsClickThrough.apply(this.overlayStage, isClickThrough);
        
        // 模式切换时重新对齐悬浮把手
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
     * 内部构建逻辑：创建占据大面积的主透明黑底显示窗口。
     *
     * @return 初始化的主 Stage
     */
    private Stage createOverlayStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.setTitle(this.overlayTitle);
        stage.setAlwaysOnTop(true);
        stage.setX(this.settings.getX());
        stage.setY(this.settings.getY());
        stage.setOpacity(this.settings.getOpacity());

        this.wordFlow = new TextFlow();
        this.wordFlow.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        renderMessage("正在加载...");

        this.overlayRoot = new StackPane(this.wordFlow);
        this.overlayRoot.setPadding(new Insets(14, 22, 14, 22));
        this.overlayRoot.setStyle("-fx-background-color: rgba(0,0,0,0.58); -fx-background-radius: 18;");
        
        // 防止文本把窗口无限制地撑大，动态绑定其内部最大尺寸到外层窗口
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
     * 内部构建逻辑：创建一个完全独立于主窗口的微型透明舞台，里面画着几个小圆点表示“把手”。
     * 这个把手永远不受主窗口鼠标穿透的影响，所以任何时候都能拖拽。
     *
     * @return 独立移动控制 Stage
     */
    private Stage createMoveHandleStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(this.overlayStage);
        stage.setTitle(this.moveHandleTitle);
        stage.setAlwaysOnTop(true);

        StackPane moveHandle = createMoveHandleNode();
        
        moveHandle.setOnMousePressed(event -> {
            this.moveHandleDragOffsetX = this.overlayStage.getX() - event.getScreenX();
            this.moveHandleDragOffsetY = this.overlayStage.getY() - event.getScreenY();
            event.consume();
        });
        
        // 拖拽控制把手时，同步修改和驱动主窗口的位移
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
        
        // 松手时将坐标持久化到本地文件
        moveHandle.setOnMouseReleased(event -> {
            this.settingsStore.save(this.settings);
            event.consume();
        });

        Scene scene = new Scene(moveHandle, MOVE_HANDLE_SIZE, MOVE_HANDLE_SIZE);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // 如果主窗口由于代码原因发生了尺寸变化，通过监听器让把手紧随其后移动
        this.overlayStage.xProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        this.overlayStage.yProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        this.overlayStage.widthProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        this.overlayStage.heightProperty().addListener((o, ov, nv) -> syncControlHandlePositions());
        
        return stage;
    }

    /**
     * 用 JavaFX 画布绘制移动把手的内部图形（3行2列的半透明白点）。
     */
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
        // 背景设置为万分之一不透明度，骗过底层鼠标点击检测
        handle.setStyle("-fx-background-color: rgba(255,255,255,0.01); -fx-cursor: move;");
        
        return handle;
    }

    /**
     * 内部构建逻辑：创建右下角的独立微型缩放舞台。
     */
    private Stage createResizeHandleStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(this.overlayStage);
        stage.setTitle(this.resizeHandleTitle);
        stage.setAlwaysOnTop(true);

        Pane resizeHandle = createResizeHandleNode();
        
        resizeHandle.setOnMousePressed(event -> {
            this.resizeStartScreenX = event.getScreenX();
            this.resizeStartScreenY = event.getScreenY();
            this.resizeStartWidth = this.overlayStage.getWidth();
            this.resizeStartHeight = this.overlayStage.getHeight();
            event.consume();
        });
        
        // 拖拽右下角的控制柄时，根据鼠标移动的距离差增减主舞台的宽和高
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
     * 绘制右下角缩放把手的小三角形条纹纹理。
     */
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

    /**
     * 核心对齐算法：不论主窗口被拖拉到哪里，两个独立的把手都会如同吸铁石一般紧紧黏附在它的右上和右下角。
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
     * 将从 Core 模型下发的单词，通过配置设定的颜色、字号将其映射为一组五彩斑斓的 JavaFX 文本块。
     */
    private void renderWord(WordEntry wordEntry) {
        this.wordFlow.getChildren().clear();
        
        for (WordDisplaySegment segment : this.wordDisplayFormatter.format(wordEntry, this.settings.getDisplayMode())) {
            if (segment.getType() == WordDisplaySegment.Type.WORD) {
                appendText(segment.getText(), this.settings.getWordColor(), this.settings.getWordFontSize(), FontWeight.BOLD);
            } else if (segment.getType() == WordDisplaySegment.Type.TYPE) {
                appendText(segment.getText(), this.settings.getTypeColor(), this.settings.getDetailFontSize(), FontWeight.BOLD);
            } else if (segment.getType() == WordDisplaySegment.Type.PHRASE) {
                appendText(segment.getText(), this.settings.getPhraseColor(), this.settings.getDetailFontSize(), FontWeight.BOLD);
            } else {
                appendText(segment.getText(), this.settings.getTranslationColor(), this.settings.getDetailFontSize(), FontWeight.NORMAL);
            }
        }
    }

    /**
     * 在填空考核阶段专用的渲染通道，支持隐去提示信息和展示带有下划线的半成单词。
     *
     * @param displayWord     包含底线的需要展示的词
     * @param originalEntry   关联的原始参考模型
     * @param hidePhrases     当前状态机的参数是否要求抹除例句
     * @param hideTranslation 当前状态机参数是否要求抹除翻译
     */
    public void updateFillBlankWord(String displayWord, WordEntry originalEntry, boolean hidePhrases, boolean hideTranslation) {
        // 利用原始属性组合成一个虚假的填空模型去欺骗 Formatter 渲染颜色分片
        WordEntry tempEntry = new WordEntry(displayWord, originalEntry.getTranslations(), originalEntry.getPhrases());
        this.wordFlow.getChildren().clear();
        
        for (WordDisplaySegment segment : this.wordDisplayFormatter.format(tempEntry, this.settings.getDisplayMode(), hidePhrases, hideTranslation)) {
            if (segment.getType() == WordDisplaySegment.Type.WORD) {
                appendText(segment.getText(), this.settings.getWordColor(), this.settings.getWordFontSize(), FontWeight.BOLD);
            } else if (segment.getType() == WordDisplaySegment.Type.TYPE) {
                appendText(segment.getText(), this.settings.getTypeColor(), this.settings.getDetailFontSize(), FontWeight.BOLD);
            } else if (segment.getType() == WordDisplaySegment.Type.PHRASE) {
                appendText(segment.getText(), this.settings.getPhraseColor(), this.settings.getDetailFontSize(), FontWeight.BOLD);
            } else {
                appendText(segment.getText(), this.settings.getTranslationColor(), this.settings.getDetailFontSize(), FontWeight.NORMAL);
            }
        }
        
        ensureOverlayFitsText();
    }

    /**
     * 单纯地展示一行普通的提示信息（例如错误、或者正在加载）。
     */
    private void renderMessage(String message) {
        this.wordFlow.getChildren().clear();
        appendText(message, this.settings.getWordColor(), this.settings.getDetailFontSize(), FontWeight.NORMAL);
    }

    /**
     * JavaFX 文本拼接小工具。
     *
     * @param value      需要被绘制的文字字符串
     * @param color      十六进制的颜色码（如 "#FFFFFF"）
     * @param fontSize   绝对字号
     * @param fontWeight 字重枚举
     */
    private void appendText(String value, String color, int fontSize, FontWeight fontWeight) {
        Text text = new Text(value);
        text.setFill(Color.web(color));
        text.setFont(Font.font("Microsoft YaHei", fontWeight, fontSize));
        this.wordFlow.getChildren().add(text);
    }

    /**
     * 根据当前 TextFlow 渲染的内容块高度，自动伸缩外壳 Stage，防止底部截断。
     * 所有的运算和 Stage 高度修改必须强制推入主线程的渲染队列中执行。
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

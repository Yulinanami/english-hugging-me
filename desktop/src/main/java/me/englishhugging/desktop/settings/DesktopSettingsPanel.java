package me.englishhugging.desktop.settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.ui.DesktopUi;

import java.io.InputStream;

/**
 * 桌面端系统首选项主面板。
 *
 * <p>这个类构建了一个包含多个选项卡（Tabs）的标准窗口，用于供用户修改程序的行为。
 * 它本身并不负责绘制具体的每一页内容，而是将每个 Tab 的逻辑委托给了专门的拆分类
 * （如 {@link GeneralSettingsTab}）。这种“组合优于继承”的架构极大地降低了单个类的体积。
 *
 * <p>这个面板具有特殊性：它的右上角叉号或者点击任务栏收起并不会真正关闭程序，
 * 而是将其隐藏至后台，以便悬浮窗持续运行。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * DesktopSettingsPanel panel = new DesktopSettingsPanel(...);
 * panel.init();
 * 
 * // 从系统托盘唤醒面板
 * panel.show();
 * </code></pre>
 */
public final class DesktopSettingsPanel {

    /** 应用的主图标路径，用于设置窗口的左上角以及任务栏展示 */
    private static final String APP_ICON_RESOURCE = "/icons/app.png";

    // --- 全局依赖注入 ---
    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final DesktopOverlayController overlayController;
    
    // --- 事件回调 ---
    private final Runnable onSettingsChanged;
    private final Runnable onVocabularyChanged;

    // --- 内部状态缓存 ---
    private Stage settingsStage;
    private PlaybackRecordsTab recordsTab;
    private Image appIcon;

    /**
     * 构造面板骨架。
     *
     * @param settings            全局配置参数引用
     * @param settingsStore       持久化保存引擎
     * @param overlayController   悬浮窗控制柄（用于即时应用颜色、尺寸修改）
     * @param onSettingsChanged   普通参数更改时通知引擎的回调
     * @param onVocabularyChanged 词库变更时要求引擎大洗牌的回调
     */
    public DesktopSettingsPanel(
            AppSettings settings,
            DesktopSettingsStore settingsStore,
            DesktopOverlayController overlayController,
            Runnable onSettingsChanged,
            Runnable onVocabularyChanged
    ) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.overlayController = overlayController;
        this.onSettingsChanged = onSettingsChanged;
        this.onVocabularyChanged = onVocabularyChanged;
    }

    /**
     * 构建 JavaFX 的底层 Stage 并组合所有的子面板。
     */
    public void init() {
        this.settingsStage = createSettingsStage();
    }

    /**
     * 优雅地将面板带到前台。如果面板原本被最小化，则将其还原并推向顶层。
     */
    public void show() {
        this.settingsStage.show();
        this.settingsStage.setIconified(false);
        this.settingsStage.toFront();
    }

    /**
     * 暴露给主程序的刷新接口：当定时器后台播完了某些单词，或者跨设备同步了进度时，
     * 能够通知数据展示面板重新拉取历史记录。
     */
    public void refreshPlaybackRecords() {
        if (this.recordsTab != null) {
            this.recordsTab.refresh();
        }
    }

    /**
     * 内部组装工厂：缝合各路 Tab 和底部控制条，完成主窗口渲染。
     */
    private Stage createSettingsStage() {
        Stage stage = new Stage();
        stage.setTitle("English Hugging Me 首选项");
        applyStageIcon(stage);
        
        // 覆盖默认行为：右上角关闭 = 隐藏
        stage.setOnCloseRequest(event -> {
            event.consume();
            stage.hide();
        });
        
        // 覆盖默认行为：窗口最小化 = 隐藏
        stage.iconifiedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                stage.setIconified(false);
                stage.hide();
            }
        });

        // 实例化分拆的五个子页面逻辑
        GeneralSettingsTab generalTab = new GeneralSettingsTab(
                this.settings, this.settingsStore, this.overlayController, 
                this.onSettingsChanged, this.onVocabularyChanged
        );
        
        VocabularySettingsTab vocabularyTab = new VocabularySettingsTab(
                this.settings, this.settingsStore, stage, this.onVocabularyChanged
        );
        
        CustomVocabularyTab customTab = new CustomVocabularyTab(this.onVocabularyChanged);
        
        AppearanceSettingsTab appearanceTab = new AppearanceSettingsTab(
                this.settings, this.settingsStore, this.overlayController
        );
        
        this.recordsTab = new PlaybackRecordsTab(this.settings, this.settingsStore, vocabularyTab);

        // 封装进 TabPane 选项卡容器
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #F6F8FC; -fx-tab-min-height: 30px; -fx-tab-max-height: 30px;");
        
        tabs.getTabs().addAll(
                DesktopUi.settingsTab("常规", generalTab.createContent()),
                DesktopUi.settingsTab("词库", vocabularyTab.createContent()),
                DesktopUi.settingsTab("自定义", customTab.createContent()),
                DesktopUi.settingsTab("外观", appearanceTab.createContent()),
                DesktopUi.settingsTab("记录", this.recordsTab.createContent())
        );

        // 构建底部的安全退出按钮
        Button closeButton = DesktopUi.compactButton("退出程序");
        closeButton.setOnAction(event -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });
        
        Region spacer = new Region();
        HBox bottom = new HBox(8, spacer, closeButton);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(0, 8, 0, 8));

        // 拼接最外层结构
        VBox root = new VBox(8, tabs, bottom);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #F6F8FC; -fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', 'SimSun'; -fx-font-size: 13px;");
        
        stage.setScene(new Scene(root, 560, 460));
        return stage;
    }

    /**
     * 为窗口挂载左上角的应用程序小图标。
     */
    private void applyStageIcon(Stage stage) {
        Image icon = appIcon();
        if (icon != null) {
            stage.getIcons().add(icon);
        }
    }

    /**
     * 延迟加载并缓存图标资源。
     */
    private Image appIcon() {
        if (this.appIcon == null) {
            try (InputStream in = DesktopSettingsPanel.class.getResourceAsStream(APP_ICON_RESOURCE)) {
                if (in != null) {
                    this.appIcon = new Image(in);
                }
            } catch (Exception ignored) {
                // 如果没有图片资源也不影响主程序运行，只是缺少美观
            }
        }
        return this.appIcon;
    }
}

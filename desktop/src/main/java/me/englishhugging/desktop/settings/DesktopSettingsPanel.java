package me.englishhugging.desktop.settings;

import atlantafx.base.theme.Styles;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.ui.DesktopUi;

import java.io.InputStream;

/**
 * 桌面端主设置窗口，集中管理常规、词库、自定义、外观和学习记录五个设置页面。
 *
 * <p>负责主设置窗口的显示与隐藏，关闭或最小化时会自动退到系统托盘后台运行。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * DesktopSettingsPanel panel = new DesktopSettingsPanel(
 *         settings,
 *         settingsStore,
 *         overlayController,
 *         () -> onSettingsUpdated(),
 *         () -> onVocabularyUpdated()
 * );
 * panel.init();
 * panel.show();
 * </code></pre>
 */
public final class DesktopSettingsPanel {
    /** 图标资源路径 */
    private static final String APP_ICON_RESOURCE = "/icons/app.png";

    /** 应用配置 */
    private final AppSettings settings;
    /** 配置存储 */
    private final DesktopSettingsStore settingsStore;
    /** 桌面悬浮窗 */
    private final DesktopOverlayController overlayController;
    /** 播放参数改变时的刷新回调 */
    private final Runnable onSettingsChanged;
    /** 词库或播放模式切换时的刷新回调 */
    private final Runnable onVocabularyChanged;

    /** 常规设置页 */
    @FXML
    private Tab generalTab;
    /** 词库选择页 */
    @FXML
    private Tab vocabularyTab;
    /** 自定义词库页 */
    @FXML
    private Tab customVocabularyTab;
    /** 外观样式页 */
    @FXML
    private Tab appearanceTab;
    /** 学习记录页 */
    @FXML
    private Tab recordsTabContainer;
    /** 退出程序按钮 */
    @FXML
    private Button exitButton;

    /** 设置窗口 */
    private Stage settingsStage;
    /** 学习记录页面对象 */
    private PlaybackRecordsTab recordsTab;
    /** 应用图标 */
    private Image appIcon;

    /**
     * 创建桌面端设置窗口。
     *
     * @param settings            应用配置
     * @param settingsStore       配置存储
     * @param overlayController   桌面悬浮窗
     * @param onSettingsChanged   播放参数改变时的刷新回调
     * @param onVocabularyChanged 词库或播放模式切换时的刷新回调
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
     * 初始化并创建设置窗口。
     */
    public void init() {
        this.settingsStage = createSettingsStage();
    }

    /**
     * 打开并置顶显示设置窗口。
     */
    public void show() {
        this.settingsStage.show();
        this.settingsStage.setIconified(false);
        this.settingsStage.toFront();
    }

    /**
     * 刷新学习记录页中的数据列表。
     */
    public void refreshPlaybackRecords() {
        if (this.recordsTab != null) {
            this.recordsTab.refresh();
        }
    }

    /**
     * 创建设置窗口并添加各个选项卡页面。
     *
     * @return 设置窗口对象
     */
    private Stage createSettingsStage() {
        Stage stage = new Stage();
        stage.setTitle("English Hugging Me 首选项");
        applyStageIcon(stage);

        // 关闭和最小化只隐藏设置窗口，悬浮窗与托盘继续运行。
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

        Parent root = DesktopUi.loadFxml("/fxml/settings-panel.fxml", this);
        this.exitButton.getStyleClass().add(Styles.DANGER);

        GeneralSettingsTab generalController = new GeneralSettingsTab(
                this.settings,
                this.settingsStore,
                this.overlayController,
                this.onSettingsChanged,
                this.onVocabularyChanged
        );
        VocabularySettingsTab vocabularyController = new VocabularySettingsTab(
                this.settings,
                this.settingsStore,
                stage,
                this.onVocabularyChanged
        );
        CustomVocabularyTab customController = new CustomVocabularyTab(
                this.onVocabularyChanged
        );
        AppearanceSettingsTab appearanceController = new AppearanceSettingsTab(
                this.settings,
                this.settingsStore,
                this.overlayController
        );
        this.recordsTab = new PlaybackRecordsTab(
                this.settings,
                this.settingsStore,
                vocabularyController
        );

        this.generalTab.setContent(generalController.createContent());
        this.vocabularyTab.setContent(vocabularyController.createContent());
        this.customVocabularyTab.setContent(customController.createContent());
        this.appearanceTab.setContent(appearanceController.createContent());
        this.recordsTabContainer.setContent(this.recordsTab.createContent());

        Scene scene = new Scene(root, 720, 600);
        stage.setScene(scene);
        return stage;
    }

    /**
     * 退出应用程序。
     */
    @FXML
    private void exitApplication() {
        javafx.application.Platform.exit();
        System.exit(0);
    }

    /**
     * 为窗口设置应用图标。
     *
     * @param stage 目标窗口
     */
    private void applyStageIcon(Stage stage) {
        Image icon = appIcon();
        if (icon != null) {
            stage.getIcons().add(icon);
        }
    }

    /**
     * 加载应用图标。
     *
     * @return 图标图片对象，如果加载失败则返回 null
     */
    private Image appIcon() {
        if (this.appIcon == null) {
            try (InputStream input = DesktopSettingsPanel.class.getResourceAsStream(
                    APP_ICON_RESOURCE
            )) {
                if (input != null) {
                    this.appIcon = new Image(input);
                }
            } catch (Exception ignored) {
                // 图标缺失不影响窗口运行
            }
        }
        return this.appIcon;
    }
}

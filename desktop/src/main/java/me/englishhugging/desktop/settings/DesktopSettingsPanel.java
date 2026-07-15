package me.englishhugging.desktop.settings;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.ui.DesktopUi;

import java.io.InputStream;

/**
 * 桌面端系统首选项窗口及各 FXML 设置页的装配控制器。
 *
 * <p>窗口和选项卡结构由 FXML 声明；本类只负责注入业务依赖、装配各页面控制器，
 * 以及保留“关闭或最小化时隐藏到托盘”的窗口生命周期行为。</p>
 */
public final class DesktopSettingsPanel {
    /** 应用图标的 classpath 资源路径。 */
    private static final String APP_ICON_RESOURCE = "/icons/app.png";

    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final DesktopOverlayController overlayController;
    private final Runnable onSettingsChanged;
    private final Runnable onVocabularyChanged;

    @FXML
    private Tab generalTab;
    @FXML
    private Tab vocabularyTab;
    @FXML
    private Tab customVocabularyTab;
    @FXML
    private Tab appearanceTab;
    @FXML
    private Tab recordsTabContainer;

    private Stage settingsStage;
    private PlaybackRecordsTab recordsTab;
    private Image appIcon;

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

    /** 创建设置窗口并加载全部 FXML 页面。 */
    public void init() {
        this.settingsStage = createSettingsStage();
    }

    /** 显示设置窗口；若窗口已最小化则先恢复。 */
    public void show() {
        this.settingsStage.show();
        this.settingsStage.setIconified(false);
        this.settingsStage.toFront();
    }

    /** 播放进度变化后刷新记录页。 */
    public void refreshPlaybackRecords() {
        if (this.recordsTab != null) {
            this.recordsTab.refresh();
        }
    }

    /** 组装窗口生命周期、FXML 根节点和五个设置页控制器。 */
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

        // 为卡片式设置页面提供舒适的默认阅读空间，窗口仍可由用户自由缩放。
        Scene scene = new Scene(root, 720, 600);
        DesktopUi.applyStylesheet(scene);
        stage.setScene(scene);
        return stage;
    }

    /** 由 FXML 底部按钮触发，彻底退出桌面程序。 */
    @FXML
    private void exitApplication() {
        javafx.application.Platform.exit();
        System.exit(0);
    }

    private void applyStageIcon(Stage stage) {
        Image icon = appIcon();
        if (icon != null) {
            stage.getIcons().add(icon);
        }
    }

    /** 延迟加载并缓存窗口图标。 */
    private Image appIcon() {
        if (this.appIcon == null) {
            try (InputStream input = DesktopSettingsPanel.class.getResourceAsStream(
                    APP_ICON_RESOURCE
            )) {
                if (input != null) {
                    this.appIcon = new Image(input);
                }
            } catch (Exception ignored) {
                // 图标缺失不影响设置窗口和悬浮窗运行。
            }
        }
        return this.appIcon;
    }
}

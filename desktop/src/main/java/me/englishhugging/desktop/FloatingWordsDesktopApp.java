package me.englishhugging.desktop;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import me.englishhugging.core.WordScheduler;
import me.englishhugging.core.WordSchedulerConfig;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.overlay.ScreenStateMonitor;
import me.englishhugging.desktop.settings.DesktopSettingsPanel;
import me.englishhugging.desktop.settings.DesktopSettingsStore;
import me.englishhugging.desktop.ui.DesktopTrayController;

import java.util.List;

/**
 * 桌面端主程序启动入口。
 *
 * <p>这个类继承了 JavaFX 的 {@link Application}，是整个 Windows/Mac 桌面端程序的启动主类。
 * 它负责初始化设置存储、透明悬浮窗、托盘图标、屏幕状态监听，并启动后台单词播放任务。
 * 所有的异常和崩溃最终也都会抛到这里由统一的对话框处理。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 在 main 函数中启动本应用
 * Application.launch(FloatingWordsDesktopApp.class, args);
 * </code></pre>
 */
public final class FloatingWordsDesktopApp extends Application {
    
    /** 配置存储 */
    private final DesktopSettingsStore settingsStore = new DesktopSettingsStore();

    /** 应用配置 */
    private AppSettings settings;
    /** 桌面悬浮窗 */
    private DesktopOverlayController overlayController;
    /** 设置窗口 */
    private DesktopSettingsPanel settingsPanel;
    /** 系统托盘 */
    private DesktopTrayController trayController;
    /** 单词播放后台任务 */
    private WordScheduler scheduler;
    /** 锁屏与息屏监听工具 */
    private ScreenStateMonitor screenMonitor;

    /**
     * JavaFX 应用启动入口。
     * 依次初始化配置存储、悬浮窗界面、系统托盘以及屏幕状态监听工具。
     *
     * @param primaryStage JavaFX 默认传入的主窗口（本应用使用自定义悬浮窗，故忽略该参数）
     */
    @Override
    public void start(Stage primaryStage) {
        // 1. 初始化全局 UI 皮肤
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        
        // 禁止所有窗口关闭时自动退出应用，因为我们要常驻系统托盘
        Platform.setImplicitExit(false);

        // 2. 加载用户配置与最新进度
        this.settings = this.settingsStore.load();
        this.settingsStore.loadPlaybackProgress(this.settings, this.settings.getVocabularyPath());

        // 3. 构建透明的单词悬浮显示卡片
        this.overlayController = new DesktopOverlayController(this.settings, this.settingsStore);
        this.overlayController.init();

        // 4. 构建设置面板，并设置回调以便随时更新播放配置
        this.settingsPanel = new DesktopSettingsPanel(
                this.settings, 
                this.settingsStore, 
                this.overlayController,
                () -> {
                    if (this.scheduler != null) {
                        this.scheduler.updateIntervalSeconds(this.settings.getIntervalSeconds());
                        this.scheduler.updateFillBlankSettings(
                                this.settings.isFillBlankMode(),
                                this.settings.getFillBlankIntervalSeconds(),
                                this.settings.isFillBlankHidePhrases(),
                                this.settings.isFillBlankShowTranslation()
                        );
                    }
                },
                this::reloadVocabulary
        );
        this.settingsPanel.init();

        // 5. 安装系统右下角托盘图标并加载词库开始播放
        installTrayIcon();
        reloadVocabulary();
        
        // 首次启动时显示设置面板
        this.settingsPanel.show();

        // 6. 开启 Windows 屏幕监控，锁屏时自动暂停播放节省系统资源
        this.screenMonitor = new ScreenStateMonitor(
                () -> {
                    if (this.scheduler != null) {
                        this.scheduler.pause();
                    }
                },
                () -> {
                    if (this.scheduler != null) {
                        this.scheduler.resume();
                    }
                }
        );
        this.screenMonitor.start();
    }

    /**
     * 桌面应用退出时的清理方法，释放后台任务资源并保存最终状态。
     */
    @Override
    public void stop() {
        if (this.screenMonitor != null) {
            this.screenMonitor.stop();
            this.screenMonitor = null;
        }
        
        if (this.scheduler != null) {
            this.scheduler.stop();
        }
        
        if (this.settings != null) {
            this.settingsStore.save(this.settings);
        }
        
        if (this.overlayController != null) {
            this.overlayController.close();
        }
        
        removeTrayIcon();
    }

    /**
     * 重新加载词库。在启动或用户在设置面板切换了新词库时调用。
     */
    private void reloadVocabulary() {
        try {
            List<WordEntry> words = DesktopVocabularyLoader.load(this.settings.getVocabularyPath());
            startScheduler(words);
        } catch (Exception e) {
            showError("词库加载失败", e.getMessage());
            this.overlayController.showLoadingError();
        }
    }

    /**
     * 停止旧任务，根据最新的词库启动定时播放。
     *
     * @param words 校验过的词汇列表
     */
    private void startScheduler(List<WordEntry> words) {
        if (this.scheduler != null) {
            this.scheduler.stop();
        }
        
        this.scheduler = new WordScheduler(
                words,
                WordSchedulerConfig.fromAppSettings(this.settings),
                new WordScheduler.Listener() {
                    @Override 
                    public void onWord(WordEntry wordEntry) { 
                        Platform.runLater(() -> overlayController.updateCurrentWord(wordEntry)); 
                    }
                    
                    @Override 
                    public void onFillBlankWord(String displayWord, WordEntry originalEntry, boolean hidePhrases, boolean hideTranslation) {
                        Platform.runLater(() -> overlayController.updateFillBlankWord(displayWord, originalEntry, hidePhrases, hideTranslation));
                    }
                    
                    @Override 
                    public void onPlaybackFinished() {
                        Platform.runLater(() -> {
                            overlayController.showPlaybackFinished();
                        });
                    }
                },
                progress -> {
                    // 收到进度更新时，保存到配置文件
                    settings.setPlaybackProgress(progress);

                    settingsStore.save(settings);
                    settingsStore.savePlaybackProgress(settings, settings.getVocabularyPath());

                    // 刷新学习记录
                    Platform.runLater(() -> settingsPanel.refreshPlaybackRecords());
                }
        );
        this.scheduler.start();
    }

    /**
     * 初始化系统托盘。
     */
    private void installTrayIcon() {
        this.trayController = new DesktopTrayController(
                this.overlayController.getOverlayStage(), 
                () -> this.settingsPanel.show(), 
                this::exitApplication
        );
        
        // 如果托盘安装失败（例如运行在不支持的环境下），则直接显示设置面板
        if (!this.trayController.install()) {
            this.settingsPanel.show();
        }
    }

    /**
     * 移除系统托盘图标。
     */
    private void removeTrayIcon() {
        if (this.trayController != null) {
            this.trayController.remove();
            this.trayController = null;
        }
    }

    /**
     * 响应用户主动点击退出程序的事件。
     */
    private void exitApplication() {
        removeTrayIcon();
        Platform.setImplicitExit(true);
        Platform.exit();
    }

    /**
     * 错误弹窗，向用户提示文件读取失败等异常信息。
     *
     * @param title   错误主标题
     * @param message 错误详细信息
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            
            if (message == null) {
                alert.setContentText("未知错误");
            } else {
                alert.setContentText(message);
            }
            
            alert.showAndWait();
        });
    }
}

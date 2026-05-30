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
 * 桌面端的主入口和生命周期调度中枢。
 *
 * <p>这个类继承了 JavaFX 的 {@link Application}，是整个 Windows/Mac 程序的总指挥。
 * 它负责串联和初始化设置存储、透明悬浮窗、托盘图标、屏幕监控器，以及驱动 Core 模块中的 {@link WordScheduler}。
 * 所有的异常和崩溃最终也都会抛到这里由统一的对话框处理。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 在 main 函数中启动本应用
 * Application.launch(FloatingWordsDesktopApp.class, args);
 * </code></pre>
 */
public final class FloatingWordsDesktopApp extends Application {
    
    /** 持久化设置的本地实现 */
    private final DesktopSettingsStore settingsStore = new DesktopSettingsStore();

    private AppSettings settings;
    private DesktopOverlayController overlayController;
    private DesktopSettingsPanel settingsPanel;
    private DesktopTrayController trayController;
    private WordScheduler scheduler;
    private ScreenStateMonitor screenMonitor;

    /**
     * JavaFX 应用生命周期的启动钩子。
     * 在这里我们按严格的顺序（存储 -> UI -> 托盘 -> 监控器）进行全家桶的装配。
     *
     * @param primaryStage JavaFX 注入的主舞台（但在此处我们使用自定义的多 Stage 架构，故忽略它）
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

        // 4. 构建配置面板面板，并注入回调以便随时热更新调度器
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

        // 5. 安装系统右下角托盘图标并加载词库发车
        installTrayIcon();
        reloadVocabulary();
        
        // 第一次启动，强制弹除设置面板引导用户
        this.settingsPanel.show();

        // 6. 开启 Windows 屏幕监控，锁屏时自动暂停播放节省性能
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
     * JavaFX 生命周期的销毁钩子，在这里安全释放所有线程和持久化最后的状态。
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
     * 热重载词汇表。在启动或用户在设置面板切换了新的 JSON 词库时调用。
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
     * 停止旧引擎，根据最新的词汇表实例化全新的 {@link WordScheduler} 引擎并开始泵送单词。
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
                (nextWordIndex, shuffleOrder, shufflePosition, randomPlayedCount) -> {
                    // 当收到引擎的进度回调时，同步更新内存模型并落地
                    settings.setNextWordIndex(nextWordIndex);
                    settings.setShuffleOrder(shuffleOrder);
                    settings.setShufflePosition(shufflePosition);
                    settings.setRandomPlayedCount(randomPlayedCount);
                    
                    settingsStore.save(settings);
                    settingsStore.savePlaybackProgress(settings, settings.getVocabularyPath());
                    
                    // 通知面板刷新学习记录统计
                    Platform.runLater(() -> settingsPanel.refreshPlaybackRecords());
                }
        );
        this.scheduler.start();
    }

    /**
     * 组装右下角的系统托盘控制器。
     */
    private void installTrayIcon() {
        this.trayController = new DesktopTrayController(
                this.overlayController.getOverlayStage(), 
                () -> this.settingsPanel.show(), 
                this::exitApplication
        );
        
        // 如果托盘安装失败（例如运行在不支持的环境下），就让主设置面板弹出来，以免应用变鬼影
        if (!this.trayController.install()) {
            this.settingsPanel.show();
        }
    }

    /**
     * 剥除右下角的托盘图标。
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
     * 顶层错误弹窗，所有的灾难性错误（如读取文件权限被拒）都通过这里报告给用户。
     *
     * @param title   错误主标题
     * @param message 错误栈信息或者自定义细节
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

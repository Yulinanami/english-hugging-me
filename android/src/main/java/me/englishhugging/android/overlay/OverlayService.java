package me.englishhugging.android.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.WindowManager;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.R;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.core.WordScheduler;
import me.englishhugging.core.WordSchedulerConfig;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.vocabulary.VocabularyJsonLoader;

/**
 * Android 手机端桌面悬浮窗后台服务。
 *
 * <p>这个服务负责在手机屏幕最上层显示单词悬浮窗，即使切换到其他 App 也能继续背单词。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 启动悬浮窗服务
 * Intent intent = new Intent(context, OverlayService.class);
 * context.startForegroundService(intent);
 *
 * // 停止悬浮窗服务
 * context.stopService(intent);
 * </code></pre>
 */
public final class OverlayService extends Service {

    /** 启动悬浮窗服务的广播指令 */
    public static final String ACTION_START = "me.englishhugging.android.START_OVERLAY";
    /** 停止悬浮窗服务的广播指令 */
    public static final String ACTION_STOP = "me.englishhugging.android.STOP_OVERLAY";
    /** 重新加载设置的广播指令 */
    public static final String ACTION_RELOAD = "me.englishhugging.android.RELOAD_SETTINGS";

    /** 前台通知渠道名称 */
    private static final String CHANNEL_ID = "floating_words";
    /** 前台服务通知编号 */
    private static final int NOTIFICATION_ID = 20260517;

    /** 标识悬浮窗前台服务当前是否正在运行 */
    public static boolean isRunning = false;

    /** 主线程消息队列，用于向界面发送更新任务 */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** 单词富文本渲染组件 */
    private final OverlayTextRenderer textRenderer = new OverlayTextRenderer();
    /** 悬浮窗主窗口管理组件 */
    private OverlayWindowManager windowManagerHelper;
    /** 调整把手管理组件 */
    private OverlayResizeHandleManager resizeHandleManager;
    /** 触摸手势与交互处理组件 */
    private OverlayTouchHandler touchHandler;

    /** 单词后台定时播放任务 */
    private WordScheduler scheduler;
    /** 应用配置 */
    private AppSettings settings;

    /** 当前屏幕上显示的单词条目 */
    private WordEntry displayedWord;
    /** 当前是否隐藏短语 */
    private boolean displayedHidePhrases;
    /** 当前是否隐藏中文释义 */
    private boolean displayedHideTranslation;

    /**
     * 息屏与亮屏系统广播监听：
     * 用于在用户息屏时自动暂停播放单词，节省电量；亮屏后自动恢复播放。
     */
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                if (scheduler != null) {
                    scheduler.pause();
                }
            } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                if (scheduler != null) {
                    scheduler.resume();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        this.windowManagerHelper = new OverlayWindowManager(this, windowManager);
        this.resizeHandleManager = new OverlayResizeHandleManager(this, windowManager, this.windowManagerHelper);
        this.touchHandler = new OverlayTouchHandler(
                this,
                this.windowManagerHelper,
                this.resizeHandleManager,
                () -> this.scheduler,
                () -> this.settings
        );

        isRunning = true;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(this.screenReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && ACTION_RELOAD.equals(intent.getAction())) {
            reloadSettings();
            // 服务被系统回收后，系统会自动尝试重启服务
            return START_STICKY;
        }

        // Android 8.0 之后，后台启动服务必须升级为前台并绑定可见通知
        startForeground(NOTIFICATION_ID, createNotification());
        startOverlay();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;

        try {
            unregisterReceiver(this.screenReceiver);
        } catch (RuntimeException ignored) {
            // 忽略未注册的异常
        }

        if (this.scheduler != null) {
            this.scheduler.stop();
            this.scheduler = null;
        }

        this.resizeHandleManager.removeExternalHandle();
        this.windowManagerHelper.removeFromWindow();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 这个服务是被通过 startService() 调用的，而不是绑定的
        return null;
    }

    /**
     * 首次启动悬浮窗，加载视图并启动后台单词播放定时任务。
     */
    private void startOverlay() {
        this.settings = AndroidSettingsStore.load(this);
        AndroidSettingsStore.loadPlaybackProgress(this, this.settings, this.settings.getVocabularyFileName());
        List<WordEntry> words = loadWords(this.settings.getVocabularyFileName());

        this.windowManagerHelper.removeFromWindow();
        this.windowManagerHelper.createOverlayView(
                this.settings,
                this.touchHandler::onOverlayTouch,
                this.touchHandler::onInternalResizeTouch
        );
        this.windowManagerHelper.addToWindow(this.settings);

        this.resizeHandleManager.manageHandles(this.settings, this.touchHandler::onExternalResizeTouch);
        startScheduler(words);
    }

    /**
     * 当用户在设置页面修改了配置时，重新加载配置并应用到悬浮窗。
     */
    private void reloadSettings() {
        AppSettings previous = this.settings;
        this.settings = AndroidSettingsStore.load(this);
        AndroidSettingsStore.loadPlaybackProgress(this, this.settings, this.settings.getVocabularyFileName());

        this.windowManagerHelper.updateAlpha(this.settings.getOpacity());
        this.textRenderer.render(
                this.windowManagerHelper.getOverlayText(),
                this.displayedWord,
                this.settings,
                this.displayedHidePhrases,
                this.displayedHideTranslation
        );

        boolean modeChanged = previous == null || previous.getOverlayMode() != this.settings.getOverlayMode();
        boolean sizeChanged = previous == null || previous.getWidth() != this.settings.getWidth() || previous.getHeight() != this.settings.getHeight();
        boolean resizeModeChanged = previous == null || previous.isResizeMode() != this.settings.isResizeMode();

        if (modeChanged || sizeChanged || resizeModeChanged) {
            this.windowManagerHelper.reloadLayout(this.settings);
            this.resizeHandleManager.manageHandles(this.settings, this.touchHandler::onExternalResizeTouch);
        }

        if (hasSchedulerSettingsChanged(previous, this.settings)) {
            List<WordEntry> words = loadWords(this.settings.getVocabularyFileName());
            startScheduler(words);
        }
    }

    /**
     * 判断影响播放控制的核心配置是否发生了变更。
     */
    private boolean hasSchedulerSettingsChanged(AppSettings previous, AppSettings current) {
        if (previous == null || current == null) {
            return true;
        }
        boolean vocabularyChanged = !previous.getVocabularyFileName().equals(current.getVocabularyFileName());
        boolean playbackModeChanged = previous.getPlaybackMode() != current.getPlaybackMode();
        return vocabularyChanged
                || playbackModeChanged
                || !Objects.equals(previous.getStartingPrefix(), current.getStartingPrefix())
                || previous.isLoopPlayback() != current.isLoopPlayback()
                || previous.getIntervalSeconds() != current.getIntervalSeconds()
                || previous.isFillBlankMode() != current.isFillBlankMode()
                || previous.getFillBlankIntervalSeconds() != current.getFillBlankIntervalSeconds()
                || previous.isFillBlankHidePhrases() != current.isFillBlankHidePhrases()
                || previous.isFillBlankShowTranslation() != current.isFillBlankShowTranslation();
    }

    /**
     * 停止旧任务并根据最新词库启动播放。
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
                        mainHandler.post(() -> showWord(wordEntry, false, false));
                    }

                    @Override
                    public void onFillBlankWord(String displayWord, WordEntry originalEntry, boolean hidePhrases, boolean hideTranslation) {
                        mainHandler.post(() -> {
                            WordEntry tempEntry = new WordEntry(displayWord, originalEntry.translations(), originalEntry.phrases());
                            showWord(tempEntry, hidePhrases, hideTranslation);
                        });
                    }

                    @Override
                    public void onPlaybackFinished() {
                        mainHandler.post(() -> {
                            WordEntry finished = new WordEntry(
                                    "播放结束",
                                    Collections.emptyList(),
                                    Collections.emptyList()
                            );
                            showWord(finished, false, false);
                        });
                    }
                },
                progress -> {
                    settings.setPlaybackProgress(progress);
                    AndroidSettingsStore.savePlaybackProgress(this, settings, settings.getVocabularyFileName());
                }
        );

        this.scheduler.start();
    }

    /**
     * 保存并在悬浮窗上渲染接收到的单词内容。
     */
    private void showWord(
            WordEntry wordToDisplay,
            boolean hidePhrases,
            boolean hideTranslation
    ) {
        this.displayedWord = wordToDisplay;
        this.displayedHidePhrases = hidePhrases;
        this.displayedHideTranslation = hideTranslation;
        this.textRenderer.render(
                this.windowManagerHelper.getOverlayText(),
                this.displayedWord,
                this.settings,
                this.displayedHidePhrases,
                this.displayedHideTranslation
        );
    }

    /**
     * 加载内置词库或自定义生词列表。
     */
    private List<WordEntry> loadWords(String vocabularyFileName) {
        if (AndroidSettingsStore.isCustomVocabulary(vocabularyFileName)) {
            List<WordEntry> custom = AndroidSettingsStore.loadCustomWords(this);
            if (custom.isEmpty()) {
                return Collections.singletonList(new WordEntry("自定义词汇为空", Collections.emptyList(), Collections.emptyList()));
            } else {
                return custom;
            }
        }

        try {
            return new VocabularyJsonLoader().load(getAssets().open(vocabularyFileName));
        } catch (Exception ignored) {
            return Collections.singletonList(new WordEntry("词库加载失败", Collections.emptyList(), Collections.emptyList()));
        }
    }

    /**
     * 创建前台服务通知。
     */
    private Notification createNotification() {
        createNotificationChannel();

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_word)
                .setContentTitle("English Hugging Me 正在运行")
                .setContentText("悬浮词汇正在显示")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    /**
     * 注册 Android 8.0+ 必需的通知渠道。
     */
    private void createNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Floating Words", NotificationManager.IMPORTANCE_LOW));
        }
    }
}

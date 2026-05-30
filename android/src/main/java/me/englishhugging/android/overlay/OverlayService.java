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
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.R;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.core.display.WordDisplayFormatter;
import me.englishhugging.core.model.WordDisplaySegment;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.OverlayMode;
import me.englishhugging.core.vocabulary.VocabularyJsonLoader;
import me.englishhugging.core.WordScheduler;
import me.englishhugging.core.WordSchedulerConfig;

/**
 * Android 平台核心悬浮窗前台服务。
 *
 * <p>这个服务（Service）是整个 Android 端的灵魂。由于系统机制限制，它必须被注册为前台服务（Foreground Service）
 * 并长驻通知栏，以免被系统内存回收机制（OOM Killer）干掉。
 *
 * <p>它负责：
 * 1. 在 Android {@link WindowManager} 的最顶层（TYPE_APPLICATION_OVERLAY）绘制一个全局悬浮的 View。
 * 2. 挂载 Core 核心的 {@link WordScheduler} 调度器，将单词推送到悬浮窗。
 * 3. 监听屏幕状态广播（息屏暂停，亮屏恢复）。
 * 4. 监听外部的重载命令，当用户在 MainActivity 修改设置后即时应用颜色和大小。
 */
public final class OverlayService extends Service {
    
    // --- 服务控制常量指令 ---
    public static final String ACTION_START = "me.englishhugging.android.START_OVERLAY";
    public static final String ACTION_STOP = "me.englishhugging.android.STOP_OVERLAY";
    public static final String ACTION_RELOAD = "me.englishhugging.android.RELOAD_SETTINGS";

    private static final String CHANNEL_ID = "floating_words";
    private static final int NOTIFICATION_ID = 20260517;

    // 所有的视图更新必须被抛到 Android 的主线程执行
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 跨平台通用的高亮拆词器
    private final WordDisplayFormatter wordDisplayFormatter = new WordDisplayFormatter();

    // --- Android 视图及布局 ---
    private WindowManager windowManager;
    private FrameLayout overlayRoot;
    private TextView overlayText;
    private WindowManager.LayoutParams layoutParams;
    
    // 缩放控制柄视图
    private TextView resizeHandleView;
    private WindowManager.LayoutParams resizeHandleParams;
    
    // 尺寸监听器：当主悬浮窗宽高发生变化时（例如内容撑大了容器），自动修正右下角缩放把手的位置
    private final android.view.ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::syncResizeHandlePosition;

    // --- 业务模型与状态 ---
    private WordScheduler scheduler;
    private AppSettings settings;
    private WordEntry currentWord;
    
    // 拖拽手势中间状态缓存
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private int initialWidth;
    private int initialHeight;
    private float initialResizeTouchX;
    private float initialResizeTouchY;

    /** 提供给 MainActivity 等组件用来查询当前服务是否处于存活状态 */
    public static boolean isRunning = false;

    /**
     * 息屏/亮屏广播接收器：
     * 用于在用户离开手机时自动挂起背单词流水线，节省电池。
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
        this.windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
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
            // START_STICKY 保证如果服务被系统杀死，重启时可以继续运行
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
        
        if (this.resizeHandleView != null) { 
            this.windowManager.removeView(this.resizeHandleView); 
            this.resizeHandleView = null; 
        }
        
        if (this.overlayRoot != null) { 
            this.windowManager.removeView(this.overlayRoot); 
            this.overlayRoot = null; 
        }
        
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { 
        // 这个服务是被通过 startService() 调用的，而不是绑定的
        return null; 
    }

    /**
     * 首次冷启动悬浮窗，负责加载视图和启动单词泵。
     */
    private void startOverlay() {
        this.settings = AndroidSettingsStore.load(this);
        AndroidSettingsStore.loadPlaybackProgress(this, this.settings, this.settings.getVocabularyFileName());
        List<WordEntry> words = loadWords(this.settings.getVocabularyFileName());
        
        if (this.overlayRoot != null) {
            this.windowManager.removeView(this.overlayRoot);
        }
        
        this.overlayRoot = createOverlayView();
        this.layoutParams = createLayoutParams(this.settings.getOverlayMode());
        this.windowManager.addView(this.overlayRoot, this.layoutParams);
        
        manageResizeHandleWindow();
        startScheduler(words);
    }

    /**
     * 当主页面（MainActivity）的用户修改了偏好设置时，热重载内存。
     */
    private void reloadSettings() {
        AppSettings previous = this.settings;
        this.settings = AndroidSettingsStore.load(this);
        AndroidSettingsStore.loadPlaybackProgress(this, this.settings, this.settings.getVocabularyFileName());

        if (this.overlayRoot != null) {
            this.overlayRoot.setAlpha((float) this.settings.getOpacity());
            if (this.currentWord != null) {
                this.overlayText.setText(formatWord(this.currentWord, false, false));
            }
        }

        boolean modeChanged = previous == null || previous.getOverlayMode() != this.settings.getOverlayMode();
        boolean sizeChanged = previous == null || previous.getWidth() != this.settings.getWidth() || previous.getHeight() != this.settings.getHeight();
        boolean resizeModeChanged = previous == null || previous.isResizeMode() != this.settings.isResizeMode();

        if (modeChanged || sizeChanged || resizeModeChanged) {
            // 如果缩放模式发生变更，牵扯到独立窗口和手势，我们干脆把悬浮窗全部推倒重建最稳妥
            if (previous != null && previous.isResizeMode() != this.settings.isResizeMode() && this.overlayRoot != null) {
                this.windowManager.removeView(this.overlayRoot);
                this.overlayRoot = createOverlayView();
                this.layoutParams = createLayoutParams(this.settings.getOverlayMode());
                this.windowManager.addView(this.overlayRoot, this.layoutParams);
                manageResizeHandleWindow();
                
                if (this.currentWord != null) {
                    this.overlayText.setText(formatWord(this.currentWord, false, false));
                }
            } else {
                // 否则只是简单刷新外壳属性
                this.layoutParams = createLayoutParams(this.settings.getOverlayMode());
                if (this.overlayRoot != null) {
                    this.windowManager.updateViewLayout(this.overlayRoot, this.layoutParams);
                }
                manageResizeHandleWindow();
            }
        }

        // 热更新正在运行的后台调度引擎参数
        if (this.scheduler != null) {
            this.scheduler.updateIntervalSeconds(this.settings.getIntervalSeconds());
            this.scheduler.updateFillBlankSettings(
                    this.settings.isFillBlankMode(),
                    this.settings.getFillBlankIntervalSeconds(),
                    this.settings.isFillBlankHidePhrases(),
                    this.settings.isFillBlankShowTranslation()
            );
        }

        // 如果用户在设置里换了一本词库，或者是从顺序改成了乱序，则必须停止引擎从头再来
        boolean vocabularyChanged = previous == null || !previous.getVocabularyFileName().equals(this.settings.getVocabularyFileName());
        boolean playbackModeChanged = previous == null || previous.getPlaybackMode() != this.settings.getPlaybackMode();
        
        if (vocabularyChanged || playbackModeChanged) {
            List<WordEntry> words = loadWords(this.settings.getVocabularyFileName());
            startScheduler(words);
        }
    }

    /**
     * 利用原生的 View 系统去拼接出一个深色圆角的背单词悬浮窗视图。
     */
    private FrameLayout createOverlayView() {
        FrameLayout root = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(166, 0, 0, 0));
        bg.setCornerRadius(28);
        root.setBackground(bg);
        root.setAlpha((float) this.settings.getOpacity());

        this.overlayText = new TextView(this);
        this.overlayText.setTextColor(Color.WHITE);
        this.overlayText.setGravity(Gravity.CENTER);
        this.overlayText.setPadding(28, 18, 28, 18);
        
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        this.overlayText.setMaxWidth((int) (metrics.widthPixels * 0.9f));
        
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        textParams.gravity = Gravity.CENTER;
        
        root.addView(this.overlayText, textParams);
        this.overlayText.setOnTouchListener(this::onOverlayTouch);

        return root;
    }

    /**
     * 判断当前是否需要展现“右下角调节大小把手”，并进行挂载/卸载管理。
     */
    private void manageResizeHandleWindow() {
        if (this.settings.isResizeMode()) {
            if (this.resizeHandleView == null) {
                this.resizeHandleView = new TextView(this);
                
                try {
                    this.resizeHandleView.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/MaterialIcons-Regular.ttf"));
                    this.resizeHandleView.setText("zoom_out_map");
                } catch (Exception e) {
                    this.resizeHandleView.setText("↘");
                }
                
                this.resizeHandleView.setTextColor(android.graphics.Color.WHITE);
                this.resizeHandleView.setTextSize(24);
                this.resizeHandleView.setPadding(10, 10, 30, 30);
                this.resizeHandleView.setOnTouchListener(this::onResizeTouch);

                this.resizeHandleParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, android.graphics.PixelFormat.TRANSLUCENT);
                this.resizeHandleParams.gravity = Gravity.TOP | Gravity.START;
                
                this.windowManager.addView(this.resizeHandleView, this.resizeHandleParams);
            }
            
            if (this.overlayRoot != null) {
                this.overlayRoot.getViewTreeObserver().removeOnGlobalLayoutListener(this.layoutListener);
                this.overlayRoot.getViewTreeObserver().addOnGlobalLayoutListener(this.layoutListener);
            }
            syncResizeHandlePosition();
        } else {
            if (this.resizeHandleView != null) {
                this.windowManager.removeView(this.resizeHandleView);
                this.resizeHandleView = null;
            }
            if (this.overlayRoot != null) {
                this.overlayRoot.getViewTreeObserver().removeOnGlobalLayoutListener(this.layoutListener);
            }
        }
    }

    /**
     * 吸附算法：使得右下角的缩放把手始终牢牢贴紧悬浮黑板的右下角。
     */
    private void syncResizeHandlePosition() {
        if (this.resizeHandleView != null && this.overlayRoot != null && this.layoutParams != null) {
            int width = this.overlayRoot.getWidth();
            int height = this.overlayRoot.getHeight();
            
            if (width > 0 && height > 0) {
                this.resizeHandleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int handleW = this.resizeHandleView.getMeasuredWidth();
                int handleH = this.resizeHandleView.getMeasuredHeight();
                
                this.resizeHandleParams.x = this.layoutParams.x + width - handleW;
                this.resizeHandleParams.y = this.layoutParams.y + height - handleH;
                this.windowManager.updateViewLayout(this.resizeHandleView, this.resizeHandleParams);
            }
        }
    }

    /**
     * 基于安卓系统层级生成 LayoutParams。
     * 如果用户勾选了“鼠标穿透”，我们会打上 FLAG_NOT_TOUCHABLE 使得黑板无法拦截任何触摸事件。
     */
    private WindowManager.LayoutParams createLayoutParams(OverlayMode overlayMode) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (overlayMode == OverlayMode.CLICK_THROUGH) {
            flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = this.settings.getWidth() > 0 ? (int) (this.settings.getWidth() * metrics.density + 0.5f) : WindowManager.LayoutParams.WRAP_CONTENT;
        int height = this.settings.getHeight() > 0 ? (int) (this.settings.getHeight() * metrics.density + 0.5f) : WindowManager.LayoutParams.WRAP_CONTENT;
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = (int) this.settings.getX();
        params.y = (int) this.settings.getY();
        return params;
    }

    /**
     * 响应用户的手指拖拽事件，更新悬浮窗在屏幕上的坐标。
     */
    private boolean onOverlayTouch(View view, MotionEvent event) {
        if (this.settings.getOverlayMode() != OverlayMode.DRAGGABLE) {
            return true;
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.initialX = this.layoutParams.x; 
                this.initialY = this.layoutParams.y;
                this.initialTouchX = event.getRawX(); 
                this.initialTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                this.layoutParams.x = this.initialX + (int) (event.getRawX() - this.initialTouchX);
                this.layoutParams.y = this.initialY + (int) (event.getRawY() - this.initialTouchY);
                
                this.settings.setX(this.layoutParams.x); 
                this.settings.setY(this.layoutParams.y);
                AndroidSettingsStore.save(this, this.settings);
                
                this.windowManager.updateViewLayout(this.overlayRoot, this.layoutParams);
                syncResizeHandlePosition();
                return true;
            default: 
                return true;
        }
    }

    /**
     * 响应用户在缩放把手上的手指拖动，改变悬浮窗的宽和高（拉伸变形）。
     */
    private boolean onResizeTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.initialWidth = this.layoutParams.width;
                this.initialHeight = this.layoutParams.height;
                
                if (this.initialWidth <= 0) {
                    this.initialWidth = this.overlayRoot.getWidth();
                }
                if (this.initialHeight <= 0) {
                    this.initialHeight = this.overlayRoot.getHeight();
                }
                
                this.initialResizeTouchX = event.getRawX();
                this.initialResizeTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                int newWidth = this.initialWidth + (int) (event.getRawX() - this.initialResizeTouchX);
                int newHeight = this.initialHeight + (int) (event.getRawY() - this.initialResizeTouchY);
                
                this.layoutParams.width = Math.max(260, newWidth);
                this.layoutParams.height = Math.max(80, newHeight);
                
                float density = getResources().getDisplayMetrics().density;
                this.settings.setWidth(this.layoutParams.width / density);
                this.settings.setHeight(this.layoutParams.height / density);
                AndroidSettingsStore.save(this, this.settings);
                
                this.windowManager.updateViewLayout(this.overlayRoot, this.layoutParams);
                syncResizeHandlePosition();
                return true;
            default: 
                return true;
        }
    }

    /**
     * 停止旧任务，实例化全新的调度器并启动单词瀑布流。
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
                        mainHandler.post(() -> { 
                            currentWord = wordEntry; 
                            overlayText.setText(formatWord(currentWord, false, false)); 
                        }); 
                    }
                    
                    @Override 
                    public void onFillBlankWord(String displayWord, WordEntry originalEntry, boolean hidePhrases, boolean hideTranslation) {
                        mainHandler.post(() -> {
                            WordEntry tempEntry = new WordEntry(displayWord, originalEntry.getTranslations(), originalEntry.getPhrases());
                            overlayText.setText(formatWord(tempEntry, hidePhrases, hideTranslation));
                        });
                    }
                    
                    @Override 
                    public void onPlaybackFinished() {
                        mainHandler.post(() -> {
                            currentWord = new WordEntry("播放结束", Collections.emptyList(), Collections.emptyList());
                            overlayText.setText(formatWord(currentWord, false, false));
                        });
                    }
                },
                (nextWordIndex, shuffleOrder, shufflePosition, randomPlayedCount) -> {
                    settings.setNextWordIndex(nextWordIndex); 
                    settings.setShuffleOrder(shuffleOrder);
                    settings.setShufflePosition(shufflePosition); 
                    settings.setRandomPlayedCount(randomPlayedCount);
                    
                    AndroidSettingsStore.save(this, settings);
                    AndroidSettingsStore.savePlaybackProgress(this, settings, settings.getVocabularyFileName());
                }
        );
        
        this.scheduler.start();
    }

    /**
     * 将业务侧抽象的颜色分片，转换为 Android 平台专属的 SpannableStringBuilder 富文本格式。
     */
    private CharSequence formatWord(WordEntry wordEntry, boolean hidePhrases, boolean hideTranslation) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        
        for (WordDisplaySegment segment : this.wordDisplayFormatter.format(wordEntry, this.settings.getDisplayMode(), hidePhrases, hideTranslation)) {
            int start = builder.length();
            builder.append(segment.getText());
            int end = builder.length();
            
            if (segment.getType() == WordDisplaySegment.Type.LINE_BREAK || start == end) {
                continue;
            }
            
            // 染色
            builder.setSpan(new ForegroundColorSpan(colorForSegment(segment.getType())), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 加粗
            if (isBoldSegment(segment.getType())) {
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            // 字号（区分主标题和副标题大小）
            int fontSizeSp = segment.getType() == WordDisplaySegment.Type.WORD ? this.settings.getWordFontSize() : this.settings.getDetailFontSize();
            builder.setSpan(new AbsoluteSizeSpan(fontSizeSp, true), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        return builder;
    }

    /**
     * 将 CSS 的十六进制色值转换为 Android 原生的 Color Int。
     */
    private int colorForSegment(WordDisplaySegment.Type type) {
        if (type == WordDisplaySegment.Type.WORD) {
            return parseColor(this.settings.getWordColor(), Color.WHITE);
        }
        if (type == WordDisplaySegment.Type.TYPE) {
            return parseColor(this.settings.getTypeColor(), Color.CYAN);
        }
        if (type == WordDisplaySegment.Type.PHRASE) {
            return parseColor(this.settings.getPhraseColor(), Color.GREEN);
        }
        return parseColor(this.settings.getTranslationColor(), Color.WHITE);
    }

    private boolean isBoldSegment(WordDisplaySegment.Type type) {
        return type == WordDisplaySegment.Type.WORD || type == WordDisplaySegment.Type.TYPE || type == WordDisplaySegment.Type.PHRASE;
    }

    private int parseColor(String value, int fallback) {
        try { 
            return Color.parseColor(value); 
        } catch (RuntimeException ignored) { 
            return fallback; 
        }
    }

    /**
     * 智能路由：如果是预置词库就走 assets 加载，如果是自定义词库则走文件系统。
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
     * 创建前台保活的系统通知。
     */
    private Notification createNotification() {
        createNotificationChannel();
        
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
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
     * 注册 Android 8.0 必需的通知渠道。
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Floating Words", NotificationManager.IMPORTANCE_LOW));
            }
        }
    }
}

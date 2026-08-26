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
import java.util.Objects;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.R;
import me.englishhugging.android.databinding.OverlayResizeHandleBinding;
import me.englishhugging.android.databinding.OverlayWindowBinding;
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

    /** 主线程消息队列，用于向界面发送更新任务 */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /** 单词文本拆分工具 */
    private final WordDisplayFormatter wordDisplayFormatter = new WordDisplayFormatter();

    /** 窗口管理服务，用于添加和更新悬浮窗 */
    private WindowManager windowManager;
    /** 悬浮窗根布局 */
    private FrameLayout overlayRoot;
    /** 悬浮窗中展示单词富文本的文本控件 */
    private TextView overlayText;
    /** 悬浮窗的窗口布局参数 */
    private WindowManager.LayoutParams layoutParams;
    
    /** 悬浮窗右下角缩放把手视图 */
    private TextView resizeHandleView;
    /** 缩放把手的窗口布局参数 */
    private WindowManager.LayoutParams resizeHandleParams;
    /** 标识当前是否正在拖拽手柄缩放悬浮窗，用于锁定外部异步布局回调 */
    private boolean isResizing = false;
    /** 监听主悬浮窗尺寸变化以同步更新缩放把手位置的布局监听回调 */
    private final android.view.ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::syncResizeHandlePosition;

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
    
    /** 拖拽移动时窗口初始 X 坐标（像素） */
    private int initialX;
    /** 拖拽移动时窗口初始 Y 坐标（像素） */
    private int initialY;
    /** 拖拽移动时手指按下起始 X 坐标（像素） */
    private float initialTouchX;
    /** 拖拽移动时手指按下起始 Y 坐标（像素） */
    private float initialTouchY;
    /** 拖拽缩放时窗口初始宽度（像素） */
    private int initialWidth;
    /** 拖拽缩放时窗口初始高度（像素） */
    private int initialHeight;
    /** 拖拽缩放时手指按下起始 X 坐标（像素） */
    private float initialResizeTouchX;
    /** 拖拽缩放时手指按下起始 Y 坐标（像素） */
    private float initialResizeTouchY;

    /** 标识悬浮窗前台服务当前是否正在运行 */
    public static boolean isRunning = false;

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
        
        if (this.resizeHandleView != null) { 
            this.windowManager.removeView(this.resizeHandleView); 
            this.resizeHandleView = null; 
        }
        
        if (this.overlayRoot != null) { 
            this.overlayRoot.getViewTreeObserver().removeOnGlobalLayoutListener(this.layoutListener);
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
     * 首次启动悬浮窗，加载视图并启动后台单词播放定时任务。
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
     * 当用户在设置页面修改了配置时，重新加载配置并应用到悬浮窗。
     */
    private void reloadSettings() {
        AppSettings previous = this.settings;
        this.settings = AndroidSettingsStore.load(this);
        AndroidSettingsStore.loadPlaybackProgress(this, this.settings, this.settings.getVocabularyFileName());

        if (this.overlayRoot != null) {
            this.overlayRoot.setAlpha((float) this.settings.getOpacity());
            renderDisplayedWord();
        }

        boolean modeChanged = previous == null || previous.getOverlayMode() != this.settings.getOverlayMode();
        boolean sizeChanged = previous == null || previous.getWidth() != this.settings.getWidth() || previous.getHeight() != this.settings.getHeight();
        boolean resizeModeChanged = previous == null || previous.isResizeMode() != this.settings.isResizeMode();

        if (modeChanged || sizeChanged || resizeModeChanged) {
            this.layoutParams = createLayoutParams(this.settings.getOverlayMode());
            if (this.overlayRoot != null) {
                this.windowManager.updateViewLayout(this.overlayRoot, this.layoutParams);
            }
            manageResizeHandleWindow();
        }

        // 播放参数发生变化后立即用已保存进度重建，避免等到下一轮才生效。
        boolean vocabularyChanged = previous == null || !previous.getVocabularyFileName().equals(this.settings.getVocabularyFileName());
        boolean playbackModeChanged = previous == null || previous.getPlaybackMode() != this.settings.getPlaybackMode();
        boolean schedulerSettingsChanged = previous == null
                || vocabularyChanged
                || playbackModeChanged
                || !Objects.equals(previous.getStartingPrefix(), this.settings.getStartingPrefix())
                || previous.isLoopPlayback() != this.settings.isLoopPlayback()
                || previous.getIntervalSeconds() != this.settings.getIntervalSeconds()
                || previous.isFillBlankMode() != this.settings.isFillBlankMode()
                || previous.getFillBlankIntervalSeconds() != this.settings.getFillBlankIntervalSeconds()
                || previous.isFillBlankHidePhrases() != this.settings.isFillBlankHidePhrases()
                || previous.isFillBlankShowTranslation() != this.settings.isFillBlankShowTranslation();

        if (schedulerSettingsChanged) {
            List<WordEntry> words = loadWords(this.settings.getVocabularyFileName());
            startScheduler(words);
        }
    }

    /**
     * 加载悬浮窗布局视图。
     */
    private FrameLayout createOverlayView() {
        OverlayWindowBinding binding = OverlayWindowBinding.inflate(android.view.LayoutInflater.from(this));
        FrameLayout root = binding.getRoot();
        root.setAlpha((float) this.settings.getOpacity());

        this.overlayText = binding.overlayText;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        this.overlayText.setMaxWidth((int) (metrics.widthPixels * 0.9f));

        root.setOnTouchListener(this::onOverlayTouch);

        return root;
    }

    /**
     * 根据设置动态显示或隐藏右下角的悬浮窗缩放把手独立窗口。
     */
    private void manageResizeHandleWindow() {
        if (this.settings.isResizeMode()) {
            if (this.resizeHandleView == null) {
                OverlayResizeHandleBinding resizeBinding = OverlayResizeHandleBinding.inflate(android.view.LayoutInflater.from(this));
                this.resizeHandleView = resizeBinding.getRoot();
                try {
                    this.resizeHandleView.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/MaterialIcons-Regular.ttf"));
                } catch (Exception e) {
                    this.resizeHandleView.setText("↘");
                }
                // 初始先设为 INVISIBLE，等待测量出主悬浮窗宽高后再显示，防止 (0,0) 闪烁
                this.resizeHandleView.setVisibility(View.INVISIBLE);
                this.resizeHandleView.setOnTouchListener(this::onResizeTouch);

                this.resizeHandleParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
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
     * 同步缩放把手位置，使其始终跟随悬浮窗右下角。
     */
    private void syncResizeHandlePosition() {
        if (this.resizeHandleView != null && this.overlayRoot != null && this.layoutParams != null && this.resizeHandleParams != null) {
            if (this.isResizing) {
                // 拖拽期间手柄位置由 onResizeTouch 即时推导更新，避免异步布局回调干扰
                return;
            }
            int width = this.overlayRoot.getWidth();
            int height = this.overlayRoot.getHeight();
            
            if (width > 0 && height > 0) {
                this.resizeHandleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int handleW = this.resizeHandleView.getMeasuredWidth();
                int handleH = this.resizeHandleView.getMeasuredHeight();
                
                this.resizeHandleParams.x = this.layoutParams.x + width - handleW;
                this.resizeHandleParams.y = this.layoutParams.y + height - handleH;
                
                if (this.resizeHandleView.getVisibility() != View.VISIBLE) {
                    this.resizeHandleView.setVisibility(View.VISIBLE);
                }
                this.windowManager.updateViewLayout(this.resizeHandleView, this.resizeHandleParams);
            }
        }
    }

    /**
     * 创建悬浮窗的窗口参数。
     * 如果开启点击穿透，添加 FLAG_NOT_TOUCHABLE 标志使触摸事件直接穿透到下层界面。
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
     * 响应用户在缩放把手上的手指拖动，调整悬浮窗的宽和高。
     */
    private boolean onResizeTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.isResizing = true;
                this.initialWidth = this.layoutParams.width > 0 ? this.layoutParams.width : this.overlayRoot.getWidth();
                this.initialHeight = this.layoutParams.height > 0 ? this.layoutParams.height : this.overlayRoot.getHeight();
                
                this.initialResizeTouchX = event.getRawX();
                this.initialResizeTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                int newWidth = Math.max(260, this.initialWidth + (int) (event.getRawX() - this.initialResizeTouchX));
                int newHeight = Math.max(80, this.initialHeight + (int) (event.getRawY() - this.initialResizeTouchY));
                
                this.layoutParams.width = newWidth;
                this.layoutParams.height = newHeight;
                
                float density = getResources().getDisplayMetrics().density;
                this.settings.setWidth(this.layoutParams.width / density);
                this.settings.setHeight(this.layoutParams.height / density);
                AndroidSettingsStore.save(this, this.settings);
                
                this.windowManager.updateViewLayout(this.overlayRoot, this.layoutParams);
                
                if (this.resizeHandleParams != null && this.resizeHandleView != null) {
                    int handleW = this.resizeHandleView.getMeasuredWidth();
                    int handleH = this.resizeHandleView.getMeasuredHeight();
                    this.resizeHandleParams.x = this.layoutParams.x + newWidth - handleW;
                    this.resizeHandleParams.y = this.layoutParams.y + newHeight - handleH;
                    this.windowManager.updateViewLayout(this.resizeHandleView, this.resizeHandleParams);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                this.isResizing = false;
                return true;
            default: 
                return true;
        }
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

                    AndroidSettingsStore.save(this, settings);
                    AndroidSettingsStore.savePlaybackProgress(this, settings, settings.getVocabularyFileName());
                }
        );
        
        this.scheduler.start();
    }

    /** 保存并在悬浮窗上渲染刚刚接收到的单词内容。 */
    private void showWord(
            WordEntry wordToDisplay,
            boolean hidePhrases,
            boolean hideTranslation
    ) {
        this.displayedWord = wordToDisplay;
        this.displayedHidePhrases = hidePhrases;
        this.displayedHideTranslation = hideTranslation;
        renderDisplayedWord();
    }

    /** 使用最新外观设置重新渲染当前画面，并保留填空状态。 */
    private void renderDisplayedWord() {
        if (this.overlayText == null || this.displayedWord == null) {
            return;
        }
        this.overlayText.setText(formatWord(
                this.displayedWord,
                this.displayedHidePhrases,
                this.displayedHideTranslation
        ));
    }

    /**
     * 将单词分段信息转换为带颜色与字号的 SpannableStringBuilder 富文本。
     */
    private CharSequence formatWord(WordEntry wordEntry, boolean hidePhrases, boolean hideTranslation) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        
        for (WordDisplaySegment segment : this.wordDisplayFormatter.format(wordEntry, this.settings.getDisplayMode(), hidePhrases, hideTranslation)) {
            int start = builder.length();
            builder.append(segment.text());
            int end = builder.length();
            
            if (segment.type() == WordDisplaySegment.Type.LINE_BREAK || start == end) {
                continue;
            }
            
            // 设置字体颜色
            builder.setSpan(new ForegroundColorSpan(colorForSegment(segment.type())), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 设置加粗
            if (isBoldSegment(segment.type())) {
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            // 设置字号（区分单词和释义字号）
            int fontSizeSp = segment.type() == WordDisplaySegment.Type.WORD ? this.settings.getWordFontSize() : this.settings.getDetailFontSize();
            builder.setSpan(new AbsoluteSizeSpan(fontSizeSp, true), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        return builder;
    }

    /**
     * 将十六进制颜色字符串转换为 Android 颜色值。
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

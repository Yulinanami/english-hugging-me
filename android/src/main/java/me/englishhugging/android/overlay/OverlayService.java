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

public final class OverlayService extends Service {
    public static final String ACTION_START = "me.englishhugging.android.START_OVERLAY";
    public static final String ACTION_STOP = "me.englishhugging.android.STOP_OVERLAY";
    public static final String ACTION_RELOAD = "me.englishhugging.android.RELOAD_SETTINGS";

    private static final String CHANNEL_ID = "floating_words";
    private static final int NOTIFICATION_ID = 20260517;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final WordDisplayFormatter wordDisplayFormatter = new WordDisplayFormatter();

    private WindowManager windowManager;
    private FrameLayout overlayRoot;
    private TextView overlayText;
    private WindowManager.LayoutParams layoutParams;
    private TextView resizeHandleView;
    private WindowManager.LayoutParams resizeHandleParams;
    private final android.view.ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::syncResizeHandlePosition;

    private WordScheduler scheduler;
    private AppSettings settings;
    private WordEntry currentWord;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    public static boolean isRunning = false;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                if (scheduler != null) scheduler.pause();
            } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                if (scheduler != null) scheduler.resume();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        isRunning = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_RELOAD.equals(intent.getAction())) {
            reloadSettings();
            return START_STICKY;
        }
        startForeground(NOTIFICATION_ID, createNotification());
        startOverlay();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        try { unregisterReceiver(screenReceiver); } catch (RuntimeException ignored) {}
        if (scheduler != null) { scheduler.stop(); scheduler = null; }
        if (resizeHandleView != null) { windowManager.removeView(resizeHandleView); resizeHandleView = null; }
        if (overlayRoot != null) { windowManager.removeView(overlayRoot); overlayRoot = null; }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void startOverlay() {
        settings = AndroidSettingsStore.load(this);
        AndroidSettingsStore.loadPlaybackProgress(this, settings, settings.getVocabularyFileName());
        List<WordEntry> words = loadWords(settings.getVocabularyFileName());
        if (overlayRoot != null) windowManager.removeView(overlayRoot);
        overlayRoot = createOverlayView();
        layoutParams = createLayoutParams(settings.getOverlayMode());
        windowManager.addView(overlayRoot, layoutParams);
        manageResizeHandleWindow();
        startScheduler(words);
    }

    private void reloadSettings() {
        AppSettings previous = settings;
        settings = AndroidSettingsStore.load(this);
        AndroidSettingsStore.loadPlaybackProgress(this, settings, settings.getVocabularyFileName());

        if (overlayRoot != null) {
            overlayRoot.setAlpha((float) settings.getOpacity());
            if (currentWord != null) overlayText.setText(formatWord(currentWord));
        }

        if (previous == null || previous.getOverlayMode() != settings.getOverlayMode() ||
            previous.getWidth() != settings.getWidth() || previous.getHeight() != settings.getHeight() ||
            previous.isResizeMode() != settings.isResizeMode()) {
            
            // Recreate overlay view entirely to apply or remove resize handle if changed
            if (previous != null && previous.isResizeMode() != settings.isResizeMode() && overlayRoot != null) {
                windowManager.removeView(overlayRoot);
                overlayRoot = createOverlayView();
                layoutParams = createLayoutParams(settings.getOverlayMode());
                windowManager.addView(overlayRoot, layoutParams);
                manageResizeHandleWindow();
                if (currentWord != null) overlayText.setText(formatWord(currentWord));
            } else {
                layoutParams = createLayoutParams(settings.getOverlayMode());
                if (overlayRoot != null) windowManager.updateViewLayout(overlayRoot, layoutParams);
                manageResizeHandleWindow();
            }
        }

        if (scheduler != null) scheduler.updateIntervalSeconds(settings.getIntervalSeconds());

        if (previous == null || !previous.getVocabularyFileName().equals(settings.getVocabularyFileName()) || previous.getPlaybackMode() != settings.getPlaybackMode()) {
            List<WordEntry> words = loadWords(settings.getVocabularyFileName());
            startScheduler(words);
        }
    }

    private FrameLayout createOverlayView() {
        FrameLayout root = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(166, 0, 0, 0));
        bg.setCornerRadius(28);
        root.setBackground(bg);
        root.setAlpha((float) settings.getOpacity());

        overlayText = new TextView(this);
        overlayText.setTextColor(Color.WHITE);
        overlayText.setGravity(Gravity.CENTER);
        overlayText.setPadding(28, 18, 28, 18);
        
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        overlayText.setMaxWidth((int) (metrics.widthPixels * 0.9f));
        
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        textParams.gravity = Gravity.CENTER;
        root.addView(overlayText, textParams);
        overlayText.setOnTouchListener(this::onOverlayTouch);

        return root;
    }

    private void manageResizeHandleWindow() {
        if (settings.isResizeMode()) {
            if (resizeHandleView == null) {
                resizeHandleView = new TextView(this);
                try {
                    resizeHandleView.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/MaterialIcons-Regular.ttf"));
                    resizeHandleView.setText("zoom_out_map");
                } catch (Exception e) {
                    resizeHandleView.setText("↘");
                }
                resizeHandleView.setTextColor(android.graphics.Color.WHITE);
                resizeHandleView.setTextSize(24);
                resizeHandleView.setPadding(10, 10, 30, 30);
                resizeHandleView.setOnTouchListener(this::onResizeTouch);

                resizeHandleParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, android.graphics.PixelFormat.TRANSLUCENT);
                resizeHandleParams.gravity = Gravity.TOP | Gravity.START;
                windowManager.addView(resizeHandleView, resizeHandleParams);
            }
            if (overlayRoot != null) {
                overlayRoot.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
                overlayRoot.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
            }
            syncResizeHandlePosition();
        } else {
            if (resizeHandleView != null) {
                windowManager.removeView(resizeHandleView);
                resizeHandleView = null;
            }
            if (overlayRoot != null) {
                overlayRoot.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
            }
        }
    }

    private void syncResizeHandlePosition() {
        if (resizeHandleView != null && overlayRoot != null && layoutParams != null) {
            int width = overlayRoot.getWidth();
            int height = overlayRoot.getHeight();
            if (width > 0 && height > 0) {
                resizeHandleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int handleW = resizeHandleView.getMeasuredWidth();
                int handleH = resizeHandleView.getMeasuredHeight();
                resizeHandleParams.x = layoutParams.x + width - handleW;
                resizeHandleParams.y = layoutParams.y + height - handleH;
                windowManager.updateViewLayout(resizeHandleView, resizeHandleParams);
            }
        }
    }

    private WindowManager.LayoutParams createLayoutParams(OverlayMode overlayMode) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (overlayMode == OverlayMode.CLICK_THROUGH) {
            flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        int width = settings.getWidth() > 0 ? (int) (settings.getWidth() * getResources().getDisplayMetrics().density + 0.5f) : WindowManager.LayoutParams.WRAP_CONTENT;
        int height = settings.getHeight() > 0 ? (int) (settings.getHeight() * getResources().getDisplayMetrics().density + 0.5f) : WindowManager.LayoutParams.WRAP_CONTENT;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = (int) settings.getX();
        params.y = (int) settings.getY();
        return params;
    }

    private boolean onOverlayTouch(View view, MotionEvent event) {
        if (settings.getOverlayMode() != OverlayMode.DRAGGABLE) return true;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = layoutParams.x; initialY = layoutParams.y;
                initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                layoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                settings.setX(layoutParams.x); settings.setY(layoutParams.y);
                AndroidSettingsStore.save(this, settings);
                windowManager.updateViewLayout(overlayRoot, layoutParams);
                syncResizeHandlePosition();
                return true;
            default: return true;
        }
    }

    private int initialWidth, initialHeight;
    private float initialResizeTouchX, initialResizeTouchY;

    private boolean onResizeTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialWidth = layoutParams.width;
                initialHeight = layoutParams.height;
                if (initialWidth <= 0) initialWidth = overlayRoot.getWidth();
                if (initialHeight <= 0) initialHeight = overlayRoot.getHeight();
                initialResizeTouchX = event.getRawX();
                initialResizeTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                int newWidth = initialWidth + (int) (event.getRawX() - initialResizeTouchX);
                int newHeight = initialHeight + (int) (event.getRawY() - initialResizeTouchY);
                layoutParams.width = Math.max(260, newWidth);
                layoutParams.height = Math.max(80, newHeight);
                settings.setWidth(layoutParams.width / getResources().getDisplayMetrics().density);
                settings.setHeight(layoutParams.height / getResources().getDisplayMetrics().density);
                AndroidSettingsStore.save(this, settings);
                windowManager.updateViewLayout(overlayRoot, layoutParams);
                syncResizeHandlePosition();
                return true;
            default: return true;
        }
    }

    private void startScheduler(List<WordEntry> words) {
        if (scheduler != null) scheduler.stop();
        scheduler = new WordScheduler(
                words, settings.getIntervalSeconds(), settings.getPlaybackMode(),
                settings.getNextWordIndex(), settings.getShuffleOrder(),
                settings.getShufflePosition(), settings.getRandomPlayedCount(),
                settings.getStartingPrefix(), settings.isLoopPlayback(),
                new WordScheduler.Listener() {
                    @Override public void onWord(WordEntry wordEntry) { mainHandler.post(() -> { currentWord = wordEntry; overlayText.setText(formatWord(currentWord)); }); }
                    @Override public void onPlaybackFinished() {
                        mainHandler.post(() -> {
                            currentWord = new WordEntry("播放结束", Collections.emptyList(), Collections.emptyList());
                            overlayText.setText(formatWord(currentWord));
                        });
                    }
                },
                (nextWordIndex, shuffleOrder, shufflePosition, randomPlayedCount) -> {
                    settings.setNextWordIndex(nextWordIndex); settings.setShuffleOrder(shuffleOrder);
                    settings.setShufflePosition(shufflePosition); settings.setRandomPlayedCount(randomPlayedCount);
                    AndroidSettingsStore.save(this, settings);
                    AndroidSettingsStore.savePlaybackProgress(this, settings, settings.getVocabularyFileName());
                }
        );
        scheduler.start();
    }

    private CharSequence formatWord(WordEntry wordEntry) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (WordDisplaySegment segment : wordDisplayFormatter.format(wordEntry, settings.getDisplayMode())) {
            int start = builder.length();
            builder.append(segment.getText());
            int end = builder.length();
            if (segment.getType() == WordDisplaySegment.Type.LINE_BREAK || start == end) continue;
            builder.setSpan(new ForegroundColorSpan(colorForSegment(segment.getType())), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (isBoldSegment(segment.getType())) builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            int fontSizeSp = segment.getType() == WordDisplaySegment.Type.WORD ? settings.getWordFontSize() : settings.getDetailFontSize();
            builder.setSpan(new AbsoluteSizeSpan(fontSizeSp, true), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    private int colorForSegment(WordDisplaySegment.Type type) {
        if (type == WordDisplaySegment.Type.WORD) return parseColor(settings.getWordColor(), Color.WHITE);
        if (type == WordDisplaySegment.Type.TYPE) return parseColor(settings.getTypeColor(), Color.CYAN);
        if (type == WordDisplaySegment.Type.PHRASE) return parseColor(settings.getPhraseColor(), Color.GREEN);
        return parseColor(settings.getTranslationColor(), Color.WHITE);
    }

    private boolean isBoldSegment(WordDisplaySegment.Type type) {
        return type == WordDisplaySegment.Type.WORD || type == WordDisplaySegment.Type.TYPE || type == WordDisplaySegment.Type.PHRASE;
    }

    private int parseColor(String value, int fallback) {
        try { return Color.parseColor(value); } catch (RuntimeException ignored) { return fallback; }
    }

    private List<WordEntry> loadWords(String vocabularyFileName) {
        if (AndroidSettingsStore.isCustomVocabulary(vocabularyFileName)) {
            List<WordEntry> custom = AndroidSettingsStore.loadCustomWords(this);
            return custom.isEmpty() ? Collections.singletonList(new WordEntry("自定义词汇为空", Collections.emptyList(), Collections.emptyList())) : custom;
        }
        try { return new VocabularyJsonLoader().load(getAssets().open(vocabularyFileName)); }
        catch (Exception ignored) { return Collections.singletonList(new WordEntry("词库加载失败", Collections.emptyList(), Collections.emptyList())); }
    }

    private Notification createNotification() {
        createNotificationChannel();
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_word)
                .setContentTitle("English Hugging Me 正在运行")
                .setContentText("悬浮词汇正在显示")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Floating Words", NotificationManager.IMPORTANCE_LOW));
        }
    }
}

package me.englishhugging.android.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
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
import me.englishhugging.core.settings.DisplayMode;
import me.englishhugging.core.settings.OverlayMode;
import me.englishhugging.core.vocabulary.VocabularyJsonLoader;
import me.englishhugging.core.WordScheduler;

public final class OverlayService extends Service {
    public static final String ACTION_START = "me.englishhugging.android.START_OVERLAY";
    public static final String ACTION_STOP = "me.englishhugging.android.STOP_OVERLAY";

    private static final String CHANNEL_ID = "floating_words";
    private static final int NOTIFICATION_ID = 20260517;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final WordDisplayFormatter wordDisplayFormatter = new WordDisplayFormatter();

    private WindowManager windowManager;
    private TextView overlayView;
    private WindowManager.LayoutParams layoutParams;
    private WordScheduler scheduler;
    private AppSettings settings;
    private WordEntry currentWord;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, createNotification());
        startOverlay();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (scheduler != null) { scheduler.stop(); scheduler = null; }
        if (overlayView != null) { windowManager.removeView(overlayView); overlayView = null; }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void startOverlay() {
        settings = AndroidSettingsStore.load(this);
        AndroidSettingsStore.loadPlaybackProgress(this, settings, settings.getVocabularyFileName());
        List<WordEntry> words = loadWords(settings.getVocabularyFileName());
        if (overlayView != null) windowManager.removeView(overlayView);
        overlayView = createOverlayView();
        layoutParams = createLayoutParams(settings.getOverlayMode());
        windowManager.addView(overlayView, layoutParams);
        startScheduler(words);
    }

    private TextView createOverlayView() {
        TextView tv = new TextView(this);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(22);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(28, 18, 28, 18);
        tv.setAlpha((float) settings.getOpacity());
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(166, 0, 0, 0));
        bg.setCornerRadius(28);
        tv.setBackground(bg);
        tv.setOnTouchListener(this::onOverlayTouch);
        return tv;
    }

    private WindowManager.LayoutParams createLayoutParams(OverlayMode overlayMode) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (overlayMode == OverlayMode.CLICK_THROUGH) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
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
                windowManager.updateViewLayout(overlayView, layoutParams);
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
                wordEntry -> mainHandler.post(() -> { currentWord = wordEntry; overlayView.setText(formatWord(currentWord)); }),
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
            builder.setSpan(new ForegroundColorSpan(colorForSegment(segment.getType())), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (isBoldSegment(segment.getType())) builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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

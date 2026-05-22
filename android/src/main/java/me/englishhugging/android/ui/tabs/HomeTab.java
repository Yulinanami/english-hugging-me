package me.englishhugging.android.ui.tabs;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.overlay.OverlayService;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.core.settings.AppSettings;

public final class HomeTab {
    private final MainActivity activity;
    private final AndroidUi ui;
    private final Runnable onNavigateToSettings;

    private TextView startCircle;
    private TextView connectedStatus;

    public HomeTab(MainActivity activity, AndroidUi ui, Runnable onNavigateToSettings) {
        this.activity = activity;
        this.ui = ui;
        this.onNavigateToSettings = onNavigateToSettings;
    }

    public void buildContent(LinearLayout pageContent) {
        AppSettings settings = AndroidSettingsStore.load(activity);
        LinearLayout header = ui.headerRow("首页", "settings");
        header.getChildAt(1).setOnClickListener(view -> onNavigateToSettings.run());
        pageContent.addView(header, ui.matchWidthWithBottomMargin(34));

        LinearLayout speedCard = ui.card();
        speedCard.setOrientation(LinearLayout.HORIZONTAL);
        speedCard.setGravity(Gravity.CENTER);
        speedCard.addView(ui.homeMetric("词汇本", settings.getVocabularyFileName()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        View divider = new View(activity);
        divider.setBackgroundColor(Color.rgb(199, 197, 209));
        speedCard.addView(divider, new LinearLayout.LayoutParams(ui.dp(1), ui.dp(54)));
        speedCard.addView(ui.homeMetric("间隔", settings.getIntervalSeconds() + " 秒"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        pageContent.addView(speedCard, ui.matchWidthWithBottomMargin(78));

        startCircle = new TextView(activity);
        startCircle.setTextSize(64);
        startCircle.setTypeface(ui.getIconFont());
        startCircle.setTextColor(Color.WHITE);
        startCircle.setGravity(Gravity.CENTER);
        startCircle.setOnClickListener(view -> {
            view.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                if (OverlayService.isRunning) {
                    activity.stopService(new Intent(activity, OverlayService.class));
                    startCircle.postDelayed(this::updateStartCircleState, 200);
                } else {
                    startOverlay();
                    startCircle.postDelayed(this::updateStartCircleState, 500);
                }
            }).start();
        });
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(ui.dp(140), ui.dp(140));
        circleParams.gravity = Gravity.CENTER_HORIZONTAL;
        pageContent.addView(startCircle, circleParams);

        connectedStatus = ui.titleText("点击启动悬浮背词");
        connectedStatus.setTextColor(AndroidUi.PRIMARY);
        connectedStatus.setGravity(Gravity.CENTER);
        pageContent.addView(connectedStatus, ui.matchWidthWithBottomMargin(72));

        updateStartCircleState();
    }

    public void updateStartCircleState() {
        if (startCircle == null || connectedStatus == null) return;
        if (OverlayService.isRunning) {
            startCircle.setText("stop");
            startCircle.setBackground(ui.oval(AndroidUi.PRIMARY));
            connectedStatus.setText("悬浮背词运行中");
            connectedStatus.setTextColor(AndroidUi.PRIMARY);
        } else {
            startCircle.setText("play_arrow");
            startCircle.setBackground(ui.oval(AndroidUi.PRIMARY));
            connectedStatus.setText("点击启动悬浮背词");
            connectedStatus.setTextColor(AndroidUi.PRIMARY);
        }
    }

    private void startOverlay() {
        if (!Settings.canDrawOverlays(activity)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName())
            );
            activity.startActivity(intent);
            Toast.makeText(activity, "请先允许悬浮窗权限", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(activity, OverlayService.class);
        intent.setAction(OverlayService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(intent);
        } else {
            activity.startService(intent);
        }
    }
}

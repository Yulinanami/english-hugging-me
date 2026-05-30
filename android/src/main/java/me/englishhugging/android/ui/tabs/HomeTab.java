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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.overlay.OverlayService;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.core.settings.AppSettings;

/**
 * 首页看板。
 *
 * <p>这个类负责通过代码拼装出 Android 首页的内容区。
 * 它包含当前词库的概览信息、一个巨大的启动/停止引擎按钮，以及挖空复习模式的快捷开关。
 */
public final class HomeTab {
    
    // --- 外部依赖 ---
    private final MainActivity activity;
    private final AndroidUi ui;
    private final Runnable onNavigateToSettings;

    // --- 动态 UI 节点 ---
    private TextView startCircle;
    private TextView connectedStatus;
    private Switch fillBlankSwitch;

    /**
     * 构建首页。
     *
     * @param activity             依附的宿主
     * @param ui                   组件工厂
     * @param onNavigateToSettings 用户点击头部齿轮图标时的跳转回调
     */
    public HomeTab(MainActivity activity, AndroidUi ui, Runnable onNavigateToSettings) {
        this.activity = activity;
        this.ui = ui;
        this.onNavigateToSettings = onNavigateToSettings;
    }

    /**
     * 将首页的专属控件注入到外部提供的容器中。
     *
     * @param pageHeader  头部容器（用于装载标题）
     * @param pageContent 滚动区域容器（用于装载数据图表和按钮）
     */
    public void buildContent(LinearLayout pageHeader, LinearLayout pageContent) {
        AppSettings settings = AndroidSettingsStore.load(this.activity);
        
        // 1. 顶部栏，带有一个跳转到详细设置页面的齿轮
        LinearLayout header = this.ui.headerRow("首页", "settings");
        header.getChildAt(1).setOnClickListener(view -> this.onNavigateToSettings.run());
        pageHeader.addView(header, this.ui.matchWidthWithBottomMargin(12));

        // 2. 核心指标卡片：词库名称和播放间隔
        LinearLayout speedCard = this.ui.card();
        speedCard.setOrientation(LinearLayout.HORIZONTAL);
        speedCard.setGravity(Gravity.CENTER);
        
        speedCard.addView(this.ui.homeMetric("词汇本", settings.getVocabularyFileName()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        
        // 中间的竖向细分割线
        View divider = new View(this.activity);
        divider.setBackgroundColor(Color.rgb(199, 197, 209));
        speedCard.addView(divider, new LinearLayout.LayoutParams(this.ui.dp(1), this.ui.dp(54)));
        
        speedCard.addView(this.ui.homeMetric("间隔", settings.getIntervalSeconds() + " 秒"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        
        pageContent.addView(speedCard, this.ui.matchWidthWithBottomMargin(78));

        // 3. 巨型的引擎启动/停止圆形按钮
        this.startCircle = new TextView(this.activity);
        this.startCircle.setTextSize(64);
        this.startCircle.setTypeface(this.ui.getIconFont());
        this.startCircle.setTextColor(Color.WHITE);
        this.startCircle.setGravity(Gravity.CENTER);
        
        // 点击带有缩放弹性动画
        this.startCircle.setOnClickListener(view -> {
            view.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                
                if (OverlayService.isRunning) {
                    this.activity.stopService(new Intent(this.activity, OverlayService.class));
                    this.startCircle.postDelayed(this::updateStartCircleState, 200);
                } else {
                    startOverlay();
                    this.startCircle.postDelayed(this::updateStartCircleState, 500);
                }
            }).start();
        });
        
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(this.ui.dp(140), this.ui.dp(140));
        circleParams.gravity = Gravity.CENTER_HORIZONTAL;
        pageContent.addView(this.startCircle, circleParams);

        // 4. 按钮下方的状态提示文字
        this.connectedStatus = this.ui.titleText("点击启动悬浮背词");
        this.connectedStatus.setTextColor(AndroidUi.PRIMARY);
        this.connectedStatus.setGravity(Gravity.CENTER);
        pageContent.addView(this.connectedStatus, this.ui.matchWidthWithBottomMargin(32));

        // 5. 挖空模式快捷开关卡片
        LinearLayout fillBlankCard = this.ui.card();
        this.fillBlankSwitch = new Switch(this.activity);
        this.fillBlankSwitch.setChecked(settings.isFillBlankMode());
        
        fillBlankCard.addView(this.ui.settingSwitchItem("挖空模式", "播放后挖空复习拼写", this.fillBlankSwitch), this.ui.matchWidthWrapHeight());
        pageContent.addView(fillBlankCard, this.ui.matchWidthWithBottomMargin(16));

        // 开关变更时，即时存盘并知会运行中的悬浮窗服务
        this.fillBlankSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings current = AndroidSettingsStore.load(this.activity);
            current.setFillBlankMode(isChecked);
            AndroidSettingsStore.save(this.activity, current);
            notifyServiceReload();
        });

        // 初始化按钮状态
        updateStartCircleState();
    }

    /**
     * 轮询更新界面中间巨大圆形按钮的展示状态。
     * 如果服务已死，则显示播放图标（启动）；如果存活，则显示方块图标（停止）。
     */
    public void updateStartCircleState() {
        if (this.startCircle == null || this.connectedStatus == null) {
            return;
        }
        
        if (OverlayService.isRunning) {
            this.startCircle.setText("stop");
            this.startCircle.setBackground(this.ui.oval(AndroidUi.PRIMARY));
            this.connectedStatus.setText("悬浮背词运行中");
            this.connectedStatus.setTextColor(AndroidUi.PRIMARY);
        } else {
            this.startCircle.setText("play_arrow");
            this.startCircle.setBackground(this.ui.oval(AndroidUi.PRIMARY));
            this.connectedStatus.setText("点击启动悬浮背词");
            this.connectedStatus.setTextColor(AndroidUi.PRIMARY);
        }
    }

    /**
     * 尝试拉起悬浮服务。如果 Android 尚未授予悬浮窗权限，则弹窗引导至系统设置页。
     */
    private void startOverlay() {
        if (!Settings.canDrawOverlays(this.activity)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + this.activity.getPackageName())
            );
            this.activity.startActivity(intent);
            Toast.makeText(this.activity, "请先允许悬浮窗权限", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this.activity, OverlayService.class);
        intent.setAction(OverlayService.ACTION_START);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.activity.startForegroundService(intent);
        } else {
            this.activity.startService(intent);
        }
    }

    /**
     * 如果悬浮窗正挂载在屏幕上，向它发送热更新广播，让它重新读取一次配置。
     */
    private void notifyServiceReload() {
        if (OverlayService.isRunning) {
            Intent intent = new Intent(this.activity, OverlayService.class);
            intent.setAction(OverlayService.ACTION_RELOAD);
            this.activity.startService(intent);
        }
    }
}

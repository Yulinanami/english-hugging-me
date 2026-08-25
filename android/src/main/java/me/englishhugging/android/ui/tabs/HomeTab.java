package me.englishhugging.android.ui.tabs;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.R;
import me.englishhugging.android.databinding.PageHomeBinding;
import me.englishhugging.android.overlay.OverlayService;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.core.settings.AppSettings;

/**
 * Android 手机端“首页”界面，展示当前背诵状态并提供悬浮窗开关。
 *
 * <p>这个类负责展示当前背词状态摘要，并提供一键开启/停止悬浮窗的大圆按钮。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * HomeTab homeTab = new HomeTab(activity, ui, () -> showSettingsPage());
 * View view = homeTab.getView();
 * pageContainer.addView(view);
 * </code></pre>
 */
public final class HomeTab {
    /** 所属的主界面对象 */
    private final MainActivity activity;

    /** 界面辅助工具 */
    private final AndroidUi ui;

    /** 跳转到设置页的回调 */
    private final Runnable onNavigateToSettings;

    /** 首页视图绑定对象 */
    private PageHomeBinding binding;

    /** 刷新悬浮窗服务的延迟任务 */
    private final Runnable delayedServiceReload = this::notifyServiceReload;

    /**
     * 创建首页页面对象。
     *
     * @param activity             所属的主界面对象
     * @param ui                   界面辅助工具
     * @param onNavigateToSettings 跳转到设置页的回调
     */
    public HomeTab(MainActivity activity, AndroidUi ui, Runnable onNavigateToSettings) {
        this.activity = activity;
        this.ui = ui;
        this.onNavigateToSettings = onNavigateToSettings;
    }

    /**
     * 加载并返回首页视图。
     *
     * @return 首页根视图
     */
    public View getView() {
        this.binding = PageHomeBinding.inflate(this.activity.getLayoutInflater());
        AppSettings settings = AndroidSettingsStore.load(this.activity);

        // 首页只展示最常用的配置摘要，完整修改入口仍在设置页。
        this.binding.vocabularyValue.setText(settings.getVocabularyFileName());
        this.binding.intervalValue.setText(
                this.activity.getString(R.string.seconds_format, settings.getIntervalSeconds())
        );
        this.binding.fillBlankSwitch.setChecked(settings.isFillBlankMode());

        this.ui.styleIcon(this.binding.settingsIcon);
        this.ui.styleIcon(this.binding.startCircle);
        this.binding.settingsIcon.setOnClickListener(view -> this.onNavigateToSettings.run());
        this.binding.startCircle.setOnClickListener(this::toggleOverlay);
        this.binding.fillBlankSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 开关变化后立即保存；服务正在运行时同步刷新悬浮窗
            AppSettings current = AndroidSettingsStore.load(this.activity);
            current.setFillBlankMode(isChecked);
            AndroidSettingsStore.save(this.activity, current);
            reloadServiceAfterSwitchAnimation();
        });

        updateStartCircleState();
        return this.binding.getRoot();
    }

    /** 根据悬浮窗服务的实际状态更新首页主按钮和提示文字。 */
    public void updateStartCircleState() {
        if (this.binding == null) {
            return;
        }

        if (OverlayService.isRunning) {
            this.binding.startCircle.setText(R.string.icon_stop);
            this.binding.connectedStatus.setText(R.string.overlay_running);
        } else {
            this.binding.startCircle.setText(R.string.icon_play);
            this.binding.connectedStatus.setText(R.string.start_overlay);
        }
    }

    /** 播放点击缩放动画，并在动画结束后切换悬浮窗服务状态。 */
    private void toggleOverlay(View view) {
        view.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    if (OverlayService.isRunning) {
                        this.activity.stopService(new Intent(this.activity, OverlayService.class));
                        // 等待系统完成服务停止，再刷新按钮，避免短暂显示旧状态。
                        this.binding.startCircle.postDelayed(this::updateStartCircleState, 200);
                    } else {
                        startOverlay();
                        // 前台服务启动需要一点时间，因此使用稍长的状态刷新延迟。
                        this.binding.startCircle.postDelayed(this::updateStartCircleState, 500);
                    }
                })
                .start();
    }

    /** 检查悬浮窗权限；权限就绪后以前台服务方式启动播放悬浮窗。 */
    private void startOverlay() {
        if (!Settings.canDrawOverlays(this.activity)) {
            // Android 要求用户在系统设置中单独授予“显示在其它应用上层”权限。
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
        this.activity.startForegroundService(intent);
    }

    /** 开关动画结束后再刷新悬浮窗服务，避免界面卡顿。 */
    private void reloadServiceAfterSwitchAnimation() {
        View hostView = this.activity.getWindow().getDecorView();
        hostView.removeCallbacks(this.delayedServiceReload);
        int animationDuration = this.activity.getResources().getInteger(
                android.R.integer.config_shortAnimTime
        );
        hostView.postDelayed(this.delayedServiceReload, animationDuration);
    }

    /** 设置变化时通知正在运行的悬浮窗重新读取配置。 */
    private void notifyServiceReload() {
        if (OverlayService.isRunning) {
            Intent intent = new Intent(this.activity, OverlayService.class);
            intent.setAction(OverlayService.ACTION_RELOAD);
            this.activity.startService(intent);
        }
    }
}

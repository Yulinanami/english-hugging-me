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
 * 首页的数据展示、悬浮窗开关和填空模式交互。
 *
 * <p>页面由 {@link PageHomeBinding} 对应的 XML 创建；每次进入首页都会重新读取设置，
 * 保证从设置页返回后显示的是最新配置。</p>
 */
public final class HomeTab {
    /** 首页依赖的 Activity，用于加载资源、申请权限和启停服务。 */
    private final MainActivity activity;

    /** XML 无法直接声明的公共 UI 行为。 */
    private final AndroidUi ui;

    /** 点击齿轮图标时由 Activity 提供的页面跳转动作。 */
    private final Runnable onNavigateToSettings;

    /** 当前首页视图的 View Binding，用于同步悬浮窗运行状态。 */
    private PageHomeBinding binding;

    /** 保存首页所需依赖，不在这里持有或创建新的 Activity。 */
    public HomeTab(MainActivity activity, AndroidUi ui, Runnable onNavigateToSettings) {
        this.activity = activity;
        this.ui = ui;
        this.onNavigateToSettings = onNavigateToSettings;
    }

    /** 创建首页视图，加载当前设置并绑定用户交互。 */
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
            // 开关变化立即持久化；服务正在运行时同步刷新悬浮窗内容。
            AppSettings current = AndroidSettingsStore.load(this.activity);
            current.setFillBlankMode(isChecked);
            AndroidSettingsStore.save(this.activity, current);
            notifyServiceReload();
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

    /** 设置变化时通知正在运行的悬浮窗重新读取配置。 */
    private void notifyServiceReload() {
        if (OverlayService.isRunning) {
            Intent intent = new Intent(this.activity, OverlayService.class);
            intent.setAction(OverlayService.ACTION_RELOAD);
            this.activity.startService(intent);
        }
    }
}

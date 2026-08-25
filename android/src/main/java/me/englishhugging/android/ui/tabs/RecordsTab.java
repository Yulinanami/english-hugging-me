package me.englishhugging.android.ui.tabs;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.databinding.ItemRecordBinding;
import me.englishhugging.android.databinding.PageRecordsBinding;
import me.englishhugging.android.overlay.OverlayService;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.core.settings.AppSettings;

/**
 * Android 手机端“学习记录”界面，展示各词库背诵进度并提供清除功能。
 *
 * <p>这个类负责展示所有词库的背词进度统计，并提供清空进度记录的功能。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * RecordsTab recordsTab = new RecordsTab(activity, ui, () -> reload(), () -> goHome());
 * View view = recordsTab.getView();
 * pageContainer.addView(view);
 * </code></pre>
 */
public final class RecordsTab {
    /** 所属的主界面对象 */
    private final MainActivity activity;

    /** 界面辅助工具 */
    private final AndroidUi ui;

    /** 刷新页面的回调 */
    private final Runnable onReloadPage;

    /** 返回首页的回调 */
    private final Runnable goHome;

    /**
     * 创建学习记录页面对象。
     *
     * @param activity     所属的主界面对象
     * @param ui           界面辅助工具
     * @param onReloadPage 重新加载页面的回调
     * @param goHome       返回首页的回调
     */
    public RecordsTab(
            MainActivity activity,
            AndroidUi ui,
            Runnable onReloadPage,
            Runnable goHome
    ) {
        this.activity = activity;
        this.ui = ui;
        this.onReloadPage = onReloadPage;
        this.goHome = goHome;
    }

    /**
     * 加载并返回学习记录页面视图。
     *
     * @return 学习记录页面根视图
     */
    public View getView() {
        PageRecordsBinding binding = PageRecordsBinding.inflate(this.activity.getLayoutInflater());
        this.ui.styleIcon(binding.backIcon);
        binding.backIcon.setOnClickListener(view -> this.goHome.run());

        for (String line : AndroidSettingsStore.playbackRecordLines(this.activity)) {
            // 使用独立条目布局，避免在 Java 中重复设置尺寸、颜色和间距。
            ItemRecordBinding item = ItemRecordBinding.inflate(
                    this.activity.getLayoutInflater(),
                    binding.recordsCard,
                    false
            );
            this.ui.styleIcon(item.playIcon);
            item.recordText.setText(line);
            binding.recordsCard.addView(item.getRoot());
        }

        binding.clearButton.setOnClickListener(view -> showClearConfirmation());
        return binding.getRoot();
    }

    /** 显示二次确认；用户确认后清除所有词库的播放进度。 */
    private void showClearConfirmation() {
        new AlertDialog.Builder(this.activity)
                .setTitle("确认清除")
                .setMessage("确定要清除所有播放记录吗？这将使所有词汇本从头开始播放。")
                .setPositiveButton("确定", (dialog, which) -> {
                    AppSettings currentSettings = AndroidSettingsStore.load(this.activity);
                    // 同时重置当前配置和本地保存的所有词库进度
                    currentSettings.resetPlaybackProgress();
                    AndroidSettingsStore.clearAllPlaybackProgress(this.activity);
                    AndroidSettingsStore.savePlaybackProgress(
                            this.activity,
                            currentSettings,
                            currentSettings.getVocabularyFileName()
                    );
                    notifyServiceReload();
                    this.onReloadPage.run();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 清除进度后通知正在运行的悬浮窗从头加载。 */
    private void notifyServiceReload() {
        if (OverlayService.isRunning) {
            Intent intent = new Intent(this.activity, OverlayService.class);
            intent.setAction(OverlayService.ACTION_RELOAD);
            this.activity.startService(intent);
        }
    }
}

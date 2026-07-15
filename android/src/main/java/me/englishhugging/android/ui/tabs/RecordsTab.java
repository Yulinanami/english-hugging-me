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
 * 播放记录页面的数据展示与清除交互。
 *
 * <p>记录行由 XML 条目逐个创建；清除操作同时重置内存设置和本地进度文件，
 * 防止服务重新加载后恢复已经删除的进度。</p>
 */
public final class RecordsTab {
    /** 页面所属 Activity。 */
    private final MainActivity activity;

    /** 负责图标字体等公共 UI 行为。 */
    private final AndroidUi ui;

    /** 清除记录后重新创建当前页面的动作。 */
    private final Runnable onReloadPage;

    /** 顶部返回按钮触发的回首页动作。 */
    private final Runnable goHome;

    /** 保存记录页所需依赖和页面导航动作。 */
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

    /** 创建记录页，并把本地播放记录逐行渲染到 XML 容器中。 */
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
                    // 同时清理当前设置对象和各词库的持久化进度。
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

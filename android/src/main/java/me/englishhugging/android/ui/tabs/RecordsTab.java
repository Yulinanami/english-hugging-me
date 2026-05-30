package me.englishhugging.android.ui.tabs;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.overlay.OverlayService;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.core.settings.AppSettings;

/**
 * 播放记录管理页面。
 *
 * <p>这个类生成一个无 XML 的 UI 页面，用于以列表形式展示所有词库的记忆进度，
 * 并且提供了一键清空所有历史记录的危险操作。
 */
public final class RecordsTab {
    
    // --- 外部依赖 ---
    private final MainActivity activity;
    private final AndroidUi ui;
    private final Runnable onReloadPage;
    private final Runnable goHome;

    /**
     * 构造学习记录展示页。
     *
     * @param activity     宿主 Activity
     * @param ui           公共样式工厂
     * @param onReloadPage 发生数据修改时要求宿主重载当前页面的回调
     * @param goHome       点击顶部返回箭头时跳转回主界面的回调
     */
    public RecordsTab(MainActivity activity, AndroidUi ui, Runnable onReloadPage, Runnable goHome) {
        this.activity = activity;
        this.ui = ui;
        this.onReloadPage = onReloadPage;
        this.goHome = goHome;
    }

    /**
     * 动态拼装页面视图内容。
     */
    public void buildContent(LinearLayout pageHeader, LinearLayout pageContent) {
        // 1. 带返回箭头的二级头部
        LinearLayout header = this.ui.headerRow("播放记录", "");
        
        TextView backIcon = new TextView(this.activity);
        backIcon.setText("chevron_left");
        backIcon.setTextSize(32);
        backIcon.setTypeface(this.ui.getIconFont());
        backIcon.setTextColor(AndroidUi.TEXT_PRIMARY);
        backIcon.setGravity(Gravity.CENTER);
        backIcon.setPadding(0, 0, this.ui.dp(8), 0);
        
        backIcon.setOnClickListener(v -> {
            if (this.goHome != null) {
                this.goHome.run();
            }
        });
        
        header.addView(backIcon, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pageHeader.addView(header, this.ui.matchWidthWithBottomMargin(12));

        // 2. 进度记录卡片
        pageContent.addView(this.ui.sectionLabel("记录"), this.ui.matchWidthWithBottomMargin(12));
        
        LinearLayout recordsCard = this.ui.card();
        for (String line : AndroidSettingsStore.playbackRecordLines(this.activity)) {
            recordsCard.addView(this.ui.recordRow(line), this.ui.matchWidthWrapHeight());
        }
        pageContent.addView(recordsCard, this.ui.matchWidthWithBottomMargin(16));

        // 3. 底部危险操作区：清除所有记录
        MaterialButton clearBtn = this.ui.secondaryButton("清除所有记录");
        clearBtn.setTextColor(Color.rgb(239, 68, 68));
        
        clearBtn.setOnClickListener(view -> {
            new AlertDialog.Builder(this.activity)
                    .setTitle("确认清除")
                    .setMessage("确定要清除所有播放记录吗？这将使所有词汇本从头开始播放。")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 在内存中抹除当前词库进度
                        AppSettings currentSettings = AndroidSettingsStore.load(this.activity);
                        currentSettings.resetPlaybackProgress();
                        
                        // 清除磁盘中所有词库文件关联的游标
                        AndroidSettingsStore.clearAllPlaybackProgress(this.activity);
                        
                        // 写回默认的 0 进度
                        AndroidSettingsStore.savePlaybackProgress(this.activity, currentSettings, currentSettings.getVocabularyFileName());
                        
                        // 通知前台服务热重载引擎
                        notifyServiceReload();
                        
                        // 强制刷新本页 UI
                        this.onReloadPage.run();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        
        pageContent.addView(clearBtn, this.ui.matchWidthWithTopMargin(8));
    }

    /**
     * 如果悬浮窗正挂载在屏幕上，向它发送热更新广播，让它重新读取进度。
     */
    private void notifyServiceReload() {
        if (OverlayService.isRunning) {
            Intent intent = new Intent(this.activity, OverlayService.class);
            intent.setAction(OverlayService.ACTION_RELOAD);
            this.activity.startService(intent);
        }
    }
}

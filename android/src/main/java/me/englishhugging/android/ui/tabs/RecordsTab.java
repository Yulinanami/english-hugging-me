package me.englishhugging.android.ui.tabs;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButton;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.overlay.OverlayService;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.core.settings.AppSettings;

public final class RecordsTab {
    private final MainActivity activity;
    private final AndroidUi ui;
    private final Runnable onReloadPage;

    public RecordsTab(MainActivity activity, AndroidUi ui, Runnable onReloadPage) {
        this.activity = activity;
        this.ui = ui;
        this.onReloadPage = onReloadPage;
    }

    public void buildContent(LinearLayout pageContent) {
        pageContent.addView(ui.headerRow("播放记录", ""), ui.matchWidthWithBottomMargin(28));

        pageContent.addView(ui.sectionLabel("记录"), ui.matchWidthWithBottomMargin(12));
        LinearLayout recordsCard = ui.card();
        for (String line : AndroidSettingsStore.playbackRecordLines(activity)) {
            recordsCard.addView(ui.recordRow(line), ui.matchWidthWrapHeight());
        }
        pageContent.addView(recordsCard, ui.matchWidthWithBottomMargin(16));

        MaterialButton clearBtn = ui.secondaryButton("清除所有记录");
        clearBtn.setTextColor(Color.rgb(239, 68, 68));
        clearBtn.setOnClickListener(view -> {
            new AlertDialog.Builder(activity)
                    .setTitle("确认清除")
                    .setMessage("确定要清除所有播放记录吗？这将使所有词汇本从头开始播放。")
                    .setPositiveButton("确定", (dialog, which) -> {
                        AppSettings currentSettings = AndroidSettingsStore.load(activity);
                        currentSettings.resetPlaybackProgress();
                        AndroidSettingsStore.clearAllPlaybackProgress(activity);
                        AndroidSettingsStore.savePlaybackProgress(activity, currentSettings, currentSettings.getVocabularyFileName());
                        notifyServiceReload();
                        onReloadPage.run();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        pageContent.addView(clearBtn, ui.matchWidthWithTopMargin(8));
    }

    private void notifyServiceReload() {
        if (OverlayService.isRunning) {
            Intent intent = new Intent(activity, OverlayService.class);
            intent.setAction(OverlayService.ACTION_RELOAD);
            activity.startService(intent);
        }
    }
}

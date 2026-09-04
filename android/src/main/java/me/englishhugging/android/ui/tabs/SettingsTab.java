package me.englishhugging.android.ui.tabs;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.SeekBar;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.R;
import me.englishhugging.android.databinding.PageSettingsBinding;
import me.englishhugging.android.overlay.OverlayService;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.DisplayMode;
import me.englishhugging.core.settings.OverlayMode;
import me.englishhugging.core.settings.PlaybackMode;

/**
 * Android 手机端“应用设置”界面，负责管理词库、播放规则与外观颜色等所有配置项。
 *
 * <p>用户在界面上修改任意配置项后，会自动保存到本地，并实时通知悬浮窗刷新。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * SettingsTab settingsTab = new SettingsTab(activity, ui, () -> showHomePage());
 * View settingsView = settingsTab.getView();
 * pageContainer.addView(settingsView);
 * </code></pre>
 */
public final class SettingsTab {
    /** 文本输入或透明度滑块停止变化后再保存的等待时间，单位：毫秒 */
    private static final long INPUT_DEBOUNCE_MILLIS = 250L;
    /** 字号加减按钮每次调整的大小，单位：dp */
    private static final int FONT_SIZE_STEP = 2;

    /** 预设颜色列表（包含代码与中文说明） */
    private static final String[] PRESET_COLORS = {
        "#FFFFFF (纯白)", "#FDE68A (淡黄)", "#7DD3FC (浅蓝)", "#86EFAC (浅绿)",
        "#FCA5A5 (浅红)", "#D8B4FE (浅紫)", "#CBD5E1 (灰蓝)", "#000000 (纯黑)"
    };

    /** 所属的主界面对象 */
    private final MainActivity activity;

    /** 界面辅助工具 */
    private final AndroidUi ui;

    /** 返回首页的回调 */
    private final Runnable goHome;

    /** 设置页视图绑定对象 */
    private PageSettingsBinding binding;

    /** 标记是否正在批量更新开关状态，防止触发多余的保存 */
    private boolean isUpdatingSwitches;

    /** 是否还有一次文本或滑块修改等待保存 */
    private boolean isInputSavePending;

    /** 是否还有一次开关修改等待通知悬浮窗重新读取设置 */
    private boolean isServiceReloadPending;

    /** 开关动画结束后通知悬浮窗重新读取设置 */
    private final Runnable delayedServiceReload = () -> {
        this.isServiceReloadPending = false;
        notifyServiceReload();
    };

    /** 用户停止连续输入后保存页面内容，并通知悬浮窗重新读取设置 */
    private final Runnable delayedSaveAndReload = () -> {
        this.isInputSavePending = false;
        saveAndReload();
    };

    /**
     * 创建设置页面对象。
     *
     * @param activity 所属的主界面对象
     * @param ui       界面辅助工具
     * @param goHome   返回首页的回调
     */
    public SettingsTab(MainActivity activity, AndroidUi ui, Runnable goHome) {
        this.activity = activity;
        this.ui = ui;
        this.goHome = goHome;
    }

    /**
     * 加载并返回设置页视图。
     *
     * @return 设置页根视图
     */
    public View getView() {
        flushPendingCallbacks();
        this.binding = PageSettingsBinding.inflate(this.activity.getLayoutInflater());
        this.ui.styleIcon(this.binding.backIcon);
        this.binding.backIcon.setOnClickListener(view -> this.goHome.run());

        // 下拉框绑定候选数据
        this.ui.bindDropdown(this.binding.vocabularyDropdown, AndroidSettingsStore.VOCABULARY_FILES);
        this.ui.bindDropdown(this.binding.displayModeDropdown, DisplayMode.labels());
        this.ui.bindDropdown(this.binding.playbackModeDropdown, PlaybackMode.labels());
        this.ui.bindDropdown(this.binding.overlayModeDropdown, OverlayMode.labels());
        this.ui.bindDropdown(this.binding.wordColor, PRESET_COLORS);
        this.ui.bindDropdown(this.binding.typeColor, PRESET_COLORS);
        this.ui.bindDropdown(this.binding.translationColor, PRESET_COLORS);
        this.ui.bindDropdown(this.binding.phraseColor, PRESET_COLORS);

        configureStepper(
                this.binding.wordFontMinus,
                this.binding.wordFontPlus,
                this.binding.wordFontSize,
                10,
                72
        );
        configureStepper(
                this.binding.detailFontMinus,
                this.binding.detailFontPlus,
                this.binding.detailFontSize,
                8,
                60
        );

        bindSettings(AndroidSettingsStore.load(this.activity));
        bindSettingsListeners();
        return this.binding.getRoot();
    }

    /** 把保存的十六进制颜色转换为下拉框中的完整显示文字。 */
    private String formatColor(String hex) {
        if (hex == null) {
            return "";
        }
        String upper = hex.toUpperCase(Locale.ROOT);
        for (String preset : PRESET_COLORS) {
            if (preset.startsWith(upper)) {
                return preset;
            }
        }
        return upper;
    }

    /** 从“#RRGGBB (名称)”格式的文本中获取十六进制颜色字符串。 */
    private String extractHex(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return text.split(" ")[0];
    }

    /** 给一组减号/加号按钮绑定带上下限的数值调节事件。 */
    private void configureStepper(
            MaterialButton minusButton,
            MaterialButton plusButton,
            EditText input,
            int min,
            int max
    ) {
        minusButton.setOnClickListener(
                view -> adjustNumber(input, -FONT_SIZE_STEP, min, max)
        );
        plusButton.setOnClickListener(
                view -> adjustNumber(input, FONT_SIZE_STEP, min, max)
        );
    }

    /** 按指定步长调整数字输入框，并确保结果不会超出允许范围。 */
    private void adjustNumber(EditText input, int delta, int min, int max) {
        try {
            int current = Integer.parseInt(input.getText().toString());
            int adjusted = Math.max(min, Math.min(max, current + delta));
            if (adjusted != current) {
                setInteger(input, adjusted);
            }
        } catch (RuntimeException ignored) {
            // 保持用户当前输入，等待下一次有效修改。
        }
    }

    /**
     * 将当前配置的值填入界面控件中。
     *
     * @param settings 应用配置
     */
    private void bindSettings(AppSettings settings) {
        this.binding.vocabularyDropdown.setText(settings.getVocabularyFileName(), false);
        this.binding.displayModeDropdown.setText(settings.getDisplayMode().toString(), false);
        this.binding.playbackModeDropdown.setText(settings.getPlaybackMode().toString(), false);
        this.binding.loopPlaybackSwitch.setChecked(settings.isLoopPlayback());
        this.binding.overlayModeDropdown.setText(settings.getOverlayMode().toString(), false);
        setInteger(this.binding.intervalSeconds, settings.getIntervalSeconds());
        this.binding.startingPrefix.setText(settings.getStartingPrefix());
        this.binding.opacitySeekBar.setProgress((int) Math.round(settings.getOpacity() * 100) - 20);

        this.binding.wordColor.setText(formatColor(settings.getWordColor()), false);
        this.binding.typeColor.setText(formatColor(settings.getTypeColor()), false);
        this.binding.translationColor.setText(formatColor(settings.getTranslationColor()), false);
        this.binding.phraseColor.setText(formatColor(settings.getPhraseColor()), false);
        setInteger(this.binding.wordFontSize, settings.getWordFontSize());
        setInteger(this.binding.detailFontSize, settings.getDetailFontSize());

        // 自动适配和手动调整互斥；初始化期间暂停监听，避免产生无意义的保存。
        this.isUpdatingSwitches = true;
        this.binding.autoSizeSwitch.setChecked(
                settings.getWidth() <= 0 && settings.getHeight() <= 0 && !settings.isResizeMode()
        );
        this.binding.resizeModeSwitch.setChecked(settings.isResizeMode());
        this.isUpdatingSwitches = false;

        setInteger(this.binding.fillBlankInterval, settings.getFillBlankIntervalSeconds());
        this.binding.fillBlankHidePhrasesSwitch.setChecked(settings.isFillBlankHidePhrases());
        this.binding.fillBlankShowTranslationSwitch.setChecked(
                settings.isFillBlankShowTranslation()
        );
    }

    /**
     * 读取当前页面值、更新本地设置，并通知运行中的悬浮窗重新加载。
     *
     * <p>该方法会被多个输入事件回调调用，因此每次都先读取最新设置，只修改页面负责的字段。</p>
     */
    private void saveAndReload() {
        cancelPendingCallbacks();
        saveSettings(true);
    }

    /** 用户停止连续输入达到设定毫秒数后，合并保存并通知悬浮窗重新读取设置。 */
    private void saveAndReloadDebounced() {
        if (this.binding == null) {
            return;
        }

        // 输入变化与等待中的开关刷新合并，避免连续两次通知悬浮窗重新读取设置。
        this.binding.getRoot().removeCallbacks(this.delayedServiceReload);
        this.isServiceReloadPending = false;
        this.binding.getRoot().removeCallbacks(this.delayedSaveAndReload);
        this.isInputSavePending = true;
        this.binding.getRoot().postDelayed(
                this.delayedSaveAndReload,
                INPUT_DEBOUNCE_MILLIS
        );
    }

    /** 立即保存开关状态，并在开关动画结束后通知悬浮窗重新读取设置。 */
    private void saveSwitchAndReload() {
        cancelPendingCallbacks();
        saveSettings(false);
        if (this.binding == null) {
            return;
        }

        int animationDuration = this.activity.getResources().getInteger(
                android.R.integer.config_shortAnimTime
        );
        this.isServiceReloadPending = true;
        this.binding.getRoot().postDelayed(this.delayedServiceReload, animationDuration);
    }

    /** 取消当前设置页上尚未执行的延迟任务。 */
    private void cancelPendingCallbacks() {
        if (this.binding != null) {
            this.binding.getRoot().removeCallbacks(this.delayedSaveAndReload);
            this.binding.getRoot().removeCallbacks(this.delayedServiceReload);
        }
        this.isInputSavePending = false;
        this.isServiceReloadPending = false;
    }

    /** 重新创建设置页前提交上一页尚未执行的最后一次变更。 */
    private void flushPendingCallbacks() {
        if (this.binding == null) {
            return;
        }

        this.binding.getRoot().removeCallbacks(this.delayedSaveAndReload);
        this.binding.getRoot().removeCallbacks(this.delayedServiceReload);
        if (this.isInputSavePending) {
            this.isInputSavePending = false;
            this.isServiceReloadPending = false;
            saveSettings(true);
        } else if (this.isServiceReloadPending) {
            this.isServiceReloadPending = false;
            notifyServiceReload();
        }
    }

    /** 把页面中的当前设置写入本地，并按需立即刷新悬浮窗。 */
    private void saveSettings(boolean reloadServiceImmediately) {
        if (this.binding == null) {
            return;
        }

        AppSettings settings = AndroidSettingsStore.load(this.activity);
        String previousVocabularyFileName = settings.getVocabularyFileName();
        PlaybackMode previousPlaybackMode = settings.getPlaybackMode();
        // 切换词库前先保存旧词库进度，防止当前播放位置丢失。
        AndroidSettingsStore.savePlaybackProgress(
                this.activity,
                settings,
                previousVocabularyFileName
        );

        settings.setVocabularyFileName(this.ui.selectedValue(
                this.binding.vocabularyDropdown,
                AndroidSettingsStore.VOCABULARY_FILES,
                AppSettings.DEFAULT_VOCABULARY_FILE_NAME
        ));
        settings.setDisplayMode(DisplayMode.values()[this.ui.selectedIndex(
                this.binding.displayModeDropdown,
                DisplayMode.labels()
        )]);
        settings.setPlaybackMode(PlaybackMode.values()[this.ui.selectedIndex(
                this.binding.playbackModeDropdown,
                PlaybackMode.labels()
        )]);
        settings.setLoopPlayback(this.binding.loopPlaybackSwitch.isChecked());
        settings.setOverlayMode(OverlayMode.values()[this.ui.selectedIndex(
                this.binding.overlayModeDropdown,
                OverlayMode.labels()
        )]);

        boolean vocabularyChanged = !previousVocabularyFileName.equals(
                settings.getVocabularyFileName()
        );
        boolean playbackModeChanged = previousPlaybackMode != settings.getPlaybackMode();
        boolean prefixChanged = !this.binding.startingPrefix.getText().toString().equals(
                settings.getStartingPrefix()
        );

        settings.setStartingPrefix(this.binding.startingPrefix.getText().toString());
        // 词库或起始前缀变化后，需要重新定位新词库对应的播放位置。
        if (vocabularyChanged || prefixChanged) {
            settings.resetPlaybackProgress();
            AndroidSettingsStore.loadPlaybackProgress(
                    this.activity,
                    settings,
                    settings.getVocabularyFileName()
            );
        } else if (playbackModeChanged) {
            settings.resetPlaybackProgress();
        }

        try {
            settings.setIntervalSeconds(Integer.parseInt(
                    this.binding.intervalSeconds.getText().toString()
            ));
        } catch (RuntimeException ignored) {
            settings.setIntervalSeconds(8);
        }

        settings.setOpacity((this.binding.opacitySeekBar.getProgress() + 20) / 100.0);
        settings.setWordColor(extractHex(this.binding.wordColor.getText().toString()));
        settings.setTypeColor(extractHex(this.binding.typeColor.getText().toString()));
        settings.setTranslationColor(extractHex(
                this.binding.translationColor.getText().toString()
        ));
        settings.setPhraseColor(extractHex(this.binding.phraseColor.getText().toString()));

        // 用户正在输入数字时可能短暂出现空字符串，此时不覆盖已有字号设置。
        try {
            settings.setWordFontSize(Integer.parseInt(
                    this.binding.wordFontSize.getText().toString()
            ));
        } catch (RuntimeException ignored) {
            // 保留现有值。
        }
        try {
            settings.setDetailFontSize(Integer.parseInt(
                    this.binding.detailFontSize.getText().toString()
            ));
        } catch (RuntimeException ignored) {
            // 保留现有值。
        }

        // 自动适配使用宽高为 0 表示由悬浮窗根据屏幕空间计算尺寸。
        if (this.binding.autoSizeSwitch.isChecked()) {
            settings.setWidth(0);
            settings.setHeight(0);
        }
        settings.setResizeMode(this.binding.resizeModeSwitch.isChecked());

        try {
            settings.setFillBlankIntervalSeconds(Integer.parseInt(
                    this.binding.fillBlankInterval.getText().toString()
            ));
        } catch (RuntimeException ignored) {
            settings.setFillBlankIntervalSeconds(3);
        }
        settings.setFillBlankHidePhrases(
                this.binding.fillBlankHidePhrasesSwitch.isChecked()
        );
        settings.setFillBlankShowTranslation(
                this.binding.fillBlankShowTranslationSwitch.isChecked()
        );

        // 先保存完整设置，再单独保存当前词库的播放进度快照。
        AndroidSettingsStore.save(this.activity, settings);
        AndroidSettingsStore.savePlaybackProgress(
                this.activity,
                settings,
                settings.getVocabularyFileName()
        );
        if (reloadServiceImmediately) {
            notifyServiceReload();
        }
    }

    /** 更新数字输入框的显示文本。 */
    private void setInteger(EditText input, int value) {
        input.setText(this.activity.getString(R.string.integer_value, value));
    }

    /** 为所有可编辑控件绑定即时保存事件。 */
    private void bindSettingsListeners() {
        // 固定选项只有在用户真正选中某一项后才触发保存。
        AdapterView.OnItemClickListener dropdownListener =
                (parent, view, position, id) -> saveAndReload();
        this.binding.vocabularyDropdown.setOnItemClickListener(dropdownListener);
        this.binding.displayModeDropdown.setOnItemClickListener(dropdownListener);
        this.binding.playbackModeDropdown.setOnItemClickListener(dropdownListener);
        this.binding.overlayModeDropdown.setOnItemClickListener(dropdownListener);
        this.binding.wordColor.setOnItemClickListener(dropdownListener);
        this.binding.typeColor.setOnItemClickListener(dropdownListener);
        this.binding.translationColor.setOnItemClickListener(dropdownListener);
        this.binding.phraseColor.setOnItemClickListener(dropdownListener);

        // 文本类设置在内容变更后统一保存，减少重复的代码。
        TextWatcher textChangeListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                saveAndReloadDebounced();
            }
        };
        this.binding.intervalSeconds.addTextChangedListener(textChangeListener);
        this.binding.startingPrefix.addTextChangedListener(textChangeListener);
        this.binding.wordColor.addTextChangedListener(textChangeListener);
        this.binding.typeColor.addTextChangedListener(textChangeListener);
        this.binding.translationColor.addTextChangedListener(textChangeListener);
        this.binding.phraseColor.addTextChangedListener(textChangeListener);
        this.binding.wordFontSize.addTextChangedListener(textChangeListener);
        this.binding.detailFontSize.addTextChangedListener(textChangeListener);
        this.binding.fillBlankInterval.addTextChangedListener(textChangeListener);

        // 自动适配与手动调整模式不能同时启用。
        this.binding.autoSizeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (this.isUpdatingSwitches) {
                return;
            }
            if (isChecked) {
                this.isUpdatingSwitches = true;
                this.binding.resizeModeSwitch.setChecked(false);
                this.isUpdatingSwitches = false;
            }
            saveSwitchAndReload();
        });
        this.binding.resizeModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (this.isUpdatingSwitches) {
                return;
            }
            if (isChecked) {
                this.isUpdatingSwitches = true;
                this.binding.autoSizeSwitch.setChecked(false);
                this.isUpdatingSwitches = false;
            }
            saveSwitchAndReload();
        });
        this.binding.loopPlaybackSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> saveSwitchAndReload()
        );
        this.binding.fillBlankHidePhrasesSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> saveSwitchAndReload()
        );
        this.binding.fillBlankShowTranslationSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> saveSwitchAndReload()
        );
        // 拖动过程中只响应用户产生的进度变化，忽略初始化赋值。
        this.binding.opacitySeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        if (fromUser) {
                            saveAndReloadDebounced();
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        saveAndReload();
                    }
                }
        );
    }

    /** 设置保存后通知正在运行的悬浮窗使用最新配置。 */
    private void notifyServiceReload() {
        if (OverlayService.isRunning) {
            Intent intent = new Intent(this.activity, OverlayService.class);
            intent.setAction(OverlayService.ACTION_RELOAD);
            this.activity.startService(intent);
        }
    }
}

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
 * 设置页面的模型绑定、输入监听和即时保存逻辑。
 *
 * <p>布局、主题和控件样式由 XML 资源负责；本类只负责把 {@link AppSettings} 映射到控件，
 * 再将用户修改写回本地设置。悬浮窗正在运行时，保存完成后会通知服务立即重新加载。</p>
 */
public final class SettingsTab {
    /** 颜色下拉框的固定候选值；括号中的中文仅用于界面展示。 */
    private static final String[] PRESET_COLORS = {
        "#FFFFFF (纯白)", "#FDE68A (淡黄)", "#7DD3FC (浅蓝)", "#86EFAC (浅绿)",
        "#FCA5A5 (浅红)", "#D8B4FE (浅紫)", "#CBD5E1 (灰蓝)", "#000000 (纯黑)"
    };

    /** 页面所属 Activity，用于读取资源、设置存储和发送服务指令。 */
    private final MainActivity activity;

    /** 负责下拉框和图标字体等公共 UI 行为。 */
    private final AndroidUi ui;

    /** 顶部返回按钮触发的回首页动作。 */
    private final Runnable goHome;

    /** 当前设置页的 View Binding；页面重建时会替换为新的实例。 */
    private PageSettingsBinding binding;

    /** 程序主动联动两个互斥开关时，阻止监听器重复保存。 */
    private boolean isUpdatingSwitches;

    /** 开关动画结束后发送的悬浮窗刷新任务；重复切换时只保留最后一次。 */
    private final Runnable delayedServiceReload = this::notifyServiceReload;

    /** 保存设置页所需依赖和导航动作。 */
    public SettingsTab(MainActivity activity, AndroidUi ui, Runnable goHome) {
        this.activity = activity;
        this.ui = ui;
        this.goHome = goHome;
    }

    /** 创建设置页，初始化下拉框和步进按钮，再加载设置并绑定监听器。 */
    public View getView() {
        this.binding = PageSettingsBinding.inflate(this.activity.getLayoutInflater());
        this.ui.styleIcon(this.binding.backIcon);
        this.binding.backIcon.setOnClickListener(view -> this.goHome.run());

        // 下拉框只允许选择固定值，适配器和夜间模式文字颜色由 AndroidUi 统一处理。
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
                2,
                10,
                72
        );
        configureStepper(
                this.binding.detailFontMinus,
                this.binding.detailFontPlus,
                this.binding.detailFontSize,
                2,
                8,
                60
        );

        bindSettings(AndroidSettingsStore.load(this.activity));
        bindSettingsListeners();
        return this.binding.getRoot();
    }

    /** 把持久化的十六进制颜色转换为下拉框中的完整显示文字。 */
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

    /** 从“#RRGGBB (名称)”形式的显示文字中提取可保存的颜色值。 */
    private String extractHex(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return text.split(" ")[0];
    }

    /** 给一组减号/加号按钮绑定带上下限的数字调整逻辑。 */
    private void configureStepper(
            MaterialButton minusButton,
            MaterialButton plusButton,
            EditText input,
            int step,
            int min,
            int max
    ) {
        minusButton.setOnClickListener(view -> adjustNumber(input, -step, min, max));
        plusButton.setOnClickListener(view -> adjustNumber(input, step, min, max));
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

    /** 把设置模型的全部字段写入页面控件。 */
    private void bindSettings(AppSettings settings) {
        this.binding.vocabularyDropdown.setText(settings.getVocabularyFileName(), false);
        this.binding.displayModeDropdown.setText(settings.getDisplayMode().getLabel(), false);
        this.binding.playbackModeDropdown.setText(settings.getPlaybackMode().getLabel(), false);
        this.binding.loopPlaybackSwitch.setChecked(settings.isLoopPlayback());
        this.binding.overlayModeDropdown.setText(settings.getOverlayMode().getLabel(), false);
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
     * <p>该方法会被多个输入监听器调用，因此每次都先读取最新设置，只修改页面负责的字段。</p>
     */
    private void saveAndReload() {
        if (this.binding != null) {
            this.binding.getRoot().removeCallbacks(this.delayedServiceReload);
        }
        saveSettings(true);
    }

    /** 立即保存开关状态，并在系统短动画结束后刷新悬浮窗，避免重载阻塞过渡帧。 */
    private void saveSwitchAndReload() {
        saveSettings(false);
        if (this.binding == null) {
            return;
        }

        this.binding.getRoot().removeCallbacks(this.delayedServiceReload);
        int animationDuration = this.activity.getResources().getInteger(
                android.R.integer.config_shortAnimTime
        );
        this.binding.getRoot().postDelayed(this.delayedServiceReload, animationDuration);
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

    /** 使用本地化整数资源更新输入框，避免各处重复字符串转换。 */
    private void setInteger(EditText input, int value) {
        input.setText(this.activity.getString(R.string.integer_value, value));
    }

    /** 为所有可编辑控件绑定即时保存监听器。 */
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

        // 文本类设置在内容变更后统一保存，减少重复的监听器实现。
        TextWatcher textChangeListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                saveAndReload();
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
                            saveAndReload();
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
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

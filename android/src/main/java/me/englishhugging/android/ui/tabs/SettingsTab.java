package me.englishhugging.android.ui.tabs;

import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.overlay.OverlayService;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.core.model.Phrase;
import me.englishhugging.core.model.Translation;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.DisplayMode;
import me.englishhugging.core.settings.OverlayMode;
import me.englishhugging.core.settings.PlaybackMode;

/**
 * Android 端全局设置面板。
 *
 * <p>这个庞大的页面将 {@link AppSettings} 模型中几十项配置一一映射为 UI 控件。
 * 它采用了典型的“所见即所得”和“即时生效”的策略，用户改变任何一个开关、输入框或进度条，
 * 都会触发底层的重新拉取和重新渲染，并同时向挂载在屏幕上的前台服务发射更新信号。
 */
public final class SettingsTab {
    
    // --- 外部依赖 ---
    private final MainActivity activity;
    private final AndroidUi ui;
    private final Runnable goHome;

    // --- 表单控件群 ---
    // 基础播放
    private MaterialAutoCompleteTextView vocabularyDropdown;
    private MaterialAutoCompleteTextView displayModeDropdown;
    private MaterialAutoCompleteTextView playbackModeDropdown;
    private Switch loopPlaybackSwitch;
    private MaterialAutoCompleteTextView overlayModeDropdown;
    private EditText intervalSeconds;
    private SeekBar opacitySeekBar;
    private EditText startingPrefix;
    private MaterialAutoCompleteTextView loopPlaybackDropdown;
    
    // 外观颜色与尺寸
    private MaterialAutoCompleteTextView wordColor;
    private MaterialAutoCompleteTextView typeColor;
    private MaterialAutoCompleteTextView translationColor;
    private MaterialAutoCompleteTextView phraseColor;
    private EditText wordFontSize;
    private EditText detailFontSize;
    private Switch autoSizeSwitch;
    private Switch resizeModeSwitch;
    
    // 挖空模式参数
    private EditText fillBlankInterval;
    private Switch fillBlankHidePhrasesSwitch;
    private Switch fillBlankShowTranslationSwitch;

    // 用于在代码反填 UI 时临时屏蔽掉 onChange 回调，防止死循环或脏写
    private boolean isUpdatingSwitches = false;

    // 内置推荐的十六进制颜色预设字典
    private static final String[] PRESET_COLORS = {
        "#FFFFFF (纯白)", "#FDE68A (淡黄)", "#7DD3FC (浅蓝)", "#86EFAC (浅绿)",
        "#FCA5A5 (浅红)", "#D8B4FE (浅紫)", "#CBD5E1 (灰蓝)", "#000000 (纯黑)"
    };

    /**
     * 构建设置面板。
     */
    public SettingsTab(MainActivity activity, AndroidUi ui, Runnable goHome) {
        this.activity = activity;
        this.ui = ui;
        this.goHome = goHome;
    }

    /**
     * 解析纯粹的色值，如果它是预设之一，则附加上人性化的中文括号说明。
     */
    private String formatColor(String hex) {
        if (hex == null) {
            return "";
        }
        
        String upper = hex.toUpperCase();
        for (String p : PRESET_COLORS) {
            if (p.startsWith(upper)) {
                return p;
            }
        }
        return upper;
    }

    /**
     * 从形如 "#FFFFFF (纯白)" 的下拉框选择结果中，萃取出真实可用的十六进制特征码。
     */
    private String extractHex(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return text.split(" ")[0];
    }

    /**
     * 将整个冗长复杂的设置页面布局，以组块的形式追加到容器中。
     */
    public void buildContent(LinearLayout pageHeader, LinearLayout pageContent) {
        AppSettings settings = AndroidSettingsStore.load(this.activity);

        // 1. 带返回箭头的页面标题栏
        LinearLayout header = this.ui.headerRow("设置", "");
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

        // 2. 基础设置卡片
        pageContent.addView(this.ui.sectionLabel("基础设置"), this.ui.matchWidthWithBottomMargin(12));
        LinearLayout generalCard = this.ui.card();
        
        this.vocabularyDropdown = this.ui.dropdown(AndroidSettingsStore.VOCABULARY_FILES);
        this.displayModeDropdown = this.ui.dropdown(DisplayMode.labels());
        this.playbackModeDropdown = this.ui.dropdown(PlaybackMode.labels());
        this.loopPlaybackSwitch = new Switch(this.activity);
        this.loopPlaybackSwitch.setText("");
        this.overlayModeDropdown = this.ui.dropdown(OverlayMode.labels());
        this.intervalSeconds = this.ui.input(Integer.toString(settings.getIntervalSeconds()));
        this.intervalSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        this.startingPrefix = this.ui.input(settings.getStartingPrefix());
        this.opacitySeekBar = new SeekBar(this.activity);
        this.opacitySeekBar.setMax(80);
        
        generalCard.addView(this.ui.settingItem("词汇本", "选择要播放的词汇本", this.vocabularyDropdown), this.ui.matchWidthWrapHeight());
        generalCard.addView(this.ui.settingItem("显示内容", "悬浮窗展示哪些内容", this.displayModeDropdown), this.ui.matchWidthWrapHeight());
        generalCard.addView(this.ui.settingItem("播放顺序", "顺序、随机或随机不重复", this.playbackModeDropdown), this.ui.matchWidthWrapHeight());
        generalCard.addView(this.ui.settingItem("悬浮行为", "拖动、锁定或点击穿透", this.overlayModeDropdown), this.ui.matchWidthWrapHeight());
        generalCard.addView(this.ui.settingItem("切换间隔", "单位：秒", this.intervalSeconds), this.ui.matchWidthWrapHeight());
        generalCard.addView(this.ui.settingItem("透明度", "调整悬浮窗透明度", this.opacitySeekBar), this.ui.matchWidthWrapHeight());
        pageContent.addView(generalCard, this.ui.matchWidthWithBottomMargin(26));
        
        // 3. 高级过滤卡片
        pageContent.addView(this.ui.sectionLabel("按前缀播放"), this.ui.matchWidthWithBottomMargin(12));
        LinearLayout prefixCard = this.ui.card();
        this.startingPrefix.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.startingPrefix.setHint("留空表示播放全部单词");
        
        prefixCard.addView(this.ui.settingItem("特定字母前缀", "设置要过滤的单词前缀", this.startingPrefix), this.ui.matchWidthWrapHeight());
        prefixCard.addView(this.ui.settingSwitchItem("循环播放", "开启：无限循环；关闭：播完一遍即停", this.loopPlaybackSwitch), this.ui.matchWidthWrapHeight());
        pageContent.addView(prefixCard, this.ui.matchWidthWithBottomMargin(26));

        // 4. 外观与字号定制卡片
        pageContent.addView(this.ui.sectionLabel("外观"), this.ui.matchWidthWithBottomMargin(12));
        LinearLayout colorCard = this.ui.card();
        
        this.wordColor = this.ui.dropdown(PRESET_COLORS);
        this.typeColor = this.ui.dropdown(PRESET_COLORS);
        this.translationColor = this.ui.dropdown(PRESET_COLORS);
        this.phraseColor = this.ui.dropdown(PRESET_COLORS);
        
        colorCard.addView(this.ui.settingItem("单词颜色", "选择单词颜色", this.wordColor), this.ui.matchWidthWrapHeight());
        colorCard.addView(this.ui.settingItem("词性颜色", "选择词性颜色", this.typeColor), this.ui.matchWidthWrapHeight());
        colorCard.addView(this.ui.settingItem("释义颜色", "选择释义颜色", this.translationColor), this.ui.matchWidthWrapHeight());
        colorCard.addView(this.ui.settingItem("短语/例句颜色", "选择短语/例句颜色", this.phraseColor), this.ui.matchWidthWrapHeight());
        
        this.wordFontSize = this.ui.input(Integer.toString(settings.getWordFontSize()));
        this.detailFontSize = this.ui.input(Integer.toString(settings.getDetailFontSize()));
        this.resizeModeSwitch = new Switch(this.activity);
        this.autoSizeSwitch = new Switch(this.activity);
        
        colorCard.addView(this.ui.settingItem("单词字号", "悬浮窗单词字体大小", this.ui.numberAdjuster(this.wordFontSize, 2, 10, 72)), this.ui.matchWidthWrapHeight());
        colorCard.addView(this.ui.settingItem("释义字号", "悬浮窗释义字体大小", this.ui.numberAdjuster(this.detailFontSize, 2, 8, 60)), this.ui.matchWidthWrapHeight());
        colorCard.addView(this.ui.settingSwitchItem("自动适配内容大小", "悬浮窗尺寸随文字自动变化（恢复默认）", this.autoSizeSwitch), this.ui.matchWidthWrapHeight());
        colorCard.addView(this.ui.settingSwitchItem("调整悬浮窗大小", "开启后右下角出现可拖拽手柄", this.resizeModeSwitch), this.ui.matchWidthWrapHeight());
        
        pageContent.addView(colorCard, this.ui.matchWidthWithBottomMargin(26));

        // 5. 挖空模式参数调整卡片
        pageContent.addView(this.ui.sectionLabel("挖空模式设置"), this.ui.matchWidthWithBottomMargin(12));
        LinearLayout fillBlankCard = this.ui.card();
        
        this.fillBlankInterval = this.ui.input(Integer.toString(settings.getFillBlankIntervalSeconds()));
        this.fillBlankInterval.setInputType(InputType.TYPE_CLASS_NUMBER);
        this.fillBlankHidePhrasesSwitch = new Switch(this.activity);
        this.fillBlankShowTranslationSwitch = new Switch(this.activity);
        
        fillBlankCard.addView(this.ui.settingItem("填充间隔", "每次自动填写一个空位的时间（秒）", this.fillBlankInterval), this.ui.matchWidthWrapHeight());
        fillBlankCard.addView(this.ui.settingSwitchItem("挖空时关闭短语", "挖空阶段不显示短语/例句", this.fillBlankHidePhrasesSwitch), this.ui.matchWidthWrapHeight());
        fillBlankCard.addView(this.ui.settingSwitchItem("挖空时显示释义", "挖空阶段显示词性和释义", this.fillBlankShowTranslationSwitch), this.ui.matchWidthWrapHeight());
        
        pageContent.addView(fillBlankCard, this.ui.matchWidthWithBottomMargin(26));

        // 6. 将 Model 数据同步进 UI
        bindSettings(settings);
        
        // 7. 开启各种交互的实时监听
        bindSettingsListeners();
    }

    /**
     * 将从本地拉取的数据单向流回填至界面表单中。
     */
    private void bindSettings(AppSettings settings) {
        this.vocabularyDropdown.setText(settings.getVocabularyFileName(), false);
        this.displayModeDropdown.setText(settings.getDisplayMode().getLabel(), false);
        this.playbackModeDropdown.setText(settings.getPlaybackMode().getLabel(), false);
        this.loopPlaybackSwitch.setChecked(settings.isLoopPlayback());
        this.overlayModeDropdown.setText(settings.getOverlayMode().getLabel(), false);
        this.intervalSeconds.setText(Integer.toString(settings.getIntervalSeconds()));
        this.startingPrefix.setText(settings.getStartingPrefix());
        this.opacitySeekBar.setProgress((int) Math.round(settings.getOpacity() * 100) - 20);
        
        this.wordColor.setText(formatColor(settings.getWordColor()), false);
        this.typeColor.setText(formatColor(settings.getTypeColor()), false);
        this.translationColor.setText(formatColor(settings.getTranslationColor()), false);
        this.phraseColor.setText(formatColor(settings.getPhraseColor()), false);
        
        this.wordFontSize.setText(Integer.toString(settings.getWordFontSize()));
        this.detailFontSize.setText(Integer.toString(settings.getDetailFontSize()));
        
        // 暂停监听器，避免触发联动的开/关逻辑
        this.isUpdatingSwitches = true;
        this.autoSizeSwitch.setChecked(settings.getWidth() <= 0 && settings.getHeight() <= 0 && !settings.isResizeMode());
        this.resizeModeSwitch.setChecked(settings.isResizeMode());
        this.isUpdatingSwitches = false;

        this.fillBlankInterval.setText(Integer.toString(settings.getFillBlankIntervalSeconds()));
        this.fillBlankHidePhrasesSwitch.setChecked(settings.isFillBlankHidePhrases());
        this.fillBlankShowTranslationSwitch.setChecked(settings.isFillBlankShowTranslation());
    }

    /**
     * 万剑归宗的存盘接口：
     * 读取所有输入框的新数据 -> 写入 Model -> 写回 JSON -> 发送广播唤醒服务。
     */
    private void saveAndReload() {
        if (this.vocabularyDropdown == null) {
            return;
        }
        
        AppSettings settings = AndroidSettingsStore.load(this.activity);
        String previousVocabularyFileName = settings.getVocabularyFileName();
        PlaybackMode previousPlaybackMode = settings.getPlaybackMode();
        AndroidSettingsStore.savePlaybackProgress(this.activity, settings, previousVocabularyFileName);
        
        settings.setVocabularyFileName(this.ui.selectedValue(this.vocabularyDropdown, AndroidSettingsStore.VOCABULARY_FILES, AppSettings.DEFAULT_VOCABULARY_FILE_NAME));
        settings.setDisplayMode(DisplayMode.values()[this.ui.selectedIndex(this.displayModeDropdown, DisplayMode.labels())]);
        settings.setPlaybackMode(PlaybackMode.values()[this.ui.selectedIndex(this.playbackModeDropdown, PlaybackMode.labels())]);
        settings.setLoopPlayback(this.loopPlaybackSwitch.isChecked());
        settings.setOverlayMode(OverlayMode.values()[this.ui.selectedIndex(this.overlayModeDropdown, OverlayMode.labels())]);
        
        boolean vocabularyChanged = !previousVocabularyFileName.equals(settings.getVocabularyFileName());
        boolean playbackModeChanged = previousPlaybackMode != settings.getPlaybackMode();
        boolean prefixChanged = !this.startingPrefix.getText().toString().equals(settings.getStartingPrefix());
        
        settings.setStartingPrefix(this.startingPrefix.getText().toString());
        
        // 如果更换了词本或前缀条件，进度必须归零然后试图读取新词本的老进度
        if (vocabularyChanged || prefixChanged) {
            settings.resetPlaybackProgress();
            AndroidSettingsStore.loadPlaybackProgress(this.activity, settings, settings.getVocabularyFileName());
        } else if (playbackModeChanged) {
            // 如果仅切换模式（如顺势切成乱序），只需把进度游标归零重新算起
            settings.resetPlaybackProgress();
        }
        
        try {
            settings.setIntervalSeconds(Integer.parseInt(this.intervalSeconds.getText().toString()));
        } catch (RuntimeException ignored) {
            settings.setIntervalSeconds(8);
        }
        
        settings.setOpacity((this.opacitySeekBar.getProgress() + 20) / 100.0);
        settings.setWordColor(extractHex(this.wordColor.getText().toString()));
        settings.setTypeColor(extractHex(this.typeColor.getText().toString()));
        settings.setTranslationColor(extractHex(this.translationColor.getText().toString()));
        settings.setPhraseColor(extractHex(this.phraseColor.getText().toString()));
        
        try { 
            settings.setWordFontSize(Integer.parseInt(this.wordFontSize.getText().toString())); 
        } catch (Exception ignored) { }
        try { 
            settings.setDetailFontSize(Integer.parseInt(this.detailFontSize.getText().toString())); 
        } catch (Exception ignored) { }
        
        if (this.autoSizeSwitch.isChecked()) {
            settings.setWidth(0);
            settings.setHeight(0);
        }
        settings.setResizeMode(this.resizeModeSwitch.isChecked());
        
        try { 
            settings.setFillBlankIntervalSeconds(Integer.parseInt(this.fillBlankInterval.getText().toString())); 
        } catch (Exception ignored) { 
            settings.setFillBlankIntervalSeconds(3); 
        }
        settings.setFillBlankHidePhrases(this.fillBlankHidePhrasesSwitch.isChecked());
        settings.setFillBlankShowTranslation(this.fillBlankShowTranslationSwitch.isChecked());

        AndroidSettingsStore.save(this.activity, settings);
        AndroidSettingsStore.savePlaybackProgress(this.activity, settings, settings.getVocabularyFileName());
        
        notifyServiceReload();
    }

    /**
     * 以 Intent 意图的方式热更新正在后台跑跑的 OverlayService。
     */
    private void notifyServiceReload() {
        if (OverlayService.isRunning) {
            Intent intent = new Intent(this.activity, OverlayService.class);
            intent.setAction(OverlayService.ACTION_RELOAD);
            this.activity.startService(intent);
        }
    }

    /**
     * 将所有 UI 控件的回调统一路由至 saveAndReload() 方法上。
     */
    private void bindSettingsListeners() {
        AdapterView.OnItemClickListener dropdownListener = (parent, view, position, id) -> saveAndReload();
        this.vocabularyDropdown.setOnItemClickListener(dropdownListener);
        this.displayModeDropdown.setOnItemClickListener(dropdownListener);
        this.playbackModeDropdown.setOnItemClickListener(dropdownListener);
        this.overlayModeDropdown.setOnItemClickListener(dropdownListener);
        this.wordColor.setOnItemClickListener(dropdownListener);
        this.typeColor.setOnItemClickListener(dropdownListener);
        this.translationColor.setOnItemClickListener(dropdownListener);
        this.phraseColor.setOnItemClickListener(dropdownListener);

        TextWatcher textChangeListener = new TextWatcher() {
            @Override 
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override 
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override 
            public void afterTextChanged(Editable s) { 
                saveAndReload(); 
            }
        };
        
        this.intervalSeconds.addTextChangedListener(textChangeListener);
        this.startingPrefix.addTextChangedListener(textChangeListener);
        this.wordColor.addTextChangedListener(textChangeListener);
        this.typeColor.addTextChangedListener(textChangeListener);
        this.translationColor.addTextChangedListener(textChangeListener);
        this.phraseColor.addTextChangedListener(textChangeListener);
        this.wordFontSize.addTextChangedListener(textChangeListener);
        this.detailFontSize.addTextChangedListener(textChangeListener);
        this.fillBlankInterval.addTextChangedListener(textChangeListener);
        
        // 自动适配和可拉伸是两个互斥开关
        this.autoSizeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (this.isUpdatingSwitches) {
                return;
            }
            if (isChecked) {
                this.isUpdatingSwitches = true;
                this.resizeModeSwitch.setChecked(false);
                this.isUpdatingSwitches = false;
            }
            saveAndReload();
        });
        
        this.resizeModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (this.isUpdatingSwitches) {
                return;
            }
            if (isChecked) {
                this.isUpdatingSwitches = true;
                this.autoSizeSwitch.setChecked(false);
                this.isUpdatingSwitches = false;
            }
            saveAndReload();
        });
        
        this.loopPlaybackSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveAndReload());
        this.fillBlankHidePhrasesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveAndReload());
        this.fillBlankShowTranslationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveAndReload());

        this.opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override 
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { 
                if (fromUser) {
                    saveAndReload(); 
                }
            }
            @Override 
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override 
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

}

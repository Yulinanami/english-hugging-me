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

public final class SettingsTab {
    private final MainActivity activity;
    private final AndroidUi ui;

    private MaterialAutoCompleteTextView vocabularyDropdown;
    private MaterialAutoCompleteTextView displayModeDropdown;
    private MaterialAutoCompleteTextView playbackModeDropdown;
    private Switch loopPlaybackSwitch;
    private MaterialAutoCompleteTextView overlayModeDropdown;
    private EditText intervalSeconds;
    private MaterialAutoCompleteTextView wordColor;
    private MaterialAutoCompleteTextView typeColor;
    private MaterialAutoCompleteTextView translationColor;
    private MaterialAutoCompleteTextView phraseColor;
    private EditText wordFontSize;
    private EditText detailFontSize;
    private Switch autoSizeSwitch;
    private Switch resizeModeSwitch;
    private EditText startingPrefix;
    private MaterialAutoCompleteTextView loopPlaybackDropdown;
    private SeekBar opacitySeekBar;
    private EditText fillBlankInterval;
    private Switch fillBlankHidePhrasesSwitch;
    private Switch fillBlankShowTranslationSwitch;

    private final Runnable goHome;

    public SettingsTab(MainActivity activity, AndroidUi ui, Runnable goHome) {
        this.activity = activity;
        this.ui = ui;
        this.goHome = goHome;
    }

    private static final String[] PRESET_COLORS = {
        "#FFFFFF (纯白)", "#FDE68A (淡黄)", "#7DD3FC (浅蓝)", "#86EFAC (浅绿)",
        "#FCA5A5 (浅红)", "#D8B4FE (浅紫)", "#CBD5E1 (灰蓝)", "#000000 (纯黑)"
    };

    private String formatColor(String hex) {
        if (hex == null) return "";
        String upper = hex.toUpperCase();
        for (String p : PRESET_COLORS) if (p.startsWith(upper)) return p;
        return upper;
    }

    private String extractHex(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        return text.split(" ")[0];
    }

    public void buildContent(LinearLayout pageHeader, LinearLayout pageContent) {
        AppSettings settings = AndroidSettingsStore.load(activity);

        LinearLayout header = ui.headerRow("设置", "");
        TextView backIcon = new TextView(activity);
        backIcon.setText("chevron_left");
        backIcon.setTextSize(32);
        backIcon.setTypeface(ui.getIconFont());
        backIcon.setTextColor(AndroidUi.TEXT_PRIMARY);
        backIcon.setGravity(Gravity.CENTER);
        backIcon.setPadding(0, 0, ui.dp(8), 0);
        backIcon.setOnClickListener(v -> {
            if (goHome != null) goHome.run();
        });
        header.addView(backIcon, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pageHeader.addView(header, ui.matchWidthWithBottomMargin(12));

        pageContent.addView(ui.sectionLabel("基础设置"), ui.matchWidthWithBottomMargin(12));
        LinearLayout generalCard = ui.card();
        vocabularyDropdown = ui.dropdown(AndroidSettingsStore.VOCABULARY_FILES);
        displayModeDropdown = ui.dropdown(DisplayMode.labels());
        playbackModeDropdown = ui.dropdown(PlaybackMode.labels());
        loopPlaybackSwitch = new Switch(activity);
        loopPlaybackSwitch.setText("");
        overlayModeDropdown = ui.dropdown(OverlayMode.labels());
        intervalSeconds = ui.input(Integer.toString(settings.getIntervalSeconds()));
        intervalSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        startingPrefix = ui.input(settings.getStartingPrefix());
        opacitySeekBar = new SeekBar(activity);
        opacitySeekBar.setMax(80);
        generalCard.addView(ui.settingItem("词汇本", "选择要播放的词汇本", vocabularyDropdown), ui.matchWidthWrapHeight());
        generalCard.addView(ui.settingItem("显示内容", "悬浮窗展示哪些内容", displayModeDropdown), ui.matchWidthWrapHeight());
        generalCard.addView(ui.settingItem("播放顺序", "顺序、随机或随机不重复", playbackModeDropdown), ui.matchWidthWrapHeight());
        
        generalCard.addView(ui.settingItem("悬浮行为", "拖动、锁定或点击穿透", overlayModeDropdown), ui.matchWidthWrapHeight());
        generalCard.addView(ui.settingItem("切换间隔", "单位：秒", intervalSeconds), ui.matchWidthWrapHeight());
        generalCard.addView(ui.settingItem("透明度", "调整悬浮窗透明度", opacitySeekBar), ui.matchWidthWrapHeight());
        pageContent.addView(generalCard, ui.matchWidthWithBottomMargin(26));
        
        pageContent.addView(ui.sectionLabel("按前缀播放"), ui.matchWidthWithBottomMargin(12));
        LinearLayout prefixCard = ui.card();
        startingPrefix.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        startingPrefix.setHint("留空表示播放全部单词");
        prefixCard.addView(ui.settingItem("特定字母前缀", "设置要过滤的单词前缀", startingPrefix), ui.matchWidthWrapHeight());
        prefixCard.addView(ui.settingSwitchItem("循环播放", "开启：无限循环；关闭：播完一遍即停", loopPlaybackSwitch), ui.matchWidthWrapHeight());
        pageContent.addView(prefixCard, ui.matchWidthWithBottomMargin(26));

        pageContent.addView(ui.sectionLabel("外观"), ui.matchWidthWithBottomMargin(12));
        LinearLayout colorCard = ui.card();
        wordColor = ui.dropdown(PRESET_COLORS);
        typeColor = ui.dropdown(PRESET_COLORS);
        translationColor = ui.dropdown(PRESET_COLORS);
        phraseColor = ui.dropdown(PRESET_COLORS);
        colorCard.addView(ui.settingItem("单词颜色", "选择单词颜色", wordColor), ui.matchWidthWrapHeight());
        colorCard.addView(ui.settingItem("词性颜色", "选择词性颜色", typeColor), ui.matchWidthWrapHeight());
        colorCard.addView(ui.settingItem("释义颜色", "选择释义颜色", translationColor), ui.matchWidthWrapHeight());
        colorCard.addView(ui.settingItem("短语/例句颜色", "选择短语/例句颜色", phraseColor), ui.matchWidthWrapHeight());
        wordFontSize = ui.input(Integer.toString(settings.getWordFontSize()));
        detailFontSize = ui.input(Integer.toString(settings.getDetailFontSize()));
        resizeModeSwitch = new Switch(activity);
        
        colorCard.addView(ui.settingItem("单词字号", "悬浮窗单词字体大小", ui.numberAdjuster(wordFontSize, 2, 10, 72)), ui.matchWidthWrapHeight());
        colorCard.addView(ui.settingItem("释义字号", "悬浮窗释义字体大小", ui.numberAdjuster(detailFontSize, 2, 8, 60)), ui.matchWidthWrapHeight());
        
        autoSizeSwitch = new Switch(activity);
        colorCard.addView(ui.settingSwitchItem("自动适配内容大小", "悬浮窗尺寸随文字自动变化（恢复默认）", autoSizeSwitch), ui.matchWidthWrapHeight());
        
        colorCard.addView(ui.settingSwitchItem("调整悬浮窗大小", "开启后右下角出现可拖拽手柄", resizeModeSwitch), ui.matchWidthWrapHeight());
        
        pageContent.addView(colorCard, ui.matchWidthWithBottomMargin(26));

        pageContent.addView(ui.sectionLabel("挖空模式设置"), ui.matchWidthWithBottomMargin(12));
        LinearLayout fillBlankCard = ui.card();
        fillBlankInterval = ui.input(Integer.toString(settings.getFillBlankIntervalSeconds()));
        fillBlankInterval.setInputType(InputType.TYPE_CLASS_NUMBER);
        fillBlankHidePhrasesSwitch = new Switch(activity);
        fillBlankShowTranslationSwitch = new Switch(activity);
        fillBlankCard.addView(ui.settingItem("填充间隔", "每次自动填写一个空位的时间（秒）", fillBlankInterval), ui.matchWidthWrapHeight());
        fillBlankCard.addView(ui.settingSwitchItem("挖空时关闭短语", "挖空阶段不显示短语/例句", fillBlankHidePhrasesSwitch), ui.matchWidthWrapHeight());
        fillBlankCard.addView(ui.settingSwitchItem("挖空时显示释义", "挖空阶段显示词性和释义", fillBlankShowTranslationSwitch), ui.matchWidthWrapHeight());
        pageContent.addView(fillBlankCard, ui.matchWidthWithBottomMargin(26));



        bindSettings(settings);
        bindSettingsListeners();
    }

    private void bindSettings(AppSettings settings) {
        vocabularyDropdown.setText(settings.getVocabularyFileName(), false);
        displayModeDropdown.setText(settings.getDisplayMode().getLabel(), false);
        playbackModeDropdown.setText(settings.getPlaybackMode().getLabel(), false);
        loopPlaybackSwitch.setChecked(settings.isLoopPlayback());
        overlayModeDropdown.setText(settings.getOverlayMode().getLabel(), false);
        intervalSeconds.setText(Integer.toString(settings.getIntervalSeconds()));
        startingPrefix.setText(settings.getStartingPrefix());
        opacitySeekBar.setProgress((int) Math.round(settings.getOpacity() * 100) - 20);
        wordColor.setText(formatColor(settings.getWordColor()), false);
        typeColor.setText(formatColor(settings.getTypeColor()), false);
        translationColor.setText(formatColor(settings.getTranslationColor()), false);
        phraseColor.setText(formatColor(settings.getPhraseColor()), false);
        wordFontSize.setText(Integer.toString(settings.getWordFontSize()));
        detailFontSize.setText(Integer.toString(settings.getDetailFontSize()));
        
        isUpdatingSwitches = true;
        autoSizeSwitch.setChecked(settings.getWidth() <= 0 && settings.getHeight() <= 0 && !settings.isResizeMode());
        resizeModeSwitch.setChecked(settings.isResizeMode());
        isUpdatingSwitches = false;

        fillBlankInterval.setText(Integer.toString(settings.getFillBlankIntervalSeconds()));
        fillBlankHidePhrasesSwitch.setChecked(settings.isFillBlankHidePhrases());
        fillBlankShowTranslationSwitch.setChecked(settings.isFillBlankShowTranslation());
    }

    private void saveAndReload() {
        if (vocabularyDropdown == null) return;
        AppSettings settings = AndroidSettingsStore.load(activity);
        String previousVocabularyFileName = settings.getVocabularyFileName();
        PlaybackMode previousPlaybackMode = settings.getPlaybackMode();
        AndroidSettingsStore.savePlaybackProgress(activity, settings, previousVocabularyFileName);
        
        settings.setVocabularyFileName(ui.selectedValue(vocabularyDropdown, AndroidSettingsStore.VOCABULARY_FILES, AppSettings.DEFAULT_VOCABULARY_FILE_NAME));
        settings.setDisplayMode(DisplayMode.values()[ui.selectedIndex(displayModeDropdown, DisplayMode.labels())]);
        settings.setPlaybackMode(PlaybackMode.values()[ui.selectedIndex(playbackModeDropdown, PlaybackMode.labels())]);
        settings.setLoopPlayback(loopPlaybackSwitch.isChecked());
        settings.setOverlayMode(OverlayMode.values()[ui.selectedIndex(overlayModeDropdown, OverlayMode.labels())]);
        
        boolean vocabularyChanged = !previousVocabularyFileName.equals(settings.getVocabularyFileName());
        boolean playbackModeChanged = previousPlaybackMode != settings.getPlaybackMode();
        boolean prefixChanged = !startingPrefix.getText().toString().equals(settings.getStartingPrefix());
        
        settings.setStartingPrefix(startingPrefix.getText().toString());
        
        if (vocabularyChanged || prefixChanged) {
            settings.resetPlaybackProgress();
            AndroidSettingsStore.loadPlaybackProgress(activity, settings, settings.getVocabularyFileName());
        } else if (playbackModeChanged) {
            settings.resetPlaybackProgress();
        }
        try {
            settings.setIntervalSeconds(Integer.parseInt(intervalSeconds.getText().toString()));
        } catch (RuntimeException ignored) {
            settings.setIntervalSeconds(8);
        }
        settings.setOpacity((opacitySeekBar.getProgress() + 20) / 100.0);
        settings.setWordColor(extractHex(wordColor.getText().toString()));
        settings.setTypeColor(extractHex(typeColor.getText().toString()));
        settings.setTranslationColor(extractHex(translationColor.getText().toString()));
        settings.setPhraseColor(extractHex(phraseColor.getText().toString()));
        try { settings.setWordFontSize(Integer.parseInt(wordFontSize.getText().toString())); } catch (Exception ignored) {}
        try { settings.setDetailFontSize(Integer.parseInt(detailFontSize.getText().toString())); } catch (Exception ignored) {}
        
        if (autoSizeSwitch.isChecked()) {
            settings.setWidth(0);
            settings.setHeight(0);
        }
        settings.setResizeMode(resizeModeSwitch.isChecked());
        
        try { settings.setFillBlankIntervalSeconds(Integer.parseInt(fillBlankInterval.getText().toString())); } catch (Exception ignored) { settings.setFillBlankIntervalSeconds(3); }
        settings.setFillBlankHidePhrases(fillBlankHidePhrasesSwitch.isChecked());
        settings.setFillBlankShowTranslation(fillBlankShowTranslationSwitch.isChecked());

        AndroidSettingsStore.save(activity, settings);
        AndroidSettingsStore.savePlaybackProgress(activity, settings, settings.getVocabularyFileName());
        notifyServiceReload();
    }

    private void notifyServiceReload() {
        if (OverlayService.isRunning) {
            Intent intent = new Intent(activity, OverlayService.class);
            intent.setAction(OverlayService.ACTION_RELOAD);
            activity.startService(intent);
        }
    }

    private boolean isUpdatingSwitches = false;

    private void bindSettingsListeners() {
        AdapterView.OnItemClickListener dropdownListener = (parent, view, position, id) -> saveAndReload();
        vocabularyDropdown.setOnItemClickListener(dropdownListener);
        displayModeDropdown.setOnItemClickListener(dropdownListener);
        playbackModeDropdown.setOnItemClickListener(dropdownListener);
        overlayModeDropdown.setOnItemClickListener(dropdownListener);
        wordColor.setOnItemClickListener(dropdownListener);
        typeColor.setOnItemClickListener(dropdownListener);
        translationColor.setOnItemClickListener(dropdownListener);
        phraseColor.setOnItemClickListener(dropdownListener);

        TextWatcher textChangeListener = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { saveAndReload(); }
        };
        intervalSeconds.addTextChangedListener(textChangeListener);
        startingPrefix.addTextChangedListener(textChangeListener);
        wordColor.addTextChangedListener(textChangeListener);
        typeColor.addTextChangedListener(textChangeListener);
        translationColor.addTextChangedListener(textChangeListener);
        phraseColor.addTextChangedListener(textChangeListener);
        wordFontSize.addTextChangedListener(textChangeListener);
        detailFontSize.addTextChangedListener(textChangeListener);
        fillBlankInterval.addTextChangedListener(textChangeListener);
        
        autoSizeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitches) return;
            if (isChecked) {
                isUpdatingSwitches = true;
                resizeModeSwitch.setChecked(false);
                isUpdatingSwitches = false;
            }
            saveAndReload();
        });
        
        resizeModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitches) return;
            if (isChecked) {
                isUpdatingSwitches = true;
                autoSizeSwitch.setChecked(false);
                isUpdatingSwitches = false;
            }
            saveAndReload();
        });
        
        loopPlaybackSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveAndReload());
        fillBlankHidePhrasesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveAndReload());
        fillBlankShowTranslationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveAndReload());

        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser) saveAndReload(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

}

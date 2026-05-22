package me.englishhugging.android.ui.tabs;

import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

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
    private MaterialAutoCompleteTextView overlayModeDropdown;
    private EditText intervalSeconds;
    private EditText wordColor;
    private EditText typeColor;
    private EditText translationColor;
    private EditText phraseColor;
    private SeekBar opacitySeekBar;

    public SettingsTab(MainActivity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    public void buildContent(LinearLayout pageContent) {
        AppSettings settings = AndroidSettingsStore.load(activity);

        pageContent.addView(ui.headerRow("设置", ""), ui.matchWidthWithBottomMargin(28));

        pageContent.addView(ui.sectionLabel("基础设置"), ui.matchWidthWithBottomMargin(12));
        LinearLayout generalCard = ui.card();
        vocabularyDropdown = ui.dropdown(AndroidSettingsStore.VOCABULARY_FILES);
        displayModeDropdown = ui.dropdown(DisplayMode.labels());
        playbackModeDropdown = ui.dropdown(PlaybackMode.labels());
        overlayModeDropdown = ui.dropdown(OverlayMode.labels());
        intervalSeconds = ui.input(Integer.toString(settings.getIntervalSeconds()));
        intervalSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        opacitySeekBar = new SeekBar(activity);
        opacitySeekBar.setMax(80);
        generalCard.addView(ui.settingItem("词汇本", "选择要播放的词汇本", vocabularyDropdown), ui.matchWidthWrapHeight());
        generalCard.addView(ui.settingItem("显示内容", "悬浮窗展示哪些内容", displayModeDropdown), ui.matchWidthWrapHeight());
        generalCard.addView(ui.settingItem("播放顺序", "顺序、随机或随机不重复", playbackModeDropdown), ui.matchWidthWrapHeight());
        generalCard.addView(ui.settingItem("悬浮行为", "拖动、锁定或点击穿透", overlayModeDropdown), ui.matchWidthWrapHeight());
        generalCard.addView(ui.settingItem("切换间隔", "单位：秒", intervalSeconds), ui.matchWidthWrapHeight());
        generalCard.addView(ui.settingItem("透明度", "调整悬浮窗透明度", opacitySeekBar), ui.matchWidthWrapHeight());
        pageContent.addView(generalCard, ui.matchWidthWithBottomMargin(26));

        pageContent.addView(ui.sectionLabel("外观"), ui.matchWidthWithBottomMargin(12));
        LinearLayout colorCard = ui.card();
        wordColor = ui.input(settings.getWordColor());
        typeColor = ui.input(settings.getTypeColor());
        translationColor = ui.input(settings.getTranslationColor());
        phraseColor = ui.input(settings.getPhraseColor());
        colorCard.addView(ui.settingItem("单词颜色", "例如 #FFFFFF", wordColor), ui.matchWidthWrapHeight());
        colorCard.addView(ui.settingItem("词性颜色", "例如 #7DD3FC", typeColor), ui.matchWidthWrapHeight());
        colorCard.addView(ui.settingItem("释义颜色", "例如 #FDE68A", translationColor), ui.matchWidthWrapHeight());
        colorCard.addView(ui.settingItem("短语/例句颜色", "例如 #86EFAC", phraseColor), ui.matchWidthWrapHeight());
        pageContent.addView(colorCard, ui.matchWidthWithBottomMargin(26));

        pageContent.addView(ui.sectionLabel("自定义词汇"), ui.matchWidthWithBottomMargin(12));
        LinearLayout customCard = ui.card();
        EditText customWord = ui.input("");
        EditText customType = ui.input("");
        EditText customMeaning = ui.input("");
        EditText customPhrase = ui.input("");
        EditText customPhraseMeaning = ui.input("");
        EditText customExample = ui.input("");
        customCard.addView(ui.settingItem("单词", "必填", customWord), ui.matchWidthWrapHeight());
        customCard.addView(ui.settingItem("词性", "名词、动词等", customType), ui.matchWidthWrapHeight());
        customCard.addView(ui.settingItem("意思", "中文释义", customMeaning), ui.matchWidthWrapHeight());
        customCard.addView(ui.settingItem("词组", "可选", customPhrase), ui.matchWidthWrapHeight());
        customCard.addView(ui.settingItem("词组意思", "词组释义", customPhraseMeaning), ui.matchWidthWrapHeight());
        customCard.addView(ui.settingItem("例句", "可选", customExample), ui.matchWidthWrapHeight());
        MaterialButton addCustomWord = ui.secondaryButton("添加到自定义词汇");
        addCustomWord.setOnClickListener(view -> addCustomWord(customWord, customType, customMeaning, customPhrase, customPhraseMeaning, customExample));
        customCard.addView(addCustomWord, ui.matchWidthWithTopMargin(14));
        pageContent.addView(customCard, ui.matchWidthWithBottomMargin(16));

        bindSettings(settings);
        bindSettingsListeners();
    }

    private void bindSettings(AppSettings settings) {
        vocabularyDropdown.setText(settings.getVocabularyFileName(), false);
        displayModeDropdown.setText(settings.getDisplayMode().getLabel(), false);
        playbackModeDropdown.setText(settings.getPlaybackMode().getLabel(), false);
        overlayModeDropdown.setText(settings.getOverlayMode().getLabel(), false);
        intervalSeconds.setText(Integer.toString(settings.getIntervalSeconds()));
        opacitySeekBar.setProgress((int) Math.round(settings.getOpacity() * 100) - 20);
        wordColor.setText(settings.getWordColor());
        typeColor.setText(settings.getTypeColor());
        translationColor.setText(settings.getTranslationColor());
        phraseColor.setText(settings.getPhraseColor());
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
        settings.setOverlayMode(OverlayMode.values()[ui.selectedIndex(overlayModeDropdown, OverlayMode.labels())]);
        
        boolean vocabularyChanged = !previousVocabularyFileName.equals(settings.getVocabularyFileName());
        boolean playbackModeChanged = previousPlaybackMode != settings.getPlaybackMode();
        
        if (vocabularyChanged) {
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
        settings.setWordColor(wordColor.getText().toString());
        settings.setTypeColor(typeColor.getText().toString());
        settings.setTranslationColor(translationColor.getText().toString());
        settings.setPhraseColor(phraseColor.getText().toString());
        
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

    private void bindSettingsListeners() {
        AdapterView.OnItemClickListener dropdownListener = (parent, view, position, id) -> saveAndReload();
        vocabularyDropdown.setOnItemClickListener(dropdownListener);
        displayModeDropdown.setOnItemClickListener(dropdownListener);
        playbackModeDropdown.setOnItemClickListener(dropdownListener);
        overlayModeDropdown.setOnItemClickListener(dropdownListener);

        TextWatcher textChangeListener = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { saveAndReload(); }
        };
        intervalSeconds.addTextChangedListener(textChangeListener);
        wordColor.addTextChangedListener(textChangeListener);
        typeColor.addTextChangedListener(textChangeListener);
        translationColor.addTextChangedListener(textChangeListener);
        phraseColor.addTextChangedListener(textChangeListener);

        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser) saveAndReload(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void addCustomWord(
            EditText customWord, EditText customType, EditText customMeaning,
            EditText customPhrase, EditText customPhraseMeaning, EditText customExample
    ) {
        String word = customWord.getText().toString().trim();
        if (word.length() == 0) {
            Toast.makeText(activity, "请先填写单词", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = customType.getText().toString().trim();
        String meaning = customMeaning.getText().toString().trim();
        String phrase = customPhrase.getText().toString().trim();
        String phraseMeaning = customPhraseMeaning.getText().toString().trim();
        String example = customExample.getText().toString().trim();
        List<Translation> translations = meaning.length() == 0 && type.length() == 0
                ? Collections.emptyList()
                : Collections.singletonList(new Translation(meaning, type));
        List<Phrase> phrases = new ArrayList<>();
        if (phrase.length() > 0) {
            phrases.add(new Phrase(phrase, phraseMeaning));
        }
        if (example.length() > 0) {
            phrases.add(new Phrase(example, ""));
        }
        AndroidSettingsStore.appendCustomWord(activity, new WordEntry(word, translations, phrases));
        vocabularyDropdown.setText(AndroidSettingsStore.CUSTOM_VOCABULARY_FILE_NAME, false);
        Toast.makeText(activity, "已添加到自定义词汇", Toast.LENGTH_SHORT).show();
        customWord.setText("");
        customType.setText("");
        customMeaning.setText("");
        customPhrase.setText("");
        customPhraseMeaning.setText("");
        customExample.setText("");
    }
}

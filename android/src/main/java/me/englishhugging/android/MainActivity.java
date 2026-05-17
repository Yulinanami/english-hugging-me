package me.englishhugging.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.DisplayMode;
import me.englishhugging.core.settings.OverlayMode;
import me.englishhugging.core.model.Phrase;
import me.englishhugging.core.settings.PlaybackMode;
import me.englishhugging.core.model.Translation;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.android.overlay.OverlayService;

public final class MainActivity extends Activity {
    private AndroidUi ui;
    private LinearLayout pageContent;
    private MaterialButton homeTab;
    private MaterialButton settingsTab;
    private MaterialButton recordsTab;
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
    private TextView startCircle;
    private TextView connectedStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = new AndroidUi(this);
        styleSystemBars();
        requestNotificationPermissionIfNeeded();
        setContentView(createContentView());
        showHomePage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStartCircleState();
    }

    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AndroidUi.PAGE_BACKGROUND);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(AndroidUi.PAGE_BACKGROUND);
        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContent.setPadding(ui.dp(24), ui.getStatusBarHeight() + ui.dp(28), ui.dp(24), ui.dp(18));
        scrollView.addView(pageContent, ui.matchWidthWrapHeight());
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        LinearLayout navWrap = new LinearLayout(this);
        navWrap.setGravity(Gravity.CENTER);
        navWrap.setPadding(ui.dp(28), ui.dp(4), ui.dp(28), ui.dp(18));
        navWrap.addView(createBottomNavigation(), ui.matchWidthWrapHeight());
        root.addView(navWrap, ui.matchWidthWrapHeight());
        return root;
    }

    private LinearLayout createBottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4));
        nav.setBackground(ui.rounded(Color.rgb(243, 241, 248), Color.rgb(226, 224, 234), ui.dp(24)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            nav.setElevation(0);
            nav.setTranslationZ(0);
        }

        homeTab = ui.tabButton("首页");
        settingsTab = ui.tabButton("设置");
        recordsTab = ui.tabButton("记录");
        homeTab.setOnClickListener(view -> showHomePage());
        settingsTab.setOnClickListener(view -> showSettingsPage());
        recordsTab.setOnClickListener(view -> showRecordsPage());
        nav.addView(homeTab, ui.tabLayoutParams());
        nav.addView(recordsTab, ui.tabLayoutParams());
        return nav;
    }

    private void updateStartCircleState() {
        if (startCircle == null || connectedStatus == null) return;
        if (OverlayService.isRunning) {
            startCircle.setText("stop");
            startCircle.setBackground(ui.oval(AndroidUi.PRIMARY));
            connectedStatus.setText("悬浮背词运行中");
            connectedStatus.setTextColor(AndroidUi.PRIMARY);
        } else {
            startCircle.setText("play_arrow");
            startCircle.setBackground(ui.oval(AndroidUi.PRIMARY));
            connectedStatus.setText("点击启动悬浮背词");
            connectedStatus.setTextColor(AndroidUi.PRIMARY);
        }
    }

    private void switchPage(MaterialButton tab, Runnable buildContent) {
        if (pageContent.getChildCount() > 0) {
            pageContent.animate().alpha(0f).translationY(ui.dp(10)).setDuration(150).withEndAction(() -> {
                clearSettingsFields();
                selectTab(tab);
                pageContent.removeAllViews();
                buildContent.run();
                pageContent.setTranslationY(ui.dp(-10));
                pageContent.animate().alpha(1f).translationY(0).setDuration(150).start();
            }).start();
        } else {
            clearSettingsFields();
            selectTab(tab);
            pageContent.removeAllViews();
            buildContent.run();
            pageContent.setAlpha(0f);
            pageContent.setTranslationY(ui.dp(10));
            pageContent.animate().alpha(1f).translationY(0).setDuration(300).start();
        }
    }

    private void showHomePage() {
        switchPage(homeTab, () -> {
            AppSettings settings = AndroidSettingsStore.load(this);
            LinearLayout header = ui.headerRow("首页", "设置");
            header.getChildAt(1).setOnClickListener(view -> showSettingsPage());
            pageContent.addView(header, ui.matchWidthWithBottomMargin(34));

            LinearLayout speedCard = ui.card();
            speedCard.setOrientation(LinearLayout.HORIZONTAL);
            speedCard.setGravity(Gravity.CENTER);
            speedCard.addView(ui.homeMetric("词汇本", settings.getVocabularyFileName()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            View divider = new View(this);
            divider.setBackgroundColor(Color.rgb(199, 197, 209));
            speedCard.addView(divider, new LinearLayout.LayoutParams(ui.dp(1), ui.dp(54)));
            speedCard.addView(ui.homeMetric("间隔", settings.getIntervalSeconds() + " 秒"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            pageContent.addView(speedCard, ui.matchWidthWithBottomMargin(78));

            startCircle = new TextView(this);
            startCircle.setTextSize(64);
            startCircle.setTypeface(ui.getIconFont());
            startCircle.setTextColor(Color.WHITE);
            startCircle.setGravity(Gravity.CENTER);
            startCircle.setOnClickListener(view -> {
                view.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    if (OverlayService.isRunning) {
                        stopService(new Intent(this, OverlayService.class));
                        startCircle.postDelayed(this::updateStartCircleState, 200);
                    } else {
                        startOverlay();
                        startCircle.postDelayed(this::updateStartCircleState, 500);
                    }
                }).start();
            });
            LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(ui.dp(140), ui.dp(140));
            circleParams.gravity = Gravity.CENTER_HORIZONTAL;
            pageContent.addView(startCircle, circleParams);

            connectedStatus = ui.titleText("点击启动悬浮背词");
            connectedStatus.setTextColor(AndroidUi.PRIMARY);
            connectedStatus.setGravity(Gravity.CENTER);
            pageContent.addView(connectedStatus, ui.matchWidthWithBottomMargin(72));

            updateStartCircleState();
        });
    }

    private void showSettingsPage() {
        switchPage(settingsTab, () -> {
            AppSettings settings = AndroidSettingsStore.load(this);

            pageContent.addView(ui.headerRow("设置", ""), ui.matchWidthWithBottomMargin(28));

            pageContent.addView(ui.sectionLabel("基础设置"), ui.matchWidthWithBottomMargin(12));
            LinearLayout generalCard = ui.card();
            vocabularyDropdown = ui.dropdown(AndroidSettingsStore.VOCABULARY_FILES);
            displayModeDropdown = ui.dropdown(DisplayMode.labels());
            playbackModeDropdown = ui.dropdown(PlaybackMode.labels());
            overlayModeDropdown = ui.dropdown(OverlayMode.labels());
            intervalSeconds = ui.input(Integer.toString(settings.getIntervalSeconds()));
            intervalSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
            opacitySeekBar = new SeekBar(this);
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
            MaterialButton save = ui.primaryButton("保存设置");
            save.setOnClickListener(view -> saveSettingsOnly());
            colorCard.addView(save, ui.matchWidthWithTopMargin(14));
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
        });
    }

    private void showRecordsPage() {
        switchPage(recordsTab, () -> {
            pageContent.addView(ui.headerRow("播放记录", ""), ui.matchWidthWithBottomMargin(28));

            pageContent.addView(ui.sectionLabel("记录"), ui.matchWidthWithBottomMargin(12));
            LinearLayout recordsCard = ui.card();
            for (String line : AndroidSettingsStore.playbackRecordLines(this)) {
                recordsCard.addView(ui.recordRow(line), ui.matchWidthWrapHeight());
            }
            pageContent.addView(recordsCard, ui.matchWidthWithBottomMargin(16));
        });
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

    private void startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            Toast.makeText(this, "请先允许悬浮窗权限", Toast.LENGTH_LONG).show();
            return;
        }

        AppSettings settings = vocabularyDropdown == null ? AndroidSettingsStore.load(this) : collectSettings();
        if (vocabularyDropdown == null) {
            AndroidSettingsStore.loadPlaybackProgress(this, settings, settings.getVocabularyFileName());
        }
        AndroidSettingsStore.save(this, settings);
        AndroidSettingsStore.savePlaybackProgress(this, settings, settings.getVocabularyFileName());
        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction(OverlayService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void saveSettingsOnly() {
        AppSettings settings = collectSettings();
        AndroidSettingsStore.save(this, settings);
        AndroidSettingsStore.savePlaybackProgress(this, settings, settings.getVocabularyFileName());
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
    }

    private AppSettings collectSettings() {
        AppSettings settings = AndroidSettingsStore.load(this);
        String previousVocabularyFileName = settings.getVocabularyFileName();
        PlaybackMode previousPlaybackMode = settings.getPlaybackMode();
        AndroidSettingsStore.savePlaybackProgress(this, settings, previousVocabularyFileName);
        settings.setVocabularyFileName(ui.selectedValue(vocabularyDropdown, AndroidSettingsStore.VOCABULARY_FILES, AppSettings.DEFAULT_VOCABULARY_FILE_NAME));
        settings.setDisplayMode(DisplayMode.values()[ui.selectedIndex(displayModeDropdown, DisplayMode.labels())]);
        settings.setPlaybackMode(PlaybackMode.values()[ui.selectedIndex(playbackModeDropdown, PlaybackMode.labels())]);
        settings.setOverlayMode(OverlayMode.values()[ui.selectedIndex(overlayModeDropdown, OverlayMode.labels())]);
        boolean vocabularyChanged = !previousVocabularyFileName.equals(settings.getVocabularyFileName());
        boolean playbackModeChanged = previousPlaybackMode != settings.getPlaybackMode();
        if (vocabularyChanged) {
            settings.resetPlaybackProgress();
            AndroidSettingsStore.loadPlaybackProgress(this, settings, settings.getVocabularyFileName());
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
        return settings;
    }

    private void addCustomWord(
            EditText customWord,
            EditText customType,
            EditText customMeaning,
            EditText customPhrase,
            EditText customPhraseMeaning,
            EditText customExample
    ) {
        String word = customWord.getText().toString().trim();
        if (word.length() == 0) {
            Toast.makeText(this, "请先填写单词", Toast.LENGTH_SHORT).show();
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
        AndroidSettingsStore.appendCustomWord(this, new WordEntry(word, translations, phrases));
        vocabularyDropdown.setText(AndroidSettingsStore.CUSTOM_VOCABULARY_FILE_NAME, false);
        Toast.makeText(this, "已添加到自定义词汇", Toast.LENGTH_SHORT).show();
        customWord.setText("");
        customType.setText("");
        customMeaning.setText("");
        customPhrase.setText("");
        customPhraseMeaning.setText("");
        customExample.setText("");
    }

    private void clearSettingsFields() {
        vocabularyDropdown = null;
        displayModeDropdown = null;
        playbackModeDropdown = null;
        overlayModeDropdown = null;
        intervalSeconds = null;
        wordColor = null;
        typeColor = null;
        translationColor = null;
        phraseColor = null;
        opacitySeekBar = null;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void styleSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(AndroidUi.PAGE_BACKGROUND);
            getWindow().setNavigationBarColor(AndroidUi.PAGE_BACKGROUND);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void selectTab(MaterialButton selected) {
        ui.styleTab(homeTab, selected == homeTab);
        ui.styleTab(settingsTab, selected == settingsTab);
        ui.styleTab(recordsTab, selected == recordsTab);
    }
}


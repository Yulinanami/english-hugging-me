package me.englishhugging.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.englishhugging.core.AppSettings;
import me.englishhugging.core.DisplayMode;
import me.englishhugging.core.OverlayMode;
import me.englishhugging.core.Phrase;
import me.englishhugging.core.PlaybackMode;
import me.englishhugging.core.Translation;
import me.englishhugging.core.WordEntry;

public final class MainActivity extends Activity {
    private LinearLayout content;
    private Button homeTab;
    private Button settingsTab;
    private Button recordsTab;
    private Spinner vocabularySpinner;
    private Spinner displayModeSpinner;
    private Spinner playbackModeSpinner;
    private Spinner overlayModeSpinner;
    private EditText intervalSeconds;
    private EditText wordColor;
    private EditText typeColor;
    private EditText translationColor;
    private EditText phraseColor;
    private SeekBar opacitySeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        styleSystemBars();
        requestNotificationPermissionIfNeeded();
        setContentView(createContentView());
        showHomePage();
    }

    private ScrollView createContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.parseColor("#F6F8FC"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), getStatusBarHeight() + dp(18), dp(20), dp(24));
        scrollView.addView(root, matchWidthWrapHeight());

        TextView title = new TextView(this);
        title.setText("English Hugging Me");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#172033"));
        root.addView(title, matchWidthWrapHeight());

        TextView subtitle = new TextView(this);
        subtitle.setText("移动端悬浮背词");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.parseColor("#667085"));
        subtitle.setPadding(0, dp(4), 0, dp(16));
        root.addView(subtitle, matchWidthWrapHeight());

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(0, 0, 0, dp(14));
        root.addView(tabs, matchWidthWrapHeight());

        homeTab = tabButton("首页");
        settingsTab = tabButton("设置");
        recordsTab = tabButton("播放记录");
        tabs.addView(homeTab, tabLayoutParams());
        tabs.addView(settingsTab, tabLayoutParams());
        tabs.addView(recordsTab, tabLayoutParams());

        homeTab.setOnClickListener(view -> showHomePage());
        settingsTab.setOnClickListener(view -> showSettingsPage());
        recordsTab.setOnClickListener(view -> showRecordsPage());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, matchWidthWrapHeight());
        return scrollView;
    }

    private void showHomePage() {
        selectTab(homeTab);
        content.removeAllViews();

        AppSettings settings = AndroidSettingsStore.load(this);
        LinearLayout statusCard = card();
        TextView headline = titleText("当前词汇本");
        TextView currentVocabulary = bodyText(settings.getVocabularyFileName());
        TextView note = bodyText("启动后词汇会悬浮在其它 App 上方；关闭设置页不会影响悬浮窗。最上方已预留状态栏空间，不会和系统状态栏堆叠。");
        statusCard.addView(headline, matchWidthWrapHeight());
        statusCard.addView(currentVocabulary, matchWidthWrapHeight());
        statusCard.addView(note, matchWidthWrapHeight());
        content.addView(statusCard, matchWidthWrapHeightWithBottomMargin());

        LinearLayout actions = card();
        Button start = primaryButton("启动悬浮背词");
        start.setOnClickListener(view -> startOverlay());
        actions.addView(start, matchWidthWrapHeight());

        Button stop = secondaryButton("停止悬浮背词");
        stop.setOnClickListener(view -> stopService(new Intent(this, OverlayService.class)));
        actions.addView(stop, matchWidthWrapHeight());
        content.addView(actions, matchWidthWrapHeightWithBottomMargin());
    }

    private void showSettingsPage() {
        selectTab(settingsTab);
        content.removeAllViews();
        AppSettings settings = AndroidSettingsStore.load(this);

        LinearLayout settingsCard = card();
        settingsCard.addView(titleText("基础设置"), matchWidthWrapHeight());
        vocabularySpinner = addSpinner(settingsCard, "词汇本", AndroidSettingsStore.VOCABULARY_FILES);
        displayModeSpinner = addSpinner(settingsCard, "显示内容", displayModeLabels());
        playbackModeSpinner = addSpinner(settingsCard, "播放顺序", playbackModeLabels());
        overlayModeSpinner = addSpinner(settingsCard, "悬浮行为", overlayModeLabels());

        settingsCard.addView(label("切换间隔（秒）"));
        intervalSeconds = input(Integer.toString(settings.getIntervalSeconds()));
        intervalSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        settingsCard.addView(intervalSeconds, matchWidthWrapHeight());

        settingsCard.addView(label("透明度"));
        opacitySeekBar = new SeekBar(this);
        opacitySeekBar.setMax(80);
        settingsCard.addView(opacitySeekBar, matchWidthWrapHeight());
        content.addView(settingsCard, matchWidthWrapHeightWithBottomMargin());

        LinearLayout colorCard = card();
        colorCard.addView(titleText("自定义颜色"), matchWidthWrapHeight());
        wordColor = colorInput(colorCard, "单词颜色", settings.getWordColor());
        typeColor = colorInput(colorCard, "词性颜色", settings.getTypeColor());
        translationColor = colorInput(colorCard, "释义颜色", settings.getTranslationColor());
        phraseColor = colorInput(colorCard, "短语/例句颜色", settings.getPhraseColor());
        Button save = primaryButton("保存设置");
        save.setOnClickListener(view -> saveSettingsOnly());
        colorCard.addView(save, matchWidthWrapHeight());
        content.addView(colorCard, matchWidthWrapHeightWithBottomMargin());

        LinearLayout customCard = card();
        customCard.addView(titleText("自定义词汇"), matchWidthWrapHeight());
        EditText customWord = labeledInput(customCard, "单词", "");
        EditText customType = labeledInput(customCard, "词性", "");
        EditText customPhrase = labeledInput(customCard, "词组", "");
        EditText customExample = labeledInput(customCard, "例句", "");
        EditText customMeaning = labeledInput(customCard, "意思", "");
        Button addCustomWord = secondaryButton("添加到自定义词汇");
        addCustomWord.setOnClickListener(view -> addCustomWord(customWord, customType, customPhrase, customExample, customMeaning));
        customCard.addView(addCustomWord, matchWidthWrapHeight());
        content.addView(customCard, matchWidthWrapHeightWithBottomMargin());

        bindSettings(settings);
    }

    private void showRecordsPage() {
        selectTab(recordsTab);
        content.removeAllViews();

        LinearLayout recordsCard = card();
        recordsCard.addView(titleText("播放记录"), matchWidthWrapHeight());
        TextView note = bodyText("记录各个词汇本顺序播放到哪里，以及随机播放了多少个单词。");
        recordsCard.addView(note, matchWidthWrapHeight());
        for (String line : AndroidSettingsStore.playbackRecordLines(this)) {
            TextView item = bodyText(line);
            item.setPadding(0, dp(10), 0, 0);
            recordsCard.addView(item, matchWidthWrapHeight());
        }
        content.addView(recordsCard, matchWidthWrapHeightWithBottomMargin());
    }

    private Spinner addSpinner(LinearLayout root, String label, String[] values) {
        root.addView(label(label));
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        root.addView(spinner, matchWidthWrapHeight());
        return spinner;
    }

    private EditText colorInput(LinearLayout root, String label, String value) {
        EditText input = labeledInput(root, label, value);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        return input;
    }

    private EditText labeledInput(LinearLayout root, String label, String value) {
        root.addView(label(label));
        EditText input = input(value);
        root.addView(input, matchWidthWrapHeight());
        return input;
    }

    private EditText input(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setSingleLine(true);
        input.setTextColor(Color.parseColor("#172033"));
        input.setTextSize(15);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackground(rounded(Color.WHITE, Color.parseColor("#D0D5DD"), dp(12)));
        return input;
    }

    private TextView label(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(14);
        label.setTextColor(Color.parseColor("#475467"));
        label.setPadding(0, dp(14), 0, dp(6));
        return label;
    }

    private TextView titleText(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#172033"));
        title.setPadding(0, 0, 0, dp(8));
        return title;
    }

    private TextView bodyText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(14);
        textView.setTextColor(Color.parseColor("#475467"));
        textView.setLineSpacing(dp(2), 1.0f);
        return textView;
    }

    private void bindSettings(AppSettings settings) {
        vocabularySpinner.setSelection(AndroidSettingsStore.vocabularyIndex(settings.getVocabularyFileName()));
        displayModeSpinner.setSelection(settings.getDisplayMode().ordinal());
        playbackModeSpinner.setSelection(settings.getPlaybackMode().ordinal());
        overlayModeSpinner.setSelection(settings.getOverlayMode().ordinal());
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

        AppSettings settings = vocabularySpinner == null ? AndroidSettingsStore.load(this) : collectSettings();
        if (vocabularySpinner == null) {
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
        settings.setVocabularyFileName(AndroidSettingsStore.VOCABULARY_FILES[vocabularySpinner.getSelectedItemPosition()]);
        settings.setDisplayMode(DisplayMode.values()[displayModeSpinner.getSelectedItemPosition()]);
        settings.setPlaybackMode(PlaybackMode.values()[playbackModeSpinner.getSelectedItemPosition()]);
        settings.setOverlayMode(OverlayMode.values()[overlayModeSpinner.getSelectedItemPosition()]);
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
            EditText customPhrase,
            EditText customExample,
            EditText customMeaning
    ) {
        String word = customWord.getText().toString().trim();
        if (word.length() == 0) {
            Toast.makeText(this, "请先填写单词", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = customType.getText().toString().trim();
        String phrase = customPhrase.getText().toString().trim();
        String example = customExample.getText().toString().trim();
        String meaning = customMeaning.getText().toString().trim();
        List<Translation> translations = meaning.length() == 0 && type.length() == 0
                ? Collections.emptyList()
                : Collections.singletonList(new Translation(meaning, type));
        List<Phrase> phrases = new ArrayList<>();
        if (phrase.length() > 0) {
            phrases.add(new Phrase(phrase, ""));
        }
        if (example.length() > 0) {
            phrases.add(new Phrase(example, meaning));
        }
        AndroidSettingsStore.appendCustomWord(this, new WordEntry(word, translations, phrases));
        vocabularySpinner.setSelection(AndroidSettingsStore.vocabularyIndex(AndroidSettingsStore.CUSTOM_VOCABULARY_FILE_NAME));
        Toast.makeText(this, "已添加到自定义词汇", Toast.LENGTH_SHORT).show();
        customWord.setText("");
        customType.setText("");
        customPhrase.setText("");
        customExample.setText("");
        customMeaning.setText("");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void styleSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#F6F8FC"));
            getWindow().setNavigationBarColor(Color.WHITE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private Button tabButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        return button;
    }

    private void selectTab(Button selected) {
        styleTab(homeTab, selected == homeTab);
        styleTab(settingsTab, selected == settingsTab);
        styleTab(recordsTab, selected == recordsTab);
    }

    private void styleTab(Button button, boolean selected) {
        button.setAllCaps(false);
        button.setTextColor(selected ? Color.WHITE : Color.parseColor("#344054"));
        button.setTextSize(14);
        button.setBackground(rounded(selected ? Color.parseColor("#2F6FED") : Color.WHITE, Color.TRANSPARENT, dp(20)));
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setBackground(rounded(Color.parseColor("#2F6FED"), Color.TRANSPARENT, dp(14)));
        button.setPadding(0, dp(8), 0, dp(8));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.parseColor("#2F6FED"));
        button.setTextSize(15);
        button.setBackground(rounded(Color.parseColor("#EEF4FF"), Color.TRANSPARENT, dp(14)));
        button.setPadding(0, dp(8), 0, dp(8));
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(Color.WHITE, Color.TRANSPARENT, dp(18)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        return card;
    }

    private GradientDrawable rounded(int color, int strokeColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams tabLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private ViewGroup.LayoutParams matchWidthWrapHeight() {
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams matchWidthWrapHeightWithBottomMargin() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));
        return params;
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return dp(24);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String[] displayModeLabels() {
        return new String[]{"只显示单词", "单词 + 释义", "单词 + 释义 + 短语"};
    }

    private static String[] playbackModeLabels() {
        return new String[]{"顺序播放", "随机播放", "随机不重复"};
    }

    private static String[] overlayModeLabels() {
        return new String[]{"可拖动", "锁定位置", "点击穿透"};
    }
}

package me.englishhugging.android;

import android.Manifest;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.Filter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final int PAGE_BACKGROUND = Color.rgb(250, 248, 255);
    private static final int CARD_BACKGROUND = Color.rgb(246, 244, 251);
    private static final int PRIMARY = Color.rgb(82, 105, 154);
    private static final int TEXT_PRIMARY = Color.rgb(39, 43, 54);
    private static final int TEXT_SECONDARY = Color.rgb(95, 96, 110);

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        styleSystemBars();
        requestNotificationPermissionIfNeeded();
        setContentView(createContentView());
        showHomePage();
    }

    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAGE_BACKGROUND);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(PAGE_BACKGROUND);
        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContent.setPadding(dp(24), getStatusBarHeight() + dp(28), dp(24), dp(18));
        scrollView.addView(pageContent, matchWidthWrapHeight());
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        LinearLayout navWrap = new LinearLayout(this);
        navWrap.setGravity(Gravity.CENTER);
        navWrap.setPadding(dp(28), dp(4), dp(28), dp(18));
        navWrap.addView(createBottomNavigation(), matchWidthWrapHeight());
        root.addView(navWrap, matchWidthWrapHeight());
        return root;
    }

    private LinearLayout createBottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(4), dp(4), dp(4), dp(4));
        nav.setBackground(rounded(Color.rgb(243, 241, 248), Color.rgb(226, 224, 234), dp(24)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            nav.setElevation(0);
            nav.setTranslationZ(0);
        }

        homeTab = tabButton("首页");
        settingsTab = tabButton("设置");
        recordsTab = tabButton("记录");
        homeTab.setOnClickListener(view -> showHomePage());
        settingsTab.setOnClickListener(view -> showSettingsPage());
        recordsTab.setOnClickListener(view -> showRecordsPage());
        nav.addView(homeTab, tabLayoutParams());
        nav.addView(recordsTab, tabLayoutParams());
        return nav;
    }

    private void showHomePage() {
        clearSettingsFields();
        selectTab(homeTab);
        pageContent.removeAllViews();

        AppSettings settings = AndroidSettingsStore.load(this);
        LinearLayout header = headerRow("首页", "设置");
        header.getChildAt(1).setOnClickListener(view -> showSettingsPage());
        pageContent.addView(header, matchWidthWithBottomMargin(34));

        LinearLayout speedCard = card();
        speedCard.setOrientation(LinearLayout.HORIZONTAL);
        speedCard.setGravity(Gravity.CENTER);
        speedCard.addView(homeMetric("词汇本", settings.getVocabularyFileName()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(199, 197, 209));
        speedCard.addView(divider, new LinearLayout.LayoutParams(dp(1), dp(54)));
        speedCard.addView(homeMetric("间隔", settings.getIntervalSeconds() + " 秒"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        pageContent.addView(speedCard, matchWidthWithBottomMargin(78));

        TextView startCircle = new TextView(this);
        startCircle.setText("✓");
        startCircle.setTextSize(82);
        startCircle.setTextColor(Color.WHITE);
        startCircle.setGravity(Gravity.CENTER);
        startCircle.setBackground(oval(PRIMARY));
        startCircle.setOnClickListener(view -> startOverlay());
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(dp(190), dp(190));
        circleParams.gravity = Gravity.CENTER_HORIZONTAL;
        pageContent.addView(startCircle, circleParams);

        TextView connected = titleText("点击启动悬浮背词");
        connected.setTextColor(PRIMARY);
        connected.setGravity(Gravity.CENTER);
        pageContent.addView(connected, matchWidthWithBottomMargin(72));

        TextView detailTitle = titleText("当前配置");
        detailTitle.setTextSize(20);
        pageContent.addView(detailTitle, matchWidthWithBottomMargin(18));

        LinearLayout detailCard = card();
        detailCard.setOrientation(LinearLayout.HORIZONTAL);
        detailCard.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = circularIcon("★", Color.rgb(226, 226, 229), Color.WHITE);
        detailCard.addView(icon, new LinearLayout.LayoutParams(dp(68), dp(68)));
        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setPadding(dp(18), 0, 0, 0);
        textColumn.addView(titleText(settings.getVocabularyFileName()), matchWidthWrapHeight());
        textColumn.addView(bodyText(playbackModeLabels()[settings.getPlaybackMode().ordinal()]), matchWidthWrapHeight());
        detailCard.addView(textColumn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        MaterialButton stop = iconButton("■");
        stop.setOnClickListener(view -> stopService(new Intent(this, OverlayService.class)));
        detailCard.addView(stop, new LinearLayout.LayoutParams(dp(54), dp(54)));
        pageContent.addView(detailCard, matchWidthWithBottomMargin(16));
    }

    private void showSettingsPage() {
        selectTab(settingsTab);
        pageContent.removeAllViews();
        AppSettings settings = AndroidSettingsStore.load(this);

        pageContent.addView(headerRow("设置", ""), matchWidthWithBottomMargin(28));

        pageContent.addView(sectionLabel("基础设置"), matchWidthWithBottomMargin(12));
        LinearLayout generalCard = card();
        vocabularyDropdown = dropdown(AndroidSettingsStore.VOCABULARY_FILES);
        displayModeDropdown = dropdown(displayModeLabels());
        playbackModeDropdown = dropdown(playbackModeLabels());
        overlayModeDropdown = dropdown(overlayModeLabels());
        intervalSeconds = input(Integer.toString(settings.getIntervalSeconds()));
        intervalSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        opacitySeekBar = new SeekBar(this);
        opacitySeekBar.setMax(80);
        generalCard.addView(settingItem("词汇本", "选择要播放的词汇本", vocabularyDropdown), matchWidthWrapHeight());
        generalCard.addView(settingItem("显示内容", "悬浮窗展示哪些内容", displayModeDropdown), matchWidthWrapHeight());
        generalCard.addView(settingItem("播放顺序", "顺序、随机或随机不重复", playbackModeDropdown), matchWidthWrapHeight());
        generalCard.addView(settingItem("悬浮行为", "拖动、锁定或点击穿透", overlayModeDropdown), matchWidthWrapHeight());
        generalCard.addView(settingItem("切换间隔", "单位：秒", intervalSeconds), matchWidthWrapHeight());
        generalCard.addView(settingItem("透明度", "调整悬浮窗透明度", opacitySeekBar), matchWidthWrapHeight());
        pageContent.addView(generalCard, matchWidthWithBottomMargin(26));

        pageContent.addView(sectionLabel("外观"), matchWidthWithBottomMargin(12));
        LinearLayout colorCard = card();
        wordColor = input(settings.getWordColor());
        typeColor = input(settings.getTypeColor());
        translationColor = input(settings.getTranslationColor());
        phraseColor = input(settings.getPhraseColor());
        colorCard.addView(settingItem("单词颜色", "例如 #FFFFFF", wordColor), matchWidthWrapHeight());
        colorCard.addView(settingItem("词性颜色", "例如 #7DD3FC", typeColor), matchWidthWrapHeight());
        colorCard.addView(settingItem("释义颜色", "例如 #FDE68A", translationColor), matchWidthWrapHeight());
        colorCard.addView(settingItem("短语/例句颜色", "例如 #86EFAC", phraseColor), matchWidthWrapHeight());
        MaterialButton save = primaryButton("保存设置");
        save.setOnClickListener(view -> saveSettingsOnly());
        colorCard.addView(save, matchWidthWithTopMargin(14));
        pageContent.addView(colorCard, matchWidthWithBottomMargin(26));

        pageContent.addView(sectionLabel("自定义词汇"), matchWidthWithBottomMargin(12));
        LinearLayout customCard = card();
        EditText customWord = input("");
        EditText customType = input("");
        EditText customPhrase = input("");
        EditText customExample = input("");
        EditText customMeaning = input("");
        customCard.addView(settingItem("单词", "必填", customWord), matchWidthWrapHeight());
        customCard.addView(settingItem("词性", "名词、动词等", customType), matchWidthWrapHeight());
        customCard.addView(settingItem("词组", "可选", customPhrase), matchWidthWrapHeight());
        customCard.addView(settingItem("例句", "可选", customExample), matchWidthWrapHeight());
        customCard.addView(settingItem("意思", "中文释义", customMeaning), matchWidthWrapHeight());
        MaterialButton addCustomWord = secondaryButton("添加到自定义词汇");
        addCustomWord.setOnClickListener(view -> addCustomWord(customWord, customType, customPhrase, customExample, customMeaning));
        customCard.addView(addCustomWord, matchWidthWithTopMargin(14));
        pageContent.addView(customCard, matchWidthWithBottomMargin(16));

        bindSettings(settings);
    }

    private void showRecordsPage() {
        clearSettingsFields();
        selectTab(recordsTab);
        pageContent.removeAllViews();
        pageContent.addView(headerRow("播放记录", ""), matchWidthWithBottomMargin(28));

        pageContent.addView(sectionLabel("记录"), matchWidthWithBottomMargin(12));
        LinearLayout recordsCard = card();
        for (String line : AndroidSettingsStore.playbackRecordLines(this)) {
            recordsCard.addView(recordRow(line), matchWidthWrapHeight());
        }
        pageContent.addView(recordsCard, matchWidthWithBottomMargin(16));
    }

    private LinearLayout headerRow(String title, String iconText) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(34);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(TEXT_PRIMARY);
        header.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (iconText.length() > 0) {
            MaterialButton icon = smallActionButton(iconText);
            header.addView(icon, new LinearLayout.LayoutParams(dp(86), dp(46)));
        }
        return header;
    }

    private LinearLayout homeMetric(String label, String value) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        TextView labelView = bodyText(label);
        labelView.setGravity(Gravity.CENTER);
        TextView valueView = titleText(value);
        valueView.setGravity(Gravity.CENTER);
        column.addView(labelView, matchWidthWrapHeight());
        column.addView(valueView, matchWidthWrapHeight());
        return column;
    }

    private View settingItem(String title, String subtitle, View control) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, dp(10), 0, dp(14));

        TextView titleView = itemTitleText(title);
        TextView subtitleView = itemSubtitleText(subtitle);
        LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        controlParams.setMargins(0, dp(10), 0, 0);

        item.addView(titleView, matchWidthWrapHeight());
        item.addView(subtitleView, matchWidthWrapHeight());
        item.addView(control, controlParams);
        return item;
    }

    private View recordRow(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.addView(circularIcon("▶", Color.rgb(232, 232, 238), PRIMARY), new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView value = bodyText(text);
        value.setTextSize(15);
        value.setPadding(dp(14), 0, 0, 0);
        row.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private MaterialAutoCompleteTextView dropdown(String[] values) {
        MaterialAutoCompleteTextView dropdown = new MaterialAutoCompleteTextView(this);
        ArrayAdapter<String> adapter = dropdownAdapter(values);
        dropdown.setAdapter(adapter);
        dropdown.setText(values.length == 0 ? "" : values[0], false);
        dropdown.setThreshold(0);
        dropdown.setInputType(InputType.TYPE_NULL);
        dropdown.setSingleLine(true);
        dropdown.setTextSize(14);
        dropdown.setTextColor(TEXT_PRIMARY);
        dropdown.setHintTextColor(TEXT_SECONDARY);
        dropdown.setPadding(dp(12), dp(9), dp(12), dp(9));
        dropdown.setBackground(rounded(Color.WHITE, Color.rgb(218, 216, 226), dp(14)));
        dropdown.setDropDownBackgroundDrawable(rounded(Color.WHITE, Color.TRANSPARENT, dp(14)));
        dropdown.setDropDownHeight(Math.min(dp(260), Math.max(dp(48), values.length * dp(54))));
        dropdown.setOnClickListener(view -> {
            adapter.getFilter().filter(null);
            dropdown.showDropDown();
        });
        dropdown.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                adapter.getFilter().filter(null);
                dropdown.showDropDown();
            }
        });
        return dropdown;
    }

    private ArrayAdapter<String> dropdownAdapter(String[] values) {
        List<String> items = new ArrayList<>(Arrays.asList(values));
        return new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return dropdownTextView(super.getView(position, convertView, parent));
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return dropdownTextView(super.getDropDownView(position, convertView, parent));
            }

            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        results.values = values;
                        results.count = values.length;
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        clear();
                        addAll(values);
                        notifyDataSetChanged();
                    }
                };
            }
        };
    }

    private View dropdownTextView(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setTextColor(TEXT_PRIMARY);
            textView.setTextSize(15);
            textView.setPadding(dp(16), dp(12), dp(16), dp(12));
        }
        return view;
    }

    private EditText input(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setSingleLine(true);
        input.setTextColor(TEXT_PRIMARY);
        input.setTextSize(15);
        input.setPadding(dp(10), dp(6), dp(10), dp(6));
        input.setBackground(rounded(Color.WHITE, Color.rgb(218, 216, 226), dp(12)));
        return input;
    }

    private TextView sectionLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(18);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(PRIMARY);
        label.setPadding(dp(8), 0, 0, 0);
        return label;
    }

    private TextView titleText(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT_PRIMARY);
        return title;
    }

    private TextView itemTitleText(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT_PRIMARY);
        return title;
    }

    private TextView itemSubtitleText(String text) {
        TextView subtitle = new TextView(this);
        subtitle.setText(text);
        subtitle.setTextSize(13);
        subtitle.setTextColor(TEXT_SECONDARY);
        return subtitle;
    }

    private TextView bodyText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(TEXT_SECONDARY);
        textView.setLineSpacing(dp(2), 1.0f);
        return textView;
    }

    private void bindSettings(AppSettings settings) {
        vocabularyDropdown.setText(settings.getVocabularyFileName(), false);
        displayModeDropdown.setText(displayModeLabels()[settings.getDisplayMode().ordinal()], false);
        playbackModeDropdown.setText(playbackModeLabels()[settings.getPlaybackMode().ordinal()], false);
        overlayModeDropdown.setText(overlayModeLabels()[settings.getOverlayMode().ordinal()], false);
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
        settings.setVocabularyFileName(selectedValue(vocabularyDropdown, AndroidSettingsStore.VOCABULARY_FILES, AppSettings.DEFAULT_VOCABULARY_FILE_NAME));
        settings.setDisplayMode(DisplayMode.values()[selectedIndex(displayModeDropdown, displayModeLabels())]);
        settings.setPlaybackMode(PlaybackMode.values()[selectedIndex(playbackModeDropdown, playbackModeLabels())]);
        settings.setOverlayMode(OverlayMode.values()[selectedIndex(overlayModeDropdown, overlayModeLabels())]);
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
        vocabularyDropdown.setText(AndroidSettingsStore.CUSTOM_VOCABULARY_FILE_NAME, false);
        Toast.makeText(this, "已添加到自定义词汇", Toast.LENGTH_SHORT).show();
        customWord.setText("");
        customType.setText("");
        customPhrase.setText("");
        customExample.setText("");
        customMeaning.setText("");
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

    private String selectedValue(MaterialAutoCompleteTextView dropdown, String[] values, String fallback) {
        String value = dropdown.getText().toString();
        for (String item : values) {
            if (item.equals(value)) {
                return item;
            }
        }
        return fallback;
    }

    private int selectedIndex(MaterialAutoCompleteTextView dropdown, String[] values) {
        String value = dropdown.getText().toString();
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void styleSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(PAGE_BACKGROUND);
            getWindow().setNavigationBarColor(PAGE_BACKGROUND);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private MaterialButton tabButton(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setStrokeWidth(0);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setElevation(0);
            button.setTranslationZ(0);
            button.setStateListAnimator(null);
        }
        return button;
    }

    private void selectTab(MaterialButton selected) {
        styleTab(homeTab, selected == homeTab);
        styleTab(settingsTab, selected == settingsTab);
        styleTab(recordsTab, selected == recordsTab);
    }

    private void styleTab(MaterialButton button, boolean selected) {
        button.setAllCaps(false);
        button.setTextColor(selected ? Color.WHITE : Color.rgb(98, 99, 110));
        button.setTextSize(15);
        button.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        button.setCornerRadius(dp(20));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(selected ? PRIMARY : Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setElevation(0);
            button.setTranslationZ(0);
            button.setStateListAnimator(null);
        }
    }

    private MaterialButton primaryButton(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setCornerRadius(dp(16));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(PRIMARY));
        button.setPadding(0, dp(9), 0, dp(9));
        return button;
    }

    private MaterialButton secondaryButton(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(PRIMARY);
        button.setTextSize(16);
        button.setCornerRadius(dp(16));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(235, 238, 249)));
        button.setPadding(0, dp(9), 0, dp(9));
        return button;
    }

    private MaterialButton iconButton(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(TEXT_PRIMARY);
        button.setTextSize(28);
        button.setGravity(Gravity.CENTER);
        button.setCornerRadius(dp(28));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        return button;
    }

    private MaterialButton smallActionButton(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(PRIMARY);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setCornerRadius(dp(18));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(235, 238, 249)));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        return button;
    }

    private TextView circularIcon(String text, int background, int foreground) {
        TextView icon = new TextView(this);
        icon.setText(text);
        icon.setTextSize(26);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        icon.setTextColor(foreground);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(oval(background));
        return icon;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(rounded(CARD_BACKGROUND, Color.TRANSPARENT, dp(28)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(1));
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

    private GradientDrawable oval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private LinearLayout.LayoutParams tabLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
        params.setMargins(dp(2), 0, dp(2), 0);
        return params;
    }

    private ViewGroup.LayoutParams matchWidthWrapHeight() {
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams matchWidthWithBottomMargin(int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(bottomDp));
        return params;
    }

    private LinearLayout.LayoutParams matchWidthWithTopMargin(int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(topDp), 0, 0);
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

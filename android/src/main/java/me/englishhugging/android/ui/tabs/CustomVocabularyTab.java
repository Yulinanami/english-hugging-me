package me.englishhugging.android.ui.tabs;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.core.model.Phrase;
import me.englishhugging.core.model.Translation;
import me.englishhugging.core.model.WordEntry;

public final class CustomVocabularyTab {
    private final MainActivity activity;
    private final AndroidUi ui;
    private final LinearLayout listContainer;
    
    private EditText customWordInput;
    private EditText customTypeInput;
    private EditText customMeaningInput;
    private EditText customPhraseInput;
    private EditText customPhraseMeaningInput;
    private EditText customExampleInput;

    public CustomVocabularyTab(MainActivity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
        this.listContainer = new LinearLayout(activity);
        this.listContainer.setOrientation(LinearLayout.VERTICAL);
    }

    public View getView() {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16));

        content.addView(createAddWordSection());

        content.addView(ui.sectionLabel("已添加的词汇"), ui.matchWidthWithBottomMargin(12));

        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(listContainer);
        content.addView(scrollView);

        refreshList();

        return content;
    }

    private View createAddWordSection() {
        LinearLayout layout = ui.card();

        customWordInput = ui.input("");
        customTypeInput = ui.input("");
        customMeaningInput = ui.input("");
        customPhraseInput = ui.input("");
        customPhraseMeaningInput = ui.input("");
        customExampleInput = ui.input("");

        layout.addView(ui.settingItem("单词", "必填", customWordInput), ui.matchWidthWrapHeight());
        layout.addView(ui.settingItem("词性", "名词、动词等", customTypeInput), ui.matchWidthWrapHeight());
        layout.addView(ui.settingItem("意思", "中文释义", customMeaningInput), ui.matchWidthWrapHeight());
        layout.addView(ui.settingItem("词组", "可选", customPhraseInput), ui.matchWidthWrapHeight());
        layout.addView(ui.settingItem("词组意思", "词组释义", customPhraseMeaningInput), ui.matchWidthWrapHeight());
        layout.addView(ui.settingItem("例句", "可选", customExampleInput), ui.matchWidthWrapHeight());

        MaterialButton addBtn = ui.secondaryButton("保存单词");
        addBtn.setOnClickListener(v -> {
            String word = customWordInput.getText().toString().trim();
            if (word.isEmpty()) {
                Toast.makeText(activity, "请输入单词", Toast.LENGTH_SHORT).show();
                return;
            }
            String type = customTypeInput.getText().toString().trim();
            String meaning = customMeaningInput.getText().toString().trim();
            String phrase = customPhraseInput.getText().toString().trim();
            String phraseMeaning = customPhraseMeaningInput.getText().toString().trim();
            String example = customExampleInput.getText().toString().trim();

            List<Translation> translations = meaning.isEmpty() && type.isEmpty()
                    ? Collections.emptyList()
                    : Collections.singletonList(new Translation(meaning, type));
            List<Phrase> phrases = new ArrayList<>();
            if (!phrase.isEmpty()) phrases.add(new Phrase(phrase, phraseMeaning));
            if (!example.isEmpty()) phrases.add(new Phrase(example, ""));

            AndroidSettingsStore.appendCustomWord(activity, new WordEntry(word, translations, phrases));
            Toast.makeText(activity, "添加成功！", Toast.LENGTH_SHORT).show();

            customWordInput.setText(""); customTypeInput.setText(""); customMeaningInput.setText("");
            customPhraseInput.setText(""); customPhraseMeaningInput.setText(""); customExampleInput.setText("");
            
            refreshList();
        });
        layout.addView(addBtn, ui.matchWidthWithTopMargin(14));

        return layout;
    }

    private void refreshList() {
        listContainer.removeAllViews();
        List<WordEntry> words = AndroidSettingsStore.loadCustomWords(activity);
        if (words == null || words.isEmpty()) {
            TextView empty = ui.bodyText("暂无自定义词汇");
            empty.setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8));
            listContainer.addView(empty);
            return;
        }

        for (int i = 0; i < words.size(); i++) {
            WordEntry entry = words.get(i);
            listContainer.addView(createWordItem(entry, i));
        }
    }

    private View createWordItem(WordEntry entry, int index) {
        LinearLayout layout = ui.card();
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams params = ui.matchWidthWithBottomMargin(8);
        layout.setLayoutParams(params);

        LinearLayout textLayout = new LinearLayout(activity);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textLayout.setLayoutParams(textParams);

        textLayout.addView(ui.titleText(entry.getWord()));

        if (!entry.getTranslations().isEmpty()) {
            Translation t = entry.getTranslations().get(0);
            textLayout.addView(ui.bodyText(t.getType() + " " + t.getTranslation()));
        }
        
        for (Phrase p : entry.getPhrases()) {
            if (p.getTranslation().isEmpty()) {
                textLayout.addView(ui.bodyText("例句: " + p.getPhrase()));
            } else {
                textLayout.addView(ui.bodyText("词组: " + p.getPhrase() + " (" + p.getTranslation() + ")"));
            }
        }

        layout.addView(textLayout);

        LinearLayout btnLayout = new LinearLayout(activity);
        btnLayout.setOrientation(LinearLayout.VERTICAL);
        btnLayout.setGravity(Gravity.CENTER);

        MaterialButton editBtn = ui.secondaryButton("编辑");
        editBtn.setOnClickListener(v -> {
            ScrollView sv = (ScrollView) listContainer.getParent();
            sv.smoothScrollTo(0, 0);
            customWordInput.setText(entry.getWord());
            if (!entry.getTranslations().isEmpty()) {
                customTypeInput.setText(entry.getTranslations().get(0).getType());
                customMeaningInput.setText(entry.getTranslations().get(0).getTranslation());
            } else { customTypeInput.setText(""); customMeaningInput.setText(""); }
            customPhraseInput.setText(""); customPhraseMeaningInput.setText(""); customExampleInput.setText("");
            for (Phrase p : entry.getPhrases()) {
                if (p.getTranslation().isEmpty()) { customExampleInput.setText(p.getPhrase()); }
                else { customPhraseInput.setText(p.getPhrase()); customPhraseMeaningInput.setText(p.getTranslation()); }
            }
            Toast.makeText(activity, "可在上方修改该单词", Toast.LENGTH_SHORT).show();
        });
        btnLayout.addView(editBtn, ui.matchWidthWithBottomMargin(4));

        MaterialButton deleteBtn = ui.secondaryButton("删除");
        deleteBtn.setTextColor(Color.RED);
        deleteBtn.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setTitle("确认删除")
                .setMessage("要删除单词 " + entry.getWord() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteWord(entry.getWord());
                })
                .setNegativeButton("取消", null)
                .show());
        btnLayout.addView(deleteBtn);

        layout.addView(btnLayout);

        return layout;
    }

    private void deleteWord(String word) {
        List<WordEntry> words = AndroidSettingsStore.loadCustomWords(activity);
        if (words != null) {
            words.removeIf(w -> w.getWord().equals(word));
            AndroidSettingsStore.saveCustomWords(activity, words);
            refreshList();
            Toast.makeText(activity, "已删除", Toast.LENGTH_SHORT).show();
        }
    }
}

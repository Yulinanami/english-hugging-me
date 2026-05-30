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

/**
 * 移动端自定义词库编辑面板。
 *
 * <p>这个类提供了一个表单用来往自定义生词本中追加、编辑和删除词汇。
 * 它复用了底层的 {@link AndroidSettingsStore} 来与文件系统进行交互。
 */
public final class CustomVocabularyTab {
    
    // --- 外部依赖 ---
    private final MainActivity activity;
    private final AndroidUi ui;
    
    // --- 动态视图 ---
    private LinearLayout listContainer;
    private EditText customWordInput;
    private EditText customTypeInput;
    private EditText customMeaningInput;
    private EditText customPhraseInput;
    private EditText customPhraseMeaningInput;
    private EditText customExampleInput;

    /**
     * 构造编辑面板。
     */
    public CustomVocabularyTab(MainActivity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    /**
     * 生成并返回包裹了表单和列表的完整滚动页面。
     */
    public View getView() {
        LinearLayout content = new LinearLayout(this.activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(this.ui.dp(16), this.ui.dp(16), this.ui.dp(16), this.ui.dp(16));

        // 1. 顶部的添加/编辑表单
        content.addView(createAddWordSection());

        // 2. 底部的已添加词汇列表标签
        content.addView(this.ui.sectionLabel("已添加的词汇"), this.ui.matchWidthWithBottomMargin(12));

        // 3. 动态列表容器
        this.listContainer = new LinearLayout(this.activity);
        this.listContainer.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(this.activity);
        scrollView.addView(this.listContainer);
        content.addView(scrollView);

        // 初始化拉取数据并渲染
        refreshList();

        return content;
    }

    /**
     * 构建输入表单卡片。
     */
    private View createAddWordSection() {
        LinearLayout layout = this.ui.card();

        this.customWordInput = this.ui.input("");
        this.customTypeInput = this.ui.input("");
        this.customMeaningInput = this.ui.input("");
        this.customPhraseInput = this.ui.input("");
        this.customPhraseMeaningInput = this.ui.input("");
        this.customExampleInput = this.ui.input("");

        layout.addView(this.ui.settingItem("单词", "必填", this.customWordInput), this.ui.matchWidthWrapHeight());
        layout.addView(this.ui.settingItem("词性", "名词、动词等", this.customTypeInput), this.ui.matchWidthWrapHeight());
        layout.addView(this.ui.settingItem("意思", "中文释义", this.customMeaningInput), this.ui.matchWidthWrapHeight());
        layout.addView(this.ui.settingItem("词组", "可选", this.customPhraseInput), this.ui.matchWidthWrapHeight());
        layout.addView(this.ui.settingItem("词组意思", "词组释义", this.customPhraseMeaningInput), this.ui.matchWidthWrapHeight());
        layout.addView(this.ui.settingItem("例句", "可选", this.customExampleInput), this.ui.matchWidthWrapHeight());

        MaterialButton addBtn = this.ui.secondaryButton("保存单词");
        addBtn.setOnClickListener(v -> {
            String word = this.customWordInput.getText().toString().trim();
            if (word.isEmpty()) {
                Toast.makeText(this.activity, "请输入单词", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String type = this.customTypeInput.getText().toString().trim();
            String meaning = this.customMeaningInput.getText().toString().trim();
            String phrase = this.customPhraseInput.getText().toString().trim();
            String phraseMeaning = this.customPhraseMeaningInput.getText().toString().trim();
            String example = this.customExampleInput.getText().toString().trim();

            // 封装对象模型
            List<Translation> translations;
            if (meaning.isEmpty() && type.isEmpty()) {
                translations = Collections.emptyList();
            } else {
                translations = Collections.singletonList(new Translation(meaning, type));
            }
            
            List<Phrase> phrases = new ArrayList<>();
            if (!phrase.isEmpty()) {
                phrases.add(new Phrase(phrase, phraseMeaning));
            }
            if (!example.isEmpty()) {
                phrases.add(new Phrase(example, ""));
            }

            // 保存到磁盘
            AndroidSettingsStore.appendCustomWord(this.activity, new WordEntry(word, translations, phrases));
            Toast.makeText(this.activity, "添加成功！", Toast.LENGTH_SHORT).show();

            // 清空表单
            this.customWordInput.setText(""); 
            this.customTypeInput.setText(""); 
            this.customMeaningInput.setText("");
            this.customPhraseInput.setText(""); 
            this.customPhraseMeaningInput.setText(""); 
            this.customExampleInput.setText("");
            
            // 刷新列表视图
            refreshList();
        });
        
        layout.addView(addBtn, this.ui.matchWidthWithTopMargin(14));

        return layout;
    }

    /**
     * 重新从磁盘读取 JSON 数据并铺满列表。
     */
    private void refreshList() {
        this.listContainer.removeAllViews();
        List<WordEntry> words = AndroidSettingsStore.loadCustomWords(this.activity);
        
        if (words == null || words.isEmpty()) {
            TextView empty = this.ui.bodyText("暂无自定义词汇");
            empty.setPadding(this.ui.dp(8), this.ui.dp(8), this.ui.dp(8), this.ui.dp(8));
            this.listContainer.addView(empty);
            return;
        }

        for (int i = 0; i < words.size(); i++) {
            WordEntry entry = words.get(i);
            this.listContainer.addView(createWordItem(entry, i));
        }
    }

    /**
     * 为单个生词渲染它的预览卡片，附带“编辑”和“删除”操作按钮。
     */
    private View createWordItem(WordEntry entry, int index) {
        LinearLayout layout = this.ui.card();
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams params = this.ui.matchWidthWithBottomMargin(8);
        layout.setLayoutParams(params);

        // 左侧单词文本信息
        LinearLayout textLayout = new LinearLayout(this.activity);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textLayout.setLayoutParams(textParams);

        textLayout.addView(this.ui.titleText(entry.getWord()));

        if (!entry.getTranslations().isEmpty()) {
            Translation t = entry.getTranslations().get(0);
            textLayout.addView(this.ui.bodyText(t.getType() + " " + t.getTranslation()));
        }
        
        for (Phrase p : entry.getPhrases()) {
            if (p.getTranslation().isEmpty()) {
                textLayout.addView(this.ui.bodyText("例句: " + p.getPhrase()));
            } else {
                textLayout.addView(this.ui.bodyText("词组: " + p.getPhrase() + " (" + p.getTranslation() + ")"));
            }
        }

        layout.addView(textLayout);

        // 右侧操作按钮组
        LinearLayout btnLayout = new LinearLayout(this.activity);
        btnLayout.setOrientation(LinearLayout.VERTICAL);
        btnLayout.setGravity(Gravity.CENTER);

        MaterialButton editBtn = this.ui.secondaryButton("编辑");
        editBtn.setOnClickListener(v -> {
            // 点击编辑时，自动滚动到顶部表单并将旧数据反填
            ScrollView sv = (ScrollView) this.listContainer.getParent();
            sv.smoothScrollTo(0, 0);
            
            this.customWordInput.setText(entry.getWord());
            if (!entry.getTranslations().isEmpty()) {
                this.customTypeInput.setText(entry.getTranslations().get(0).getType());
                this.customMeaningInput.setText(entry.getTranslations().get(0).getTranslation());
            } else { 
                this.customTypeInput.setText(""); 
                this.customMeaningInput.setText(""); 
            }
            
            this.customPhraseInput.setText(""); 
            this.customPhraseMeaningInput.setText(""); 
            this.customExampleInput.setText("");
            
            for (Phrase p : entry.getPhrases()) {
                if (p.getTranslation().isEmpty()) { 
                    this.customExampleInput.setText(p.getPhrase()); 
                } else { 
                    this.customPhraseInput.setText(p.getPhrase()); 
                    this.customPhraseMeaningInput.setText(p.getTranslation()); 
                }
            }
            Toast.makeText(this.activity, "可在上方修改该单词", Toast.LENGTH_SHORT).show();
        });
        btnLayout.addView(editBtn, this.ui.matchWidthWithBottomMargin(4));

        MaterialButton deleteBtn = this.ui.secondaryButton("删除");
        deleteBtn.setTextColor(Color.RED);
        deleteBtn.setOnClickListener(v -> new AlertDialog.Builder(this.activity)
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

    /**
     * 将删除命令落地并刷新视图。
     */
    private void deleteWord(String word) {
        List<WordEntry> words = AndroidSettingsStore.loadCustomWords(this.activity);
        if (words != null) {
            // 通过拼写来定位并剔除
            words.removeIf(w -> w.getWord().equals(word));
            
            AndroidSettingsStore.saveCustomWords(this.activity, words);
            refreshList();
            Toast.makeText(this.activity, "已删除", Toast.LENGTH_SHORT).show();
        }
    }
}

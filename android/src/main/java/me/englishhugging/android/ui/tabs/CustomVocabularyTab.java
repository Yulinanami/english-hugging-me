package me.englishhugging.android.ui.tabs;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.englishhugging.android.MainActivity;
import me.englishhugging.android.databinding.ItemCustomWordBinding;
import me.englishhugging.android.databinding.PageCustomVocabularyBinding;
import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.core.model.Phrase;
import me.englishhugging.core.model.Translation;
import me.englishhugging.core.model.WordEntry;

/**
 * Android 手机端“自定义词库”界面，支持添加、修改和删除生词。
 *
 * <p>这个类负责管理用户录入的生词本，提供添加单词、编辑释义、删除词条以及列表浏览功能。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * CustomVocabularyTab customTab = new CustomVocabularyTab(activity, ui, () -> goHome());
 * View view = customTab.getView();
 * pageContainer.addView(view);
 * </code></pre>
 */
public final class CustomVocabularyTab {
    /** 所属的主界面对象 */
    private final MainActivity activity;

    /** 界面辅助工具 */
    private final AndroidUi ui;

    /** 返回首页的回调 */
    private final Runnable goHome;

    /** 自定义词库页面视图绑定对象 */
    private PageCustomVocabularyBinding binding;

    /**
     * 创建自定义生词页面对象。
     *
     * @param activity 所属的主界面对象
     * @param ui       界面辅助工具
     * @param goHome   返回首页的回调
     */
    public CustomVocabularyTab(MainActivity activity, AndroidUi ui, Runnable goHome) {
        this.activity = activity;
        this.ui = ui;
        this.goHome = goHome;
    }

    /**
     * 加载并返回自定义词库页面视图。
     *
     * @return 自定义词库页面根视图
     */
    public View getView() {
        this.binding = PageCustomVocabularyBinding.inflate(this.activity.getLayoutInflater());
        this.ui.styleIcon(this.binding.backIcon);
        this.binding.backIcon.setOnClickListener(view -> this.goHome.run());
        this.binding.saveButton.setOnClickListener(view -> saveWord());
        refreshList();
        return this.binding.getRoot();
    }

    /**
     * 从输入框读取内容并保存为自定义生词。
     */
    private void saveWord() {
        String word = this.binding.customWordInput.getText().toString().trim();
        if (word.isEmpty()) {
            Toast.makeText(this.activity, "请输入单词", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = this.binding.customTypeInput.getText().toString().trim();
        String meaning = this.binding.customMeaningInput.getText().toString().trim();
        String phrase = this.binding.customPhraseInput.getText().toString().trim();
        String phraseMeaning = this.binding.customPhraseMeaningInput.getText().toString().trim();
        String example = this.binding.customExampleInput.getText().toString().trim();

        // 只要词性或释义非空，就创建一条释义信息
        List<Translation> translations;
        if (meaning.isEmpty() && type.isEmpty()) {
            translations = Collections.emptyList();
        } else {
            translations = Collections.singletonList(new Translation(meaning, type));
        }

        List<Phrase> phrases = new ArrayList<>();
        // 短语与例句共用数据结构：没有翻译的条目按例句展示
        if (!phrase.isEmpty()) {
            phrases.add(new Phrase(phrase, phraseMeaning));
        }
        if (!example.isEmpty()) {
            phrases.add(new Phrase(example, ""));
        }

        AndroidSettingsStore.appendCustomWord(
                this.activity,
                new WordEntry(word, translations, phrases)
        );
        Toast.makeText(this.activity, "添加成功！", Toast.LENGTH_SHORT).show();
        clearForm();
        refreshList();
    }

    /** 保存成功后清空输入区，方便继续录入下一个单词。 */
    private void clearForm() {
        this.binding.customWordInput.setText("");
        this.binding.customTypeInput.setText("");
        this.binding.customMeaningInput.setText("");
        this.binding.customPhraseInput.setText("");
        this.binding.customPhraseMeaningInput.setText("");
        this.binding.customExampleInput.setText("");
    }

    /** 重新读取自定义词库并刷新列表区域。 */
    private void refreshList() {
        this.binding.listContainer.removeAllViews();
        List<WordEntry> words = AndroidSettingsStore.loadCustomWords(this.activity);
        boolean isEmpty = words.isEmpty();
        this.binding.emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            return;
        }

        for (WordEntry entry : words) {
            this.binding.listContainer.addView(createWordItem(entry));
        }
    }

    /** 为一个单词创建列表项，并绑定编辑和删除操作。 */
    private View createWordItem(WordEntry entry) {
        ItemCustomWordBinding item = ItemCustomWordBinding.inflate(
                this.activity.getLayoutInflater(),
                this.binding.listContainer,
                false
        );
        item.wordText.setText(entry.word());

        String details = formatDetails(entry);
        item.detailsText.setText(details);
        item.detailsText.setVisibility(details.isEmpty() ? View.GONE : View.VISIBLE);

        item.editButton.setOnClickListener(view -> editWord(entry));
        item.deleteButton.setOnClickListener(view -> showDeleteConfirmation(entry.word()));
        return item.getRoot();
    }

    /** 将单词的释义、短语和例句整理为适合列表快速浏览的多行文字。 */
    private String formatDetails(WordEntry entry) {
        List<String> details = new ArrayList<>();
        if (!entry.translations().isEmpty()) {
            Translation translation = entry.translations().get(0);
            details.add(translation.type() + " " + translation.translation());
        }
        for (Phrase phrase : entry.phrases()) {
            if (phrase.translation().isEmpty()) {
                details.add("例句: " + phrase.phrase());
            } else {
                details.add("词组: " + phrase.phrase() + " (" + phrase.translation() + ")");
            }
        }
        return String.join("\n", details);
    }

    /** 把已有单词内容回填到顶部表单，供用户修改后再次保存。 */
    private void editWord(WordEntry entry) {
        this.binding.scrollView.smoothScrollTo(0, 0);
        this.binding.customWordInput.setText(entry.word());
        if (!entry.translations().isEmpty()) {
            this.binding.customTypeInput.setText(entry.translations().get(0).type());
            this.binding.customMeaningInput.setText(entry.translations().get(0).translation());
        } else {
            this.binding.customTypeInput.setText("");
            this.binding.customMeaningInput.setText("");
        }

        this.binding.customPhraseInput.setText("");
        this.binding.customPhraseMeaningInput.setText("");
        this.binding.customExampleInput.setText("");
        // 当前表单各保留一个词组和一个例句，后出现的同类型内容会覆盖前一项。
        for (Phrase phrase : entry.phrases()) {
            if (phrase.translation().isEmpty()) {
                this.binding.customExampleInput.setText(phrase.phrase());
            } else {
                this.binding.customPhraseInput.setText(phrase.phrase());
                this.binding.customPhraseMeaningInput.setText(phrase.translation());
            }
        }
        Toast.makeText(this.activity, "可在上方修改该单词", Toast.LENGTH_SHORT).show();
    }

    /** 删除前显示确认对话框，避免误触直接丢失单词。 */
    private void showDeleteConfirmation(String word) {
        new AlertDialog.Builder(this.activity)
                .setTitle("确认删除")
                .setMessage("要删除单词 " + word + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteWord(word))
                .setNegativeButton("取消", null)
                .show();
    }

    /** 从本地词库删除所有同名条目，并刷新页面列表。 */
    private void deleteWord(String word) {
        List<WordEntry> words = AndroidSettingsStore.loadCustomWords(this.activity);
        words.removeIf(entry -> entry.word().equals(word));
        AndroidSettingsStore.saveCustomWords(this.activity, words);
        refreshList();
        Toast.makeText(this.activity, "已删除", Toast.LENGTH_SHORT).show();
    }
}

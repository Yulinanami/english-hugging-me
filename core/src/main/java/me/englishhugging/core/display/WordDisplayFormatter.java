package me.englishhugging.core.display;

import me.englishhugging.core.model.Phrase;
import me.englishhugging.core.model.Translation;
import me.englishhugging.core.model.WordDisplaySegment;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.DisplayMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单词文本拆分与排版工具。
 *
 * <p>将单词条目（{@link WordEntry}）拆分为带样式的富文本片段列表（{@link WordDisplaySegment}），
 * 方便界面根据不同部分（单词、词性、释义、短语）分别渲染对应的颜色和字号。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * WordDisplayFormatter formatter = new WordDisplayFormatter();
 * List&lt;WordDisplaySegment&gt; segments = formatter.format(entry, DisplayMode.WORD_WITH_TRANSLATION);
 * </code></pre>
 */
public final class WordDisplayFormatter {
    
    /** 悬浮窗最多展示的例句数量 */
    private static final int PHRASE_DISPLAY_LIMIT = 2;

    /**
     * 根据显示模式将单词拆分为富文本片段（默认不隐藏任何部分）。
     *
     * @param wordEntry   目标单词条目
     * @param displayMode 显示模式
     * @return 富文本片段列表
     */
    public List<WordDisplaySegment> format(WordEntry wordEntry, DisplayMode displayMode) {
        return format(wordEntry, displayMode, false, false);
    }

    /**
     * 根据显示模式及填空隐藏开关，将单词拆分为富文本片段。
     *
     * @param wordEntry       目标单词条目
     * @param displayMode     显示模式
     * @param hidePhrases     是否隐藏例句短语
     * @param hideTranslation 是否隐藏中文释义
     * @return 富文本片段列表
     */
    public List<WordDisplaySegment> format(
            WordEntry wordEntry, 
            DisplayMode displayMode, 
            boolean hidePhrases, 
            boolean hideTranslation
    ) {
        if (wordEntry == null) {
            return Collections.emptyList();
        }

        DisplayMode safeMode = displayMode != null ? displayMode : DisplayMode.WORD_WITH_TRANSLATION;

        List<WordDisplaySegment> segments = new ArrayList<>();
        
        // 1. 插入英文单词文本
        String safeWord = safe(wordEntry.word());
        segments.add(new WordDisplaySegment(WordDisplaySegment.Type.WORD, safeWord));

        // 仅显示单词模式或处于填空隐藏释义状态时，直接返回
        if (safeMode == DisplayMode.WORD_ONLY || hideTranslation) {
            return segments;
        }

        // 2. 插入翻译和词性
        appendTranslations(wordEntry, segments);

        // 3. 插入例句和短语
        boolean shouldShowPhrases = (safeMode == DisplayMode.WORD_WITH_TRANSLATION_AND_PHRASE) && (!hidePhrases);
        if (shouldShowPhrases) {
            appendPhrases(wordEntry, segments);
        }

        return segments;
    }

    /**
     * 将单词的所有词性与释义（如 "n. 苹果"）拆分成富文本片段并追加到显示列表中。
     */
    private void appendTranslations(WordEntry wordEntry, List<WordDisplaySegment> segments) {
        for (Translation translation : wordEntry.translations()) {
            String type = safe(translation.type());
            String meaning = safe(translation.translation());
            
            if (type.length() == 0 && meaning.length() == 0) {
                continue;
            }
            
            segments.add(new WordDisplaySegment(WordDisplaySegment.Type.LINE_BREAK, "\n"));
            
            if (type.length() > 0) {
                segments.add(new WordDisplaySegment(WordDisplaySegment.Type.TYPE, type + ". "));
            }
            
            segments.add(new WordDisplaySegment(WordDisplaySegment.Type.TRANSLATION, meaning));
        }
    }

    /**
     * 将单词的短语与例句拆分成富文本片段并追加到显示列表中（最多显示 2 条）。
     */
    private void appendPhrases(WordEntry wordEntry, List<WordDisplaySegment> segments) {
        int displayed = 0;

        for (Phrase phrase : wordEntry.phrases()) {
            String phraseText = safe(phrase.phrase());
            String phraseTranslation = safe(phrase.translation());
            
            if (phraseText.length() == 0 && phraseTranslation.length() == 0) {
                continue;
            }
            
            segments.add(new WordDisplaySegment(WordDisplaySegment.Type.LINE_BREAK, "\n"));
            segments.add(new WordDisplaySegment(WordDisplaySegment.Type.PHRASE, phraseText));
            
            if (phraseTranslation.length() > 0) {
                segments.add(new WordDisplaySegment(WordDisplaySegment.Type.PHRASE_TRANSLATION, "： " + phraseTranslation));
            }
            
            displayed++;
            if (displayed >= PHRASE_DISPLAY_LIMIT) {
                break;
            }
        }
    }

    /**
     * 安全处理字符串，null 转为空字符串并去除首尾空白字符。
     */
    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}

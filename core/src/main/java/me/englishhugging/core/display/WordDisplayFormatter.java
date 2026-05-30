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
 * 单词视图排版与着色数据格式化器。
 *
 * <p>该类负责将纯粹的数据模型 {@link WordEntry} 转换为一组富文本排版所需的片段 {@link WordDisplaySegment}。
 * 它的转换逻辑受全局设置（如 {@link DisplayMode} 和填空模式隐藏开关）的严格约束。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * WordDisplayFormatter formatter = new WordDisplayFormatter();
 * 
 * // 将单词按“仅显示单词和翻译”模式进行拆包分片
 * List&lt;WordDisplaySegment&gt; segments = formatter.format(entry, DisplayMode.WORD_WITH_TRANSLATION);
 * 
 * for (WordDisplaySegment segment : segments) {
 *     // UI 引擎根据 segment.getType() 渲染不同颜色
 *     renderToScreen(segment.getText(), getColor(segment.getType()));
 * }
 * </code></pre>
 */
public final class WordDisplayFormatter {
    
    /** 为了避免占用过多桌面空间，硬编码限制最多只展示 2 个例句 */
    private static final int PHRASE_DISPLAY_LIMIT = 2;

    /**
     * 按照指定的模式对单词词条进行格式化分片（默认不隐藏任何附加内容）。
     *
     * @param wordEntry   目标单词实体
     * @param displayMode 用户配置的显示模式
     * @return 用于渲染的格式化文本片段列表
     */
    public List<WordDisplaySegment> format(WordEntry wordEntry, DisplayMode displayMode) {
        return format(wordEntry, displayMode, false, false);
    }

    /**
     * 对单词词条进行精细的格式化分片。
     * 此重载方法提供了两个额外的布尔开关，专门用于在填空考核模式下临时遮蔽提示信息。
     *
     * @param wordEntry       目标单词实体
     * @param displayMode     用户配置的显示模式
     * @param hidePhrases     强行隐藏例句短语（覆盖 displayMode 的设置）
     * @param hideTranslation 强行隐藏中文释义
     * @return 用于渲染的格式化文本片段列表
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

        DisplayMode safeMode = DisplayMode.WORD_WITH_TRANSLATION;
        if (displayMode != null) {
            safeMode = displayMode;
        }

        List<WordDisplaySegment> segments = new ArrayList<>();
        
        // 1. 恒定插入：单词的主体字母
        String safeWord = safe(wordEntry.getWord());
        segments.add(new WordDisplaySegment(WordDisplaySegment.Type.WORD, safeWord));

        // 如果用户选择了极简模式，或者当前处于严格的填空模式下，则提前返回 (Early Return)
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
     * 辅助方法：处理并追加翻译片段到集合中。
     */
    private void appendTranslations(WordEntry wordEntry, List<WordDisplaySegment> segments) {
        if (wordEntry.getTranslations() == null) {
            return;
        }
        
        for (Translation translation : wordEntry.getTranslations()) {
            if (translation == null) {
                continue;
            }
            
            String type = safe(translation.getType());
            String meaning = safe(translation.getTranslation());
            
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
     * 辅助方法：处理并追加例句短语片段到集合中，受 {@link #PHRASE_DISPLAY_LIMIT} 约束。
     */
    private void appendPhrases(WordEntry wordEntry, List<WordDisplaySegment> segments) {
        if (wordEntry.getPhrases() == null) {
            return;
        }
        
        int displayed = 0;
        
        for (Phrase phrase : wordEntry.getPhrases()) {
            if (phrase == null) {
                continue;
            }
            
            String phraseText = safe(phrase.getPhrase());
            String phraseTranslation = safe(phrase.getTranslation());
            
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
     * 防御性文本获取工具，避免空指针并在头尾去空白。
     */
    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}

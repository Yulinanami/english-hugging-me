package me.englishhugging.android.overlay;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;

import me.englishhugging.core.display.WordDisplayFormatter;
import me.englishhugging.core.model.WordDisplaySegment;
import me.englishhugging.core.model.WordEntry;
import me.englishhugging.core.settings.AppSettings;

/**
 * 单词富文本渲染辅助工具。
 *
 * <p>负责根据用户的外观配置（字号、文本颜色、显示模式等），
 * 将单词数据条目格式化为带颜色与字号样式的富文本并呈现到文本控件中。
 */
public final class OverlayTextRenderer {

    /** 单词文本拆分工具 */
    private final WordDisplayFormatter wordDisplayFormatter = new WordDisplayFormatter();

    /**
     * 将单词内容与外观样式格式化后，直接设置到悬浮窗文本控件中。
     *
     * @param textView        用于显示单词富文本的文本控件
     * @param wordEntry       待展示的单词数据条目
     * @param settings        当前的外观配置
     * @param hidePhrases     是否隐藏例句短语
     * @param hideTranslation 是否隐藏中文释义
     */
    public void render(
            TextView textView,
            WordEntry wordEntry,
            AppSettings settings,
            boolean hidePhrases,
            boolean hideTranslation
    ) {
        if (textView == null || wordEntry == null || settings == null) {
            return;
        }
        textView.setText(format(wordEntry, settings, hidePhrases, hideTranslation));
    }

    /**
     * 将单词分段信息转换为带颜色与字号样式的 SpannableStringBuilder 富文本。
     *
     * @param wordEntry       待展示的单词数据条目
     * @param settings        当前的外观配置
     * @param hidePhrases     是否隐藏例句短语
     * @param hideTranslation 是否隐藏中文释义
     * @return 带有颜色和字号样式的富文本字符串
     */
    public CharSequence format(
            WordEntry wordEntry,
            AppSettings settings,
            boolean hidePhrases,
            boolean hideTranslation
    ) {
        if (wordEntry == null || settings == null) {
            return "";
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();

        for (WordDisplaySegment segment : this.wordDisplayFormatter.format(
                wordEntry,
                settings.getDisplayMode(),
                hidePhrases,
                hideTranslation
        )) {
            int start = builder.length();
            builder.append(segment.text());
            int end = builder.length();

            if (segment.type() == WordDisplaySegment.Type.LINE_BREAK || start == end) {
                continue;
            }

            // 设置文字颜色
            builder.setSpan(
                    new ForegroundColorSpan(colorForSegment(segment.type(), settings)),
                    start, end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // 设置加粗
            if (isBoldSegment(segment.type())) {
                builder.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        start, end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            // 设置字号（区分单词本体字号和详细释义字号，单位：sp）
            int fontSizeSp = segment.type() == WordDisplaySegment.Type.WORD
                    ? settings.getWordFontSize()
                    : settings.getDetailFontSize();
            builder.setSpan(
                    new AbsoluteSizeSpan(fontSizeSp, true),
                    start, end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        return builder;
    }

    /**
     * 根据单词文本分段类型获取对应的文字颜色（Android 颜色值）。
     */
    private int colorForSegment(WordDisplaySegment.Type type, AppSettings settings) {
        if (type == WordDisplaySegment.Type.WORD) {
            return parseColor(settings.getWordColor(), Color.WHITE);
        }
        if (type == WordDisplaySegment.Type.TYPE) {
            return parseColor(settings.getTypeColor(), Color.CYAN);
        }
        if (type == WordDisplaySegment.Type.PHRASE) {
            return parseColor(settings.getPhraseColor(), Color.GREEN);
        }
        return parseColor(settings.getTranslationColor(), Color.WHITE);
    }

    /**
     * 判断该文本分段是否需要加粗显示。
     */
    private boolean isBoldSegment(WordDisplaySegment.Type type) {
        return type == WordDisplaySegment.Type.WORD
                || type == WordDisplaySegment.Type.TYPE
                || type == WordDisplaySegment.Type.PHRASE;
    }

    /**
     * 将十六进制颜色字符串转换为 Android 颜色整数值，解析失败时使用默认备用颜色。
     */
    private int parseColor(String value, int fallbackColor) {
        try {
            return Color.parseColor(value);
        } catch (RuntimeException ignored) {
            return fallbackColor;
        }
    }
}

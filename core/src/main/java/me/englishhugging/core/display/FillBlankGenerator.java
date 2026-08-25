package me.englishhugging.core.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 单词拼写填空提示工具。
 *
 * <p>将完整的英文单词转换为带下划线的填空形式（如把 "apple" 转换为 "a_p_e"），
 * 并支持按时间间隔逐个恢复被挖空的字母。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * FillBlankGenerator generator = new FillBlankGenerator();
 * 
 * // 1. 根据原词生成填空初始状态
 * FillBlankGenerator.BlankResult result = generator.generateBlanked("hello");
 * System.out.println(result.blankedWord()); // 比如: "h_ll_"
 *
 * // 2. 填回其中一个字母
 * String nextWord = generator.fillOneBlank("h_ll_", "hello", result.blankPositions());
 * System.out.println(nextWord); // 比如: "he_ll_"
 * </code></pre>
 */
public final class FillBlankGenerator {
    
    /** 用于代替挖空字母的默认隐藏符号 */
    private static final char BLANK_CHAR = '_';
    
    /** 随机数对象，用于随机选取挖空位置 */
    private final Random random = new Random();

    /**
     * 记录单词挖空后的结果。
     *
     * @param blankedWord    带有下划线的单词（如 "a__le"）
     * @param blankPositions 被挖掉字母的字符位置列表
     */
    public record BlankResult(String blankedWord, List<Integer> blankPositions) {

        public BlankResult {
            blankPositions = Collections.unmodifiableList(new ArrayList<>(blankPositions));
        }
    }

    /**
     * 把一个完整的单词按规则挖掉部分字母生成填空题。
     *
     * <p>规则：只挖英文字母，不挖空格或标点；挖空数量约为单词总长度的三分之一。
     *
     * @param word 原始英文单词（如 "apple"）
     * @return 挖空后的结果对象
     */
    public BlankResult generateBlanked(String word) {
        if (word == null || word.isEmpty()) {
            return new BlankResult("", Collections.emptyList());
        }

        // 1. 找出所有可以被挖空的字母位置
        List<Integer> eligiblePositions = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetter(word.charAt(i))) {
                eligiblePositions.add(i);
            }
        }

        // 2. 根据比例计算需要挖出多少个空
        int blankCount = (int) Math.ceil(word.length() / 3.0);
        blankCount = Math.min(blankCount, eligiblePositions.size());

        // 3. 打乱候选位置，选取前 N 个作为挖空位置
        Collections.shuffle(eligiblePositions, this.random);
        List<Integer> blankPositions = new ArrayList<>(eligiblePositions.subList(0, blankCount));
        
        // 按照升序排序，为后续从左到右依次提示（填回）做准备
        Collections.sort(blankPositions);

        // 4. 将对应位置的字母替换为下划线
        char[] chars = word.toCharArray();
        for (int pos : blankPositions) {
            chars[pos] = BLANK_CHAR;
        }

        return new BlankResult(new String(chars), blankPositions);
    }

    /**
     * 填回一个空位：将未填满的空位还原为其原始字母。
     *
     * @param currentWord     当前屏幕上显示的带下划线字符串（如 "a_p_e"）
     * @param originalWord    完整的原始英文单词（如 "apple"）
     * @param remainingBlanks 待填回的空位位置列表（方法内部会移除已填回的位置）
     * @return 填补了一个空位后的新字符串
     */
    public String fillOneBlank(String currentWord, String originalWord, List<Integer> remainingBlanks) {
        if (remainingBlanks == null || remainingBlanks.isEmpty()) {
            return originalWord;
        }

        // 总是从左边第一个尚未填补的空隙开始
        int pos = remainingBlanks.remove(0);

        char[] chars = currentWord.toCharArray();
        boolean isValidIndex = pos >= 0 && pos < chars.length && pos < originalWord.length();
        
        if (isValidIndex) {
            chars[pos] = originalWord.charAt(pos);
        }

        return new String(chars);
    }
}

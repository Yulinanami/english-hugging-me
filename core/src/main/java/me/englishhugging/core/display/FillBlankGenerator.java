package me.englishhugging.core.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 单词拼写考核填空生成器。
 *
 * <p>这个核心业务组件用于将一个完整的英语单词转换为带有占位符的填空题（例如把 "apple" 转换为 "a_p_e"）。
 * 它负责计算应该挖空的数量、随机打乱挖空的位置，并提供了随着时间推移逐个填回空位的业务功能。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * FillBlankGenerator generator = new FillBlankGenerator();
 * 
 * // 1. 根据原词生成填空初始态
 * FillBlankGenerator.BlankResult result = generator.generateBlanked("hello");
 * System.out.println(result.getBlankedWord()); // 比如: "h_ll_"
 * 
 * // 2. 揭示（填回）其中一个字母
 * String nextWord = generator.fillOneBlank("h_ll_", "hello", result.getBlankPositions());
 * System.out.println(nextWord); // 比如: "he_ll_"
 * </code></pre>
 */
public final class FillBlankGenerator {
    
    /** 用于代替挖空字母的默认隐藏符号 */
    private static final char BLANK_CHAR = '_';
    
    /** 局部随机数发生器，用于决定哪些位置被挖空 */
    private final Random random = new Random();

    /**
     * 内部不可变值对象：包装了挖空生成后的状态结果。
     */
    public static final class BlankResult {
        
        private final String blankedWord;
        private final List<Integer> blankPositions;

        /**
         * 构造生成结果。
         *
         * @param blankedWord    带有下划线的单词字符串
         * @param blankPositions 那些被成功替换成下划线的字符数组下标
         */
        public BlankResult(String blankedWord, List<Integer> blankPositions) {
            this.blankedWord = blankedWord;
            // 进行防卫性不可变包装，防止外部修改挖空位点集合
            this.blankPositions = Collections.unmodifiableList(new ArrayList<>(blankPositions));
        }

        public String getBlankedWord() { 
            return this.blankedWord; 
        }
        
        public List<Integer> getBlankPositions() { 
            return this.blankPositions; 
        }
    }

    /**
     * 对给定的完整单词生成挖空初始态。
     * 算法规则：
     * 1. 只挖掘标准的英文字母，不影响空格和标点符号。
     * 2. 挖空数量为总字母长度的三分之一（向上取整）。
     *
     * @param word 原始的英文单词
     * @return 包含着挖空字符串和坑位索引的 {@link BlankResult}
     */
    public BlankResult generateBlanked(String word) {
        if (word == null || word.isEmpty()) {
            String safeWord = "";
            if (word != null) {
                safeWord = word;
            }
            return new BlankResult(safeWord, Collections.emptyList());
        }

        // 1. 扫描出所有合法的、能够被挖空的字母位置下标
        List<Integer> eligiblePositions = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetter(word.charAt(i))) {
                eligiblePositions.add(i);
            }
        }

        // 2. 根据比例计算需要挖出多少个空
        int blankCount = (int) Math.ceil(word.length() / 3.0);
        blankCount = Math.min(blankCount, eligiblePositions.size());

        // 3. 将所有可能挖空的位置打乱，截取前 N 个作为最终被挖空的目标位点
        Collections.shuffle(eligiblePositions, this.random);
        List<Integer> blankPositions = new ArrayList<>(eligiblePositions.subList(0, blankCount));
        
        // 按照升序排序，为后续从左到右依次提示（填回）做准备
        Collections.sort(blankPositions);

        // 4. 将对应的字符替换成底线
        char[] chars = word.toCharArray();
        for (int pos : blankPositions) {
            chars[pos] = BLANK_CHAR;
        }

        return new BlankResult(new String(chars), blankPositions);
    }

    /**
     * 执行填空提示：将第一个剩余未填满的空位还原为其本来面目。
     * 这个方法通常被定时器所驱动，每一次调用都会减少剩余空位数量。
     *
     * @param currentWord     当前屏幕上正在显示且包含挖空的字符串（如 "a_p_e"）
     * @param originalWord    完整且正确的原始单词（如 "apple"）
     * @param remainingBlanks 一个维护剩余等待填回坐标的可变集合，调用此方法会从中消费并移除元素
     * @return 填补了一个坑位后的新字符串
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

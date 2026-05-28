package me.englishhugging.core.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class FillBlankGenerator {
    private static final char BLANK_CHAR = '_';
    private final Random random = new Random();

    /**
     * Result of generating a blanked word.
     */
    public static final class BlankResult {
        private final String blankedWord;
        private final List<Integer> blankPositions;

        public BlankResult(String blankedWord, List<Integer> blankPositions) {
            this.blankedWord = blankedWord;
            this.blankPositions = Collections.unmodifiableList(new ArrayList<>(blankPositions));
        }

        public String getBlankedWord() { return blankedWord; }
        public List<Integer> getBlankPositions() { return blankPositions; }
    }

    /**
     * Generate a blanked version of the given word.
     * Number of blanks = ceil(word.length() / 3).
     * Only alphabetic characters are eligible for blanking.
     */
    public BlankResult generateBlanked(String word) {
        if (word == null || word.isEmpty()) {
            return new BlankResult(word == null ? "" : word, Collections.emptyList());
        }

        List<Integer> eligiblePositions = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetter(word.charAt(i))) {
                eligiblePositions.add(i);
            }
        }

        int blankCount = (int) Math.ceil(word.length() / 3.0);
        blankCount = Math.min(blankCount, eligiblePositions.size());

        Collections.shuffle(eligiblePositions, random);
        List<Integer> blankPositions = new ArrayList<>(eligiblePositions.subList(0, blankCount));
        Collections.sort(blankPositions);

        char[] chars = word.toCharArray();
        for (int pos : blankPositions) {
            chars[pos] = BLANK_CHAR;
        }

        return new BlankResult(new String(chars), blankPositions);
    }

    /**
     * Fill one blank back in. Returns the updated word string and removes
     * the filled position from remainingBlanks.
     */
    public String fillOneBlank(String currentWord, String originalWord, List<Integer> remainingBlanks) {
        if (remainingBlanks == null || remainingBlanks.isEmpty()) {
            return originalWord;
        }

        // Fill from left to right (remainingBlanks is sorted ascending)
        int pos = remainingBlanks.remove(0);

        char[] chars = currentWord.toCharArray();
        if (pos >= 0 && pos < chars.length && pos < originalWord.length()) {
            chars[pos] = originalWord.charAt(pos);
        }

        return new String(chars);
    }
}

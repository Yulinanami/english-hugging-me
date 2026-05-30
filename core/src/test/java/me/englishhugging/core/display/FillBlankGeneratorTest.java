package me.englishhugging.core.display;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FillBlankGeneratorTest {

    private final FillBlankGenerator generator = new FillBlankGenerator();

    @Test
    void testGenerateBlanked() {
        String word = "hello";
        FillBlankGenerator.BlankResult result = generator.generateBlanked(word);
        
        String blanked = result.getBlankedWord();
        assertNotNull(blanked);
        assertEquals(word.length(), blanked.length());
        
        List<Integer> positions = result.getBlankPositions();
        assertFalse(positions.isEmpty(), "There should be at least one blank position");
        
        for (int pos : positions) {
            assertEquals('_', blanked.charAt(pos));
        }
    }

    @Test
    void testFillOneBlank() {
        String current = "h_ll_";
        String original = "hello";
        List<Integer> remaining = new java.util.ArrayList<>(java.util.Arrays.asList(1, 4));
        
        String newCurrent = generator.fillOneBlank(current, original, remaining);
        
        assertEquals(1, remaining.size());
        // EITHER pos 1 or 4 is filled
        assertTrue(newCurrent.equals("hell_") || newCurrent.equals("h_llo"));
    }
}

package com.manorrock.assistant.cli.issue;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Priority enum.
 */
class PriorityTest {

    @Test
    void testFromStringWithExactValues() {
        assertEquals(Priority.HIGHEST, Priority.fromString("HIGHEST"));
        assertEquals(Priority.HIGH, Priority.fromString("HIGH"));
        assertEquals(Priority.MEDIUM, Priority.fromString("MEDIUM"));
        assertEquals(Priority.LOW, Priority.fromString("LOW"));
        assertEquals(Priority.LOWEST, Priority.fromString("LOWEST"));
    }
    
    @Test
    void testFromStringWithCaseInsensitivity() {
        assertEquals(Priority.HIGHEST, Priority.fromString("highest"));
        assertEquals(Priority.HIGH, Priority.fromString("High"));
        assertEquals(Priority.MEDIUM, Priority.fromString("medium"));
        assertEquals(Priority.LOW, Priority.fromString("low"));
        assertEquals(Priority.LOWEST, Priority.fromString("lowest"));
    }
    
    @Test
    void testFromStringWithCommonVariations() {
        // Test common variations
        assertEquals(Priority.HIGHEST, Priority.fromString("CRITICAL"));
        assertEquals(Priority.HIGHEST, Priority.fromString("P1"));
        assertEquals(Priority.HIGHEST, Priority.fromString("P0"));
        assertEquals(Priority.HIGHEST, Priority.fromString("1"));
        
        assertEquals(Priority.HIGH, Priority.fromString("MAJOR"));
        assertEquals(Priority.HIGH, Priority.fromString("P2"));
        assertEquals(Priority.HIGH, Priority.fromString("2"));
        
        assertEquals(Priority.MEDIUM, Priority.fromString("NORMAL"));
        assertEquals(Priority.MEDIUM, Priority.fromString("P3"));
        assertEquals(Priority.MEDIUM, Priority.fromString("3"));
        
        assertEquals(Priority.LOW, Priority.fromString("MINOR"));
        assertEquals(Priority.LOW, Priority.fromString("P4"));
        assertEquals(Priority.LOW, Priority.fromString("4"));
        
        assertEquals(Priority.LOWEST, Priority.fromString("TRIVIAL"));
        assertEquals(Priority.LOWEST, Priority.fromString("P5"));
        assertEquals(Priority.LOWEST, Priority.fromString("5"));
    }
    
    @Test
    void testFromStringWithWhitespace() {
        // Test with leading/trailing whitespace
        assertEquals(Priority.HIGH, Priority.fromString(" HIGH "));
        assertEquals(Priority.MEDIUM, Priority.fromString("  MEDIUM  "));
    }
    
    @Test
    void testFromStringWithNull() {
        // Should return MEDIUM for null input
        assertEquals(Priority.MEDIUM, Priority.fromString(null));
    }
    
    @Test
    void testFromStringWithEmptyOrBlank() {
        // Should return MEDIUM for empty or blank input
        assertEquals(Priority.MEDIUM, Priority.fromString(""));
        assertEquals(Priority.MEDIUM, Priority.fromString("   "));
    }
    
    @Test
    void testFromStringWithUnrecognizedInput() {
        // Should return MEDIUM for unrecognized input
        assertEquals(Priority.MEDIUM, Priority.fromString("not-a-priority"));
        assertEquals(Priority.MEDIUM, Priority.fromString("unknown"));
    }
}
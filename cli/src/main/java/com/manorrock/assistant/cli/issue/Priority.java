package com.manorrock.assistant.cli.issue;

/**
 * Represents the priority level of an issue.
 * <p>
 * This enum provides a standard set of priority levels that can be used
 * to categorize issues by importance and urgency.
 * </p>
 * 
 * @author Manorrock Assistant
 */
public enum Priority {
    /**
     * Highest priority, requiring immediate attention.
     */
    HIGHEST,
    
    /**
     * High priority, should be addressed soon.
     */
    HIGH,
    
    /**
     * Medium priority, the default level for most issues.
     */
    MEDIUM,
    
    /**
     * Low priority, can be addressed when time permits.
     */
    LOW,
    
    /**
     * Lowest priority, nice to have but not critical.
     */
    LOWEST;
    
    /**
     * Parses a string to return the corresponding priority level.
     * The parsing is case-insensitive and tolerant of minor variations.
     * 
     * @param text The text to parse
     * @return The matching priority level, or MEDIUM if no match is found
     */
    public static Priority fromString(String text) {
        if (text == null || text.isBlank()) {
            return MEDIUM;
        }
        
        String normalized = text.trim().toUpperCase();
        
        // Handle common variations
        if (normalized.startsWith("HIGHEST") || normalized.startsWith("CRITICAL") || 
            normalized.equals("P0") || normalized.equals("P1") || normalized.equals("1")) {
            return HIGHEST;
        } else if (normalized.startsWith("HIGH") || normalized.startsWith("MAJOR") || 
                  normalized.equals("P2") || normalized.equals("2")) {
            return HIGH;
        } else if (normalized.startsWith("MEDIUM") || normalized.startsWith("NORMAL") || 
                  normalized.equals("P3") || normalized.equals("3")) {
            return MEDIUM;
        } else if (normalized.startsWith("LOW") && !normalized.startsWith("LOWEST") || 
                  normalized.startsWith("MINOR") || 
                  normalized.equals("P4") || normalized.equals("4")) {
            return LOW;
        } else if (normalized.startsWith("LOWEST") || normalized.startsWith("TRIVIAL") || 
                  normalized.equals("P5") || normalized.equals("5")) {
            return LOWEST;
        }
        
        // Default to medium priority
        return MEDIUM;
    }
}
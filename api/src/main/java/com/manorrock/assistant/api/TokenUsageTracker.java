package com.manorrock.assistant.api;

import java.util.Map;

/**
 * Interface for tracking token usage across LLM interactions.
 */
public interface TokenUsageTracker {

    /**
     * Record token usage for a specific model interaction
     * 
     * @param modelName Name of the LLM model used
     * @param promptTokens Number of tokens in the prompt
     * @param completionTokens Number of tokens in the completion
     * @param totalTokens Total tokens used
     */
    void recordUsage(String modelName, int promptTokens, int completionTokens, int totalTokens);
    
    /**
     * Record token usage using a map of usage data
     * 
     * @param modelName Name of the LLM model used
     * @param usageData Map containing usage metrics (promptTokens, completionTokens, totalTokens)
     */
    void recordUsage(String modelName, Map<String, Integer> usageData);
    
    /**
     * Get the total number of tokens used across all models
     * 
     * @return Total token count
     */
    int getTotalTokens();
    
    /**
     * Get token usage statistics by model
     * 
     * @return Map of model names to their token usage statistics
     */
    Map<String, Map<String, Object>> getUsageStatistics();
    
    /**
     * Reset all token usage statistics
     */
    void resetStatistics();
    
    /**
     * Export usage data to specified format
     * 
     * @param format Format for export (e.g. "json", "csv")
     * @return String representation of the exported data
     */
    String exportData(String format);
}

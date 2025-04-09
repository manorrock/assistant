package com.manorrock.assistant.core;

import com.manorrock.assistant.api.TokenUsageTracker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core implementation of the TokenUsageTracker interface.
 * Tracks token usage across different models and provides analytics.
 */
public class CoreTokenUsageTracker implements TokenUsageTracker {

    /**
     * Maps model names to their usage statistics
     */
    private final Map<String, Map<String, Object>> modelStats;
    
    /**
     * Keeps track of total tokens used
     */
    private final AtomicInteger totalTokens;
    
    /**
     * JSON mapper for exporting data
     */
    private final ObjectMapper jsonMapper;
    
    /**
     * CSV mapper for exporting data
     */
    private final CsvMapper csvMapper;
    
    /**
     * Constructor. Initializes the token usage tracker.
     */
    public CoreTokenUsageTracker() {
        this.modelStats = new ConcurrentHashMap<>();
        this.totalTokens = new AtomicInteger(0);
        this.jsonMapper = new ObjectMapper();
        this.csvMapper = new CsvMapper();
    }
    
    @Override
    public void recordUsage(String modelName, int promptTokens, int completionTokens, int totalTokens) {
        Map<String, Object> modelData = modelStats.computeIfAbsent(modelName, k -> initializeModelStats());
        
        // Update token counts
        updateTokenCounts(modelData, promptTokens, completionTokens, totalTokens);
        
        // Update total token count
        this.totalTokens.addAndGet(totalTokens);
        
        // Update timestamp of last interaction
        modelData.put("lastUsed", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
    
    @Override
    public void recordUsage(String modelName, Map<String, Integer> usageData) {
        int promptTokens = usageData.getOrDefault("promptTokens", 0);
        int completionTokens = usageData.getOrDefault("completionTokens", 0);
        int totalTokens = usageData.getOrDefault("totalTokens", promptTokens + completionTokens);
        
        recordUsage(modelName, promptTokens, completionTokens, totalTokens);
    }
    
    @Override
    public int getTotalTokens() {
        return totalTokens.get();
    }
    
    @Override
    public Map<String, Map<String, Object>> getUsageStatistics() {
        return new HashMap<>(modelStats);
    }
    
    @Override
    public void resetStatistics() {
        modelStats.clear();
        totalTokens.set(0);
    }
    
    @Override
    public String exportData(String format) {
        try {
            if ("csv".equalsIgnoreCase(format)) {
                // Prepare flattened data for CSV export
                Map<String, Object> flatData = new HashMap<>();
                flatData.put("exportDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                flatData.put("totalTokens", totalTokens.get());
                
                for (Map.Entry<String, Map<String, Object>> entry : modelStats.entrySet()) {
                    String modelName = entry.getKey();
                    Map<String, Object> stats = entry.getValue();
                    
                    flatData.put(modelName + "_totalCalls", stats.get("totalCalls"));
                    flatData.put(modelName + "_totalPromptTokens", stats.get("totalPromptTokens"));
                    flatData.put(modelName + "_totalCompletionTokens", stats.get("totalCompletionTokens"));
                    flatData.put(modelName + "_totalTokens", stats.get("totalTokens"));
                    flatData.put(modelName + "_lastUsed", stats.get("lastUsed"));
                }
                
                // Build schema by adding each column individually
                CsvSchema.Builder schemaBuilder = CsvSchema.builder()
                        .addColumn("exportDate")
                        .addColumn("totalTokens");
                
                // Add each model stat as a column
                for (String columnName : flatData.keySet()) {
                    if (!columnName.equals("exportDate") && !columnName.equals("totalTokens")) {
                        schemaBuilder.addColumn(columnName);
                    }
                }
                
                CsvSchema schema = schemaBuilder.build().withHeader();
                
                return csvMapper.writer(schema).writeValueAsString(flatData);
            } else {
                // Default to JSON
                Map<String, Object> exportData = new HashMap<>();
                exportData.put("exportDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                exportData.put("totalTokens", totalTokens.get());
                exportData.put("modelStats", modelStats);
                
                return jsonMapper.writeValueAsString(exportData);
            }
        } catch (Exception e) {
            return "Error exporting data: " + e.getMessage();
        }
    }
    
    /**
     * Initialize statistics for a new model
     * 
     * @return Map with initialized statistics
     */
    private Map<String, Object> initializeModelStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCalls", 0);
        stats.put("totalPromptTokens", 0);
        stats.put("totalCompletionTokens", 0);
        stats.put("totalTokens", 0);
        stats.put("firstUsed", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return stats;
    }
    
    /**
     * Update token counts for a specific model
     * 
     * @param modelData The model's statistics map
     * @param promptTokens Number of prompt tokens
     * @param completionTokens Number of completion tokens
     * @param totalTokens Total tokens (may not equal promptTokens + completionTokens)
     */
    private void updateTokenCounts(Map<String, Object> modelData, int promptTokens, int completionTokens, int totalTokens) {
        // Increment call count
        modelData.put("totalCalls", ((Number) modelData.get("totalCalls")).intValue() + 1);
        
        // Update token counts
        modelData.put("totalPromptTokens", ((Number) modelData.get("totalPromptTokens")).intValue() + promptTokens);
        modelData.put("totalCompletionTokens", ((Number) modelData.get("totalCompletionTokens")).intValue() + completionTokens);
        modelData.put("totalTokens", ((Number) modelData.get("totalTokens")).intValue() + totalTokens);
    }
}

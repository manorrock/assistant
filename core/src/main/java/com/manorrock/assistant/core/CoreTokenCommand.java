package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.TokenUsageTracker;
import com.manorrock.assistant.api.LlmManager;

import java.util.Map;

/**
 * Command for managing token usage information.
 * 
 * <p>
 * This command allows users to view token usage statistics, toggle the display of
 * token usage during conversations, and reset token usage statistics.
 * </p>
 */
public class CoreTokenCommand implements Command {

    /**
     * The core assistant.
     */
    private final CoreAssistant assistant;
    
    /**
     * Constructor.
     *
     * @param assistant the core assistant
     */
    public CoreTokenCommand(CoreAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public String execute(String command) {
        if (command == null || command.trim().isEmpty()) {
            return getUsageStatistics();
        }
        
        String[] parts = command.trim().split("\\s+", 2);
        String operation = parts[0].toLowerCase();
        
        switch (operation) {
            case "show":
                assistant.setShowTokenUsage(true);
                return "Token usage display is now enabled. Usage statistics will be shown after each conversation.";
            case "hide":
                assistant.setShowTokenUsage(false);
                return "Token usage display is now disabled.";
            case "reset":
                resetStatistics();
                return "Token usage statistics have been reset.";
            case "export":
                String format = parts.length > 1 ? parts[1].toLowerCase() : "text";
                return exportData(format);
            default:
                return getHelpText();
        }
    }
    
    /**
     * Check if token usage should be displayed after conversations.
     * 
     * @return true if token usage should be displayed, false otherwise
     */
    public boolean shouldShowTokenUsage() {
        return assistant.shouldShowTokenUsage();
    }
    
    /**
     * Get the current token usage statistics.
     * 
     * @return formatted string with token usage statistics
     */
    private String getUsageStatistics() {
        TokenUsageTracker tracker = getTokenTracker();
        if (tracker == null) {
            return "Token tracking is not available.";
        }
        
        StringBuilder stats = new StringBuilder("Token Usage Statistics:\n\n");
        Map<String, Map<String, Object>> usageByModel = tracker.getUsageStatistics();
        
        if (usageByModel.isEmpty()) {
            stats.append("No token usage recorded yet.");
            return stats.toString();
        }
        
        int totalPromptTokens = 0;
        int totalCompletionTokens = 0;
        int grandTotal = 0;
        
        // Per-model statistics
        stats.append("Per-model statistics:\n");
        for (Map.Entry<String, Map<String, Object>> entry : usageByModel.entrySet()) {
            String modelName = entry.getKey();
            Map<String, Object> modelStats = entry.getValue();
            
            int promptTokens = ((Number) modelStats.getOrDefault("promptTokens", 0)).intValue();
            int completionTokens = ((Number) modelStats.getOrDefault("completionTokens", 0)).intValue();
            int totalTokens = ((Number) modelStats.getOrDefault("totalTokens", 0)).intValue();
            
            totalPromptTokens += promptTokens;
            totalCompletionTokens += completionTokens;
            grandTotal += totalTokens;
            
            stats.append(String.format("  %s:\n", modelName));
            stats.append(String.format("    Prompt tokens: %,d\n", promptTokens));
            stats.append(String.format("    Completion tokens: %,d\n", completionTokens));
            stats.append(String.format("    Total tokens: %,d\n", totalTokens));
        }
        
        // Overall statistics
        stats.append("\nOverall statistics:\n");
        stats.append(String.format("  Total prompt tokens: %,d\n", totalPromptTokens));
        stats.append(String.format("  Total completion tokens: %,d\n", totalCompletionTokens));
        stats.append(String.format("  Grand total tokens: %,d\n", grandTotal));
        
        stats.append("\nUse 'token show' to display usage after each conversation.");
        stats.append("\nUse 'token hide' to hide usage display.");
        stats.append("\nUse 'token reset' to reset statistics.");
        
        return stats.toString();
    }
    
    /**
     * Reset the token usage statistics.
     */
    private void resetStatistics() {
        TokenUsageTracker tracker = getTokenTracker();
        if (tracker != null) {
            tracker.resetStatistics();
        }
    }
    
    /**
     * Export usage data in the specified format.
     * 
     * @param format the format to export data in
     * @return the exported data
     */
    private String exportData(String format) {
        TokenUsageTracker tracker = getTokenTracker();
        if (tracker == null) {
            return "Token tracking is not available.";
        }
        
        return tracker.exportData(format);
    }
    
    /**
     * Get the token usage tracker from the LlmManager.
     * 
     * @return the token usage tracker, or null if not available
     */
    private TokenUsageTracker getTokenTracker() {
        LlmManager llmManager = assistant.getLlmManager();
        if (llmManager instanceof CoreLlmManager) {
            return ((CoreLlmManager) llmManager).getTokenTracker();
        }
        return null;
    }
    
    /**
     * Get help text for the token command.
     * 
     * @return help text
     */
    private String getHelpText() {
        StringBuilder help = new StringBuilder("Token Usage Command\n\n");
        help.append("Usage:\n");
        help.append("  token              - Display current token usage statistics\n");
        help.append("  token show         - Enable displaying token usage after each conversation\n");
        help.append("  token hide         - Disable displaying token usage after conversations\n");
        help.append("  token reset        - Reset token usage statistics\n");
        help.append("  token export [fmt] - Export usage data in the specified format (text, json, csv)\n");
        
        return help.toString();
    }
    
    @Override
    public String getDescription() {
        return "Manages token usage tracking and displays statistics about LLM token consumption.";
    }
    
    @Override
    public String getShortDescription() {
        return "View and manage token usage statistics";
    }
}

package com.manorrock.assistant.core;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.CommandRegistry;
import com.manorrock.assistant.api.Llm;
import com.manorrock.assistant.api.LlmManager;
import com.manorrock.assistant.api.TokenUsageTracker;
import com.manorrock.assistant.api.ToolManager;
import com.manorrock.assistant.api.AgentManager;

/**
 * The Core Assistant.
 * 
 * <p>
 * This class implements the core functionality needed by all of our Assistant
 * implementations. Its main purposes is to process messages and it does so
 * by using the LLMs, commands and tools available to it.
 * </p>
 */
public class CoreAssistant implements Assistant {

    /**
     * Stores the name of the active LLM which will be used to process the prompts.
     */
    private String activeLlm;

    /**
     * Stores the command registry.
     */
    private CommandRegistry commandRegistry;

    /**
     * Stores the llm manager.
     */
    private LlmManager llmManager;

    /**
     * Stores the tool manager.
     */
    private ToolManager toolManager;

    /**
     * Stores the agent manager.
     */
    private AgentManager agentManager;

    /**
     * Stores the current context message
     */
    private String currentContext;
    
    /**
     * Flag to determine if token usage should be displayed after each conversation.
     */
    private boolean showTokenUsage = false;

    /**
     * Constructor.
     */
    public CoreAssistant() {
        activeLlm = "llama3.2";
        llmManager = new CoreLlmManager(this);
        llmManager.registerLlm("llama3.2", new CoreLlm(llmManager));
        commandRegistry = new CoreCommandRegistry(this);
        toolManager = new CoreToolManager(this);
    }
    
    /**
     * Get the active LLM.
     * 
     * @return the active LLM
     */
    public String getActiveLlm() {
        return activeLlm;
    }

    /**
     * Get the command registry.
     * 
     * @return the command registry
     */
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * Get the LLM manager.
     * 
     * @return the LLM manager
     */
    public LlmManager getLlmManager() {
        return llmManager;
    }

    /**
     * Get the tool manager.
     * 
     * @return the tool manager
     */
    public ToolManager getToolManager() {
        return toolManager;
    }

        /**
         * Get the agent manager.
         *
         * @return the agent manager
         */
        public AgentManager getAgentManager() {
            return agentManager;
        }

    /**
     * Set the active LLM.
     * 
     * @param activeLlm the active LLM
     */
    public void setActiveLlm(String activeLlm) {
        this.activeLlm = activeLlm;
    }

    /**
     * Set the command registry
     * 
     * @param commandRegistry the command registry
     */
    public void setCommandRegistry(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    /**
     * Set the tool manager.
     * 
     * @param toolManager the tool manager
     */
    public void setToolManager(ToolManager toolManager) {
        this.toolManager = toolManager;
    }

        /**
         * Set the agent manager.
         *
         * @param agentManager the agent manager
         */
        public void setAgentManager(AgentManager agentManager) {
            this.agentManager = agentManager;
        }

    @Override
    public CompletableFuture<AssistantMessage> sendMessage(AssistantMessage message) {
        return CompletableFuture.supplyAsync(() -> processMessage(message));
    }

    @Override
    public AssistantMessage processMessage(AssistantMessage message) {
        String content = message.getContent();
        
        // Check if the message is a command (starts with /)
        if (content != null && content.startsWith("/")) {
            return processCommand(content);
        } else {
            return processPrompt(message);
        }
    }
    
    /**
     * Process a command message.
     * 
     * @param commandText the command text including the "/" prefix
     * @return the response message
     */
    private AssistantMessage processCommand(String commandText) {
        // Extract command name and arguments
        int spaceIndex = commandText.indexOf(' ', 1);
        String commandName;
        String commandArgs;
        
        if (spaceIndex > 0) {
            commandName = commandText.substring(1, spaceIndex);
            commandArgs = commandText.substring(spaceIndex + 1).trim();
        } else {
            commandName = commandText.substring(1);
            commandArgs = "";
        }
        
        // Try to get the command from the registry
        Command command = commandRegistry.getCommand(commandName);
        if (command != null) {
            // Execute the command and wrap the result in an AssistantMessage
            String result = command.execute(commandArgs);
            return new CoreAssistantMessage(result);
        } else {
            return new CoreAssistantMessage("Unknown command: /" + commandName);
        }
    }
    
    /**
     * Process a prompt message using the active LLM.
     * 
     * @param message the message to process
     * @return the response message
     */
    private AssistantMessage processPrompt(AssistantMessage message) {
        if (activeLlm == null) {
            return new CoreAssistantMessage("Unable to determine which LLM to use");
        } 
        
        Llm llmToUse = llmManager.getLlm(activeLlm);
        if (llmToUse == null) {
            return new CoreAssistantMessage("Active LLM is not registered");
        }

        // If we have context, prepend it to the message
        String prompt = message.getContent();
        if (currentContext != null && !currentContext.trim().isEmpty()) {
            prompt = currentContext + "\n\n" + prompt;
        }
        
        String llmResponse = llmToUse.process(prompt);
        
        // Check if token usage display is enabled
        if (showTokenUsage) {
            // Get the token usage statistics
            String tokenUsageStats = getTokenUsageStats();
            if (tokenUsageStats != null) {
                llmResponse += "\n\n--- Token Usage ---\n" + tokenUsageStats;
            }
        }
        
        return new CoreAssistantMessage(llmResponse);
    }
    
    /**
     * Get the latest token usage statistics in a compact format.
     * 
     * @return a compact string with token usage statistics for the last interaction, or null if not available
     */
    private String getTokenUsageStats() {
        if (!(llmManager instanceof CoreLlmManager)) {
            return null;
        }
        
        TokenUsageTracker tracker = ((CoreLlmManager) llmManager).getTokenTracker();
        if (tracker == null) {
            return null;
        }
        
        Map<String, Map<String, Object>> usageByModel = tracker.getUsageStatistics();
        if (usageByModel.isEmpty()) {
            return "No token usage recorded yet.";
        }
        
        // For the sake of brevity in the conversation, just show the most recent usage for the active model
        if (usageByModel.containsKey(activeLlm)) {
            Map<String, Object> modelStats = usageByModel.get(activeLlm);
            int promptTokens = ((Number) modelStats.getOrDefault("totalPromptTokens", 0)).intValue();
            int completionTokens = ((Number) modelStats.getOrDefault("totalCompletionTokens", 0)).intValue();
            int totalTokens = ((Number) modelStats.getOrDefault("totalTokens", 0)).intValue();
            
            return String.format("Model: %s\nPrompt: %,d tokens\nCompletion: %,d tokens\nTotal: %,d tokens", 
                activeLlm, promptTokens, completionTokens, totalTokens);
        }
        
        return null;
    }

    /**
     * Set the current context message
     * 
     * @param context The context message to set
     */
    public void setContext(String context) {
        this.currentContext = context;
    }

    /**
     * Get the current context message
     * 
     * @return The current context message
     */
    public String getContext() {
        return currentContext;
    }

    /**
     * Clear the current context
     */
    public void clearContext() {
        this.currentContext = null;
    }

    /**
     * Set whether to show token usage after each conversation.
     *
     * @param showTokenUsage true to show token usage, false otherwise
     */
    public void setShowTokenUsage(boolean showTokenUsage) {
        this.showTokenUsage = showTokenUsage;
    }

    /**
     * Check if token usage should be displayed after conversations.
     *
     * @return true if token usage should be displayed, false otherwise
     */
    public boolean shouldShowTokenUsage() {
        return showTokenUsage;
    }

    /**
     * Reset the core assistant.
     */
    public void reset() {
        Llm llmToUse = llmManager.getLlm(activeLlm);
        if (llmToUse != null) {
            llmToUse.destroy();
            llmToUse.init();
        }
    }
}

package com.manorrock.assistant.api;

import java.util.Properties;

public interface Llm {
    /**
     * Set the ToolManager instance.
     * @param toolManager the ToolManager
     */
    default void setToolManager(ToolManager toolManager) {
        // Default implementation does nothing        
    }

    /**
     * Get the ToolManager instance.
     * @return the ToolManager
     */
    default ToolManager getToolManager() { 
        return null;
    }

    /**
     * Destroy the LLM.
     */
    void destroy();

    /**
     * Get the properties>
     * 
     * @return the properties
     */
    Properties getProperties();

    /**
     * Initialize the LLM.
     */
    void init();

    /**
     * Process a prompt.
     * 
     * @param prompt the prompt
     * @return the response
     */
    String process(String prompt);
    
    /**
     * Process a prompt with streaming response.
     * 
     * <p>
     * This method processes the prompt and returns the response tokens as they
     * are generated through the provided handler. This enables real-time token
     * delivery for more interactive user experiences.
     * </p>
     * 
     * @param prompt the prompt to process
     * @param handler the handler for streaming response tokens
     */
    void processStreaming(String prompt, LlmStreamingResponseHandler handler);

    /**
     * Set the LLM properties.
     * @param properties the properties
     */
    void setProperties(Properties properties);
}

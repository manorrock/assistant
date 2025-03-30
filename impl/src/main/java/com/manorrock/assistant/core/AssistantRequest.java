package com.manorrock.assistant.core;

/**
 * An assistant request.
 */
@Deprecated
public class AssistantRequest {
    
    /**
     * Stores the prompt.
     */
    private String prompt;
    
    /**
     * Constructor.
     */
    public AssistantRequest() {
    }
    
    /**
     * Constructor.
     * 
     * @param prompt the prompt
     */
    public AssistantRequest(String prompt) {
        this.prompt = prompt;
    }
    
    /**
     * Get the prompt.
     * 
     * @return the prompt
     */
    public String getPrompt() {
        return prompt;
    }
    
    /**
     * Set the prompt.
     * 
     * @param prompt the prompt
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}

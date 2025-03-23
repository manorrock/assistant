package com.manorrock.assistant.core;

/**
 * An assistant response.
 */
public class AssistantResponse {
    
    /**
     * Stores the response.
     */
    private String response;
    
    /**
     * Constructor.
     */
    public AssistantResponse() {
    }
    
    /**
     * Constructor.
     * 
     * @param response the response
     */
    public AssistantResponse(String response) {
        this.response = response;
    }
    
    /**
     * Get the response.
     * 
     * @return the response
     */
    public String getResponse() {
        return response;
    }
    
    /**
     * Set the response.
     * 
     * @param response the response
     */
    public void setResponse(String response) {
        this.response = response;
    }
}

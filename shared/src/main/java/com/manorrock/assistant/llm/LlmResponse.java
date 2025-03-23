package com.manorrock.assistant.llm;

/**
 * A response from the LLM.
 */
public class LlmResponse {

    /**
     * Stores the content.
     */
    private String content;

    /**
     * Get the content.
     * 
     * @return the content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Set the content.
     * 
     * @param content
     */
    public void setContent(String content) {
        this.content = content;
    }
}
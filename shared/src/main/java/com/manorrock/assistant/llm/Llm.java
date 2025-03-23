package com.manorrock.assistant.llm;

/**
 * The LLM that is the reusable component when interacting with a Large Language Model.
 */
public class Llm {
    
    /**
     * Stores the configuration.
     */
    private LlmConfiguration configuration;

    /**
     * Constructor.
     * 
     * @param configuration the configuration
     */
    public Llm(LlmConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Get the configuration.
     * 
     * @return the configuration
     */
    public LlmConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Set the configuration.
     * 
     * @param configuration the configuration
     */
    public void setConfiguration(LlmConfiguration configuration) {
        this.configuration = configuration;
    }
}

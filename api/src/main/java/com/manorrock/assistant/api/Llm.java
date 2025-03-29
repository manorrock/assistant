package com.manorrock.assistant.api;

import java.util.Properties;

public interface Llm {

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
     * Set the LLM properties.
     * @param properties the properties
     */
    void setProperties(Properties properties);
}

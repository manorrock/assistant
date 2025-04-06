package com.manorrock.assistant.api;

import java.util.List;
import java.util.Map;

/**
 * The LLM Manager.
 */
public interface LlmManager {

    /**
     * Get the LLM.
     * 
     * @param name the registered name of the LLM.
     * @return the LLM (or null if not found)
     */
    Llm getLlm(String name);

    /**
     * Get the LLMs.
     * 
     * @return the LLMs.
     */
    Map<String, Llm> getLlms();

    /**
     * Register an LLM.
     * 
     * @param name the registered name of the LLM.
     * @param llm the LLM
     */
    void registerLlm(String name, Llm llm);
    
    /**
     * Unregister an LLM.
     * 
     * @param name the registered name of the LLM to unregister.
     */
    void unregisterLlm(String name);

    /**
     * Get the assistant.
     * 
     * @return the assistant.
     */
    Assistant getAssistant();
}

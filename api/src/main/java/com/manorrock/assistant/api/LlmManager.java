package com.manorrock.assistant.api;

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
     * Register an LLM.
     * 
     * @param name the registered name of the LLM.
     * @param llm the LLM
     */
    void registerLlm(String name, Llm llm);
}

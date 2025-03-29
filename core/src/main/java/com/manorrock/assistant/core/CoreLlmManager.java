package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Llm;
import com.manorrock.assistant.api.LlmManager;
import java.util.HashMap;
import java.util.Map;

/**
 * The core LlmManager.
 * 
 * <p>
 * This class is responsible for managing the LLMs in the application.
 * It implements the LLMManager interface and provides methods to add, remove,
 * and retrieve LLMs.
 * </p>
 */
public class CoreLlmManager implements LlmManager {

    /**
     * Stores the LLMs.
     */
    private Map<String, Llm> llms = new HashMap<>();

    /**
     * Get the LLM.
     * 
     * @param name the name of the LLM
     * @return the LLM
     */
    public Llm getLLM(String name) {
        return llms.get(name);
    }
    /**
     * Get the LLMs.
     * 
     * @return the LLMs
     */
    public Map<String, Llm> getLLMs() {
        return llms;
    }

    /**
     * Register an LLM.
     * 
     * @param llm the LLM
     */
    @Override
    public void registerLlm(String name, Llm llm) {
        llms.put(name, llm);
    }

    /**
     * Unregister an LLM.
     * 
     * @param name the name of the LLM
     */
    public void unregisterLLM(String name) {
        llms.remove(name);
    }

    @Override
    public Llm getLlm(String name) {
        return llms.get(name);
    }
}

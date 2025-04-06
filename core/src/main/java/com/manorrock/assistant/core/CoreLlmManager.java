package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Assistant;
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
     * Stores the Assistant.
     */
    private Assistant assistant;

    /**
     * Constructor.
     * 
     * @param assistant the Assistant
     */
    public CoreLlmManager(Assistant assistant) {
        this.assistant = assistant;
    }

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
    public Map<String, Llm> getLlms() {
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
    @Override
    public void unregisterLlm(String name) {
        llms.remove(name);
    }

    /**
     * Unregister an LLM.
     * 
     * @param name the name of the LLM
     * @deprecated Use {@link #unregisterLlm(String)} instead
     */
    @Deprecated
    public void unregisterLLM(String name) {
        unregisterLlm(name);
    }

    @Override
    public Llm getLlm(String name) {
        return llms.get(name);
    }

    @Override
    public Assistant getAssistant() {
        return assistant;
    }
}

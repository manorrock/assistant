package com.manorrock.assistant.core;

import com.manorrock.assistant.llm.Llm;
import com.manorrock.assistant.llm.LlmConfiguration;
import com.manorrock.assistant.shared.CommandRegistry;

/**
 * The Assistant class is the reusable component of the Assistant application.
 */
public class Assistant {

    /**
     * Stores the command registry.
     */
    private CommandRegistry commandRegistry;

    /**
     * Stores the LLM.
     */
    private Llm llm;
    
    /**
     * Constructor.
     */
    public Assistant() {
        this.llm = new Llm(LlmConfiguration.defaultConfig());
        this.commandRegistry = new CommandRegistry();
    }

    /**
     * Get the command registry.
     * 
     * @return the command registry
     */
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * Get the LLM.
     */
    public Llm getLlm() {
        return llm;
    }

    /**
     * Set the command registry.
     * 
     * @param commandRegistry the command registry
     */
    public void setCommandRegistry(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    /**
     * Set the LLM.
     * 
     * @param llm the LLM
     */
    public void setLlm(Llm llm) {
        this.llm = llm;
    }
}

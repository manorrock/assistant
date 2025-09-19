package com.manorrock.assistant.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for managing assistants and their lifecycle.
 * Provides methods for registering, discovering, and managing assistants.
 */
public interface AssistantManager {
    /**
     * Gets a list of all active (enabled) assistants.
     *
     * @return list of active assistants
     */
    List<Assistant> getActiveAssistants();

    /**
     * Registers an assistant with the manager.
     *
     * @param assistant the assistant to register
     */
    void registerAssistant(Assistant assistant);

    /**
     * Unregisters an assistant with the given name.
     *
     * @param assistantName the name of the assistant to unregister
     * @return true if an assistant was unregistered, false otherwise
     */
    boolean unregisterAssistant(String assistantName);

    /**
     * Gets a list of all available assistants.
     *
     * @return list of all registered assistants
     */
    List<Assistant> getAvailableAssistants();

    /**
     * Finds an assistant by name.
     *
     * @param assistantName the name of the assistant to find
     * @return an Optional containing the assistant if found, empty otherwise
     */
    Optional<Assistant> findAssistant(String assistantName);

    /**
     * Executes an assistant with the given name and parameters.
     *
     * @param assistantName the name of the assistant to execute
     * @param parameters the parameters to pass to the assistant
     * @return the result of the assistant execution
     * @throws IllegalArgumentException if the assistant is not found or parameters are invalid
     */
    AssistantResult executeAssistant(String assistantName, Map<String, Object> parameters)
            throws IllegalArgumentException;

    /**
     * Generates a description of all available assistants in a format suitable for LLM consumption.
     *
     * @return a formatted string describing all assistants
     */
    String generateAssistantDescriptionsForLlm();

    /**
     * Enable a disabled assistant.
     *
     * @param assistantName name of the assistant to enable
     * @return true if the assistant was enabled, false if an error occurred
     */
    boolean enableAssistant(String assistantName);

    /**
     * Disable an assistant temporarily.
     *
     * @param assistantName name of the assistant to disable
     * @return true if the assistant was disabled, false if an error occurred
     */
    boolean disableAssistant(String assistantName);
}

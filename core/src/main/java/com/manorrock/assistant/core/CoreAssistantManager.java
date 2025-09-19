package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.AssistantManager;
import com.manorrock.assistant.api.AssistantResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The core AssistantManager.
 *
 * <p>
 * This class is responsible for managing assistants in the application.
 * It implements the AssistantManager interface and provides methods to add, remove,
 * and retrieve assistants.
 * </p>
 */
public class CoreAssistantManager implements AssistantManager {
    /**
     * Stores the list of assistants.
     */
    private final List<Assistant> assistants = new ArrayList<>();

    /**
     * Stores disabled assistant names.
     */
    private final List<String> disabledAssistants = new ArrayList<>();

    @Override
    public List<Assistant> getActiveAssistants() {
        return assistants.stream()
            .filter(assistant -> !disabledAssistants.contains(getName(assistant)))
            .collect(Collectors.toList());
    }

    @Override
    public void registerAssistant(Assistant assistant) {
        if (assistant == null) {
            throw new IllegalArgumentException("Assistant cannot be null");
        }
        String name = getName(assistant);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Assistant name cannot be null or empty");
        }
        if (findAssistant(name).isPresent()) {
            throw new IllegalArgumentException("Assistant with name '" + name + "' is already registered");
        }
        assistants.add(assistant);
    }

    @Override
    public boolean unregisterAssistant(String assistantName) {
        Optional<Assistant> optionalAssistant = findAssistant(assistantName);
        if (optionalAssistant.isPresent()) {
            disabledAssistants.remove(assistantName);
            return assistants.removeIf(a -> getName(a).equals(assistantName));
        }
        return false;
    }

    @Override
    public List<Assistant> getAvailableAssistants() {
        return new ArrayList<>(assistants);
    }

    @Override
    public Optional<Assistant> findAssistant(String assistantName) {
        if (assistantName == null || assistantName.trim().isEmpty()) {
            return Optional.empty();
        }
        return assistants.stream()
            .filter(a -> getName(a).equals(assistantName))
            .findFirst();
    }

    @Override
    public AssistantResult executeAssistant(String assistantName, Map<String, Object> parameters) throws IllegalArgumentException {
        Optional<Assistant> optionalAssistant = findAssistant(assistantName);
        if (optionalAssistant.isEmpty()) {
            throw new IllegalArgumentException("Assistant not found: " + assistantName);
        }
        if (disabledAssistants.contains(assistantName)) {
            return AssistantResult.failure("Assistant '" + assistantName + "' is currently disabled");
        }
        Assistant assistant = optionalAssistant.get();
        try {
            // For demonstration, assume parameters contains a message
            Object messageObj = parameters.get("message");
            if (!(messageObj instanceof com.manorrock.assistant.api.AssistantMessage)) {
                throw new IllegalArgumentException("Missing or invalid 'message' parameter");
            }
            com.manorrock.assistant.api.AssistantMessage message = (com.manorrock.assistant.api.AssistantMessage) messageObj;
            com.manorrock.assistant.api.AssistantMessage response = assistant.processMessage(message);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("response", response);
            return AssistantResult.success(resultData, "Assistant executed successfully");
        } catch (Exception e) {
            return AssistantResult.failure("Error executing assistant: " + e.getMessage());
        }
    }

    @Override
    public String generateAssistantDescriptionsForLlm() {
        List<Assistant> availableAssistants = getAvailableAssistants();
        if (availableAssistants.isEmpty()) {
            return "No assistants available.";
        }
        StringBuilder descriptions = new StringBuilder("Available assistants:\n\n");
        for (Assistant assistant : availableAssistants) {
            descriptions.append("Assistant: ").append(getName(assistant)).append("\n");
            descriptions.append("Description: ").append(getDescription(assistant)).append("\n\n");
        }
        return descriptions.toString();
    }

    @Override
    public boolean enableAssistant(String assistantName) {
        if (findAssistant(assistantName).isEmpty()) {
            return false;
        }
        disabledAssistants.remove(assistantName);
        return true;
    }

    @Override
    public boolean disableAssistant(String assistantName) {
        if (findAssistant(assistantName).isEmpty()) {
            return false;
        }
        if (!disabledAssistants.contains(assistantName)) {
            disabledAssistants.add(assistantName);
            return true;
        }
        return true;
    }

    /**
     * Helper to get the name of an assistant (assumes getName() method exists or override as needed).
     */
    private String getName(Assistant assistant) {
        try {
            return (String) assistant.getClass().getMethod("getName").invoke(assistant);
        } catch (Exception e) {
            return assistant.getClass().getSimpleName();
        }
    }

    /**
     * Helper to get the description of an assistant (assumes getDescription() method exists or override as needed).
     */
    private String getDescription(Assistant assistant) {
        try {
            return (String) assistant.getClass().getMethod("getDescription").invoke(assistant);
        } catch (Exception e) {
            return "No description available.";
        }
    }
}

package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Agent;
import com.manorrock.assistant.api.AgentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The core AgentManager.
 *
 * <p>
 * This class is responsible for managing agents in the application.
 * It implements the AgentManager interface and provides methods to add, remove,
 * and retrieve agents.
 * </p>
 */
public class CoreAgentManager implements AgentManager {

    /**
     * Stores the CoreAssistant instance.
     */
    @SuppressWarnings("unused")
    private final CoreAssistant assistant;

    /**
     * Stores the list of agents.
     */
    private final List<Agent> agents = new ArrayList<>();

    /**
     * Constructor.
     */
    public CoreAgentManager(CoreAssistant assistant) {
        this.assistant = assistant;
        // Register default agent(s) here if needed
    }

    @Override
    public void registerAgent(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("Agent cannot be null");
        }
        if (agent.getName() == null || agent.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Agent name cannot be null or empty");
        }
        if (findAgent(agent.getName()).isPresent()) {
            throw new IllegalArgumentException("Agent with name '" + agent.getName() + "' is already registered");
        }
        agents.add(agent);
    }

    @Override
    public boolean unregisterAgent(String agentName) {
        Optional<Agent> optionalAgent = findAgent(agentName);
        if (optionalAgent.isPresent()) {
            return agents.removeIf(a -> a.getName().equals(agentName));
        }
        return false;
    }

    @Override
    public List<Agent> getAvailableAgents() {
        return agents.stream().collect(Collectors.toList());
    }

    @Override
    public Optional<Agent> findAgent(String agentName) {
        if (agentName == null || agentName.trim().isEmpty()) {
            return Optional.empty();
        }
        return agents.stream().filter(agent -> agent.getName().equals(agentName)).findFirst();
    }
}

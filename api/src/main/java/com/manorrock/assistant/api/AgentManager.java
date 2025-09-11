package com.manorrock.assistant.api;

import java.util.List;
import java.util.Optional;

/**
 * Interface for managing agents and their execution.
 */
public interface AgentManager {
    /**
     * Register an agent.
     * @param agent the agent to register
     */
    void registerAgent(Agent agent);

    /**
     * Unregister an agent by name.
     * @param agentName the agent name
     * @return true if unregistered, false otherwise
     */
    boolean unregisterAgent(String agentName);

    /**
     * Get all available agents.
     * @return list of agents
     */
    List<Agent> getAvailableAgents();

    /**
     * Find an agent by name.
     * @param agentName the agent name
     * @return optional agent
     */
    Optional<Agent> findAgent(String agentName);
}

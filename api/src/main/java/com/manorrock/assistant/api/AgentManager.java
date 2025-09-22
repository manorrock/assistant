package com.manorrock.assistant.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for managing agents and their interactions.
 * Provides methods for registering, discovering, and interacting with agents.
 */
public interface AgentManager {
    /**
     * Registers an agent with the manager.
     *
     * @param agent the agent to register
     */
    void registerAgent(Agent agent);

    /**
     * Unregisters an agent with the given name.
     *
     * @param agentName the name of the agent to unregister
     * @return true if an agent was unregistered, false otherwise
     */
    boolean unregisterAgent(String agentName);

    /**
     * Gets a list of all available agents.
     *
     * @return list of all registered agents
     */
    List<Agent> getAvailableAgents();

    /**
     * Finds an agent by name.
     *
     * @param agentName the name of the agent to find
     * @return an Optional containing the agent if found, empty otherwise
     */
    Optional<Agent> findAgent(String agentName);

    /**
     * Starts a chat session with the specified agent.
     *
     * @param agentName the name of the agent
     * @param parameters initialization parameters
     * @return a unique session ID
     */
    String startAgentSession(String agentName, Map<String, Object> parameters);

    /**
     * Sends a message to the agent in the context of a session.
     *
     * @param agentName the name of the agent
     * @param sessionId the session ID
     * @param message the message to send
     * @return the agent's response
     */
    String sendAgentMessage(String agentName, String sessionId, String message);

    /**
     * Ends the chat session with the agent.
     *
     * @param agentName the name of the agent
     * @param sessionId the session ID
     */
    void endAgentSession(String agentName, String sessionId);
}

package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Agent;
import com.manorrock.assistant.api.AgentManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The core AgentManager.
 *
 * <p>
 * This class is responsible for managing agents in the application.
 * It implements the AgentManager interface and provides methods to add, remove,
 * and interact with agents.
 * </p>
 */
public class CoreAgentManager implements AgentManager {
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToAgent = new ConcurrentHashMap<>();

    @Override
    public void registerAgent(Agent agent) {
        agents.put(agent.getName(), agent);
    }

    @Override
    public boolean unregisterAgent(String agentName) {
        return agents.remove(agentName) != null;
    }

    @Override
    public List<Agent> getAvailableAgents() {
        return new ArrayList<>(agents.values());
    }

    @Override
    public Optional<Agent> findAgent(String agentName) {
        return Optional.ofNullable(agents.get(agentName));
    }

    @Override
    public String startAgentSession(String agentName, Map<String, Object> parameters) {
        Agent agent = agents.get(agentName);
        if (agent == null) throw new IllegalArgumentException("Agent not found: " + agentName);
        String sessionId = agent.startSession(parameters);
        sessionToAgent.put(sessionId, agentName);
        return sessionId;
    }

    @Override
    public String sendAgentMessage(String agentName, String sessionId, String message) {
        Agent agent = agents.get(agentName);
        if (agent == null) throw new IllegalArgumentException("Agent not found: " + agentName);
        // Pass an empty history for compatibility
        return agent.sendMessage(sessionId, message, Collections.emptyList()).getContent();
    }

    @Override
    public void endAgentSession(String agentName, String sessionId) {
        Agent agent = agents.get(agentName);
        if (agent != null) {
            agent.endSession(sessionId);
            sessionToAgent.remove(sessionId);
        }
    }
}

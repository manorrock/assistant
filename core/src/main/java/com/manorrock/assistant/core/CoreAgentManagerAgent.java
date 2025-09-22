package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Agent;
import com.manorrock.assistant.api.AgentManager;
import com.manorrock.assistant.api.AgentResponse;

import java.util.*;

/**
 * An agent that provides access to the AgentManager.
 *
 * <p>
 * This agent allows querying information about available agents and performing
 * operations on the agent registry through the Agent interface.
 * </p>
 */
public class CoreAgentManagerAgent implements Agent {
    @Override
    public void init() {}

    @Override
    public void destroy() {}

    @Override
    public Properties getProperties() { return new Properties(); }

    @Override
    public void setProperties(Properties properties) {}

    @Override
    public String process(String prompt) { return "Unsupported operation"; }

    @Override
    public void processStreaming(String prompt, com.manorrock.assistant.api.AgentStreamingResponseHandler handler) { handler.onComplete(); }

    @Override
    public void sendMessageStreaming(String sessionId, String message, java.util.List<com.manorrock.assistant.api.AgentMessage> history, com.manorrock.assistant.api.AgentStreamingResponseHandler handler) { handler.onComplete(); }
    private static final String NAME = "agent_manager";
    private static final String DESCRIPTION = "Provides access to the agent manager, allowing operations such as listing agents and chatting with them.";

    private final AgentManager agentManager;

    public CoreAgentManagerAgent(AgentManager agentManager) {
        this.agentManager = agentManager;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }


    @Override
    public String startSession(Map<String, Object> parameters) {
        // Not used for manager agent
        return UUID.randomUUID().toString();
    }


    @Override
    public AgentResponse sendMessage(String sessionId, String message, List<com.manorrock.assistant.api.AgentMessage> history) {
        // Not used for manager agent
        return new AgentResponse("Unsupported operation", Collections.emptyMap());
    }


    @Override
    public void endSession(String sessionId) {
        // Not used for manager agent
    }

    @Override
    public Map<String, Object> getSessionContext(String sessionId) {
        return Collections.emptyMap();
    }

    /**
     * Execute an operation on the agent manager.
     *
     * @param parameters the parameters for the operation
     * @return the result as a string
     */
    public String execute(Map<String, Object> parameters) {
        if (!parameters.containsKey("operation")) {
            return "Missing required parameter: operation";
        }
        String operation = parameters.get("operation").toString().toLowerCase();
        switch (operation) {
            case "list":
                return listAgents();
            case "chat":
                return chatWithAgent(parameters);
            default:
                return "Unknown operation: " + operation + ". Supported operations are: list, chat";
        }
    }

    private String listAgents() {
        List<Agent> agents = agentManager.getAvailableAgents();
        StringBuilder sb = new StringBuilder();
        sb.append("Available agents:\n");
        for (Agent agent : agents) {
            sb.append("- ").append(agent.getName()).append(": ").append(agent.getDescription()).append("\n");
        }
        return sb.toString();
    }

    private String chatWithAgent(Map<String, Object> parameters) {
        if (!parameters.containsKey("agentName") || !parameters.containsKey("message")) {
            return "Missing required parameters for chat operation: agentName, message";
        }
        String agentName = parameters.get("agentName").toString();
        String message = parameters.get("message").toString();
        Optional<Agent> agentOpt = agentManager.findAgent(agentName);
        if (agentOpt.isEmpty()) {
            return "Agent not found: " + agentName;
        }
        Agent agent = agentOpt.get();
    String sessionId = agent.startSession(Collections.emptyMap());
    AgentResponse response = agent.sendMessage(sessionId, message, Collections.emptyList());
    agent.endSession(sessionId);
    return response.getContent();
    }
}

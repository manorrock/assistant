package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Agent;
import com.manorrock.assistant.api.AgentMessage;
import com.manorrock.assistant.api.AgentResponse;
import com.manorrock.assistant.api.AgentStreamingResponseHandler;

import java.util.*;
import java.util.Properties;

/**
 * A simple echo agent implementation.
 */
public class CoreEchoAgent implements Agent {
    private Properties properties = new Properties();
    private final Map<String, Map<String, Object>> sessions = new HashMap<>();

    @Override
    public void init() {
        // No initialization needed for echo
    }

    @Override
    public void destroy() {
        sessions.clear();
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String getName() {
        return "echo";
    }

    @Override
    public String getDescription() {
        return "An agent that echoes back your message.";
    }

    @Override
    public String process(String prompt) {
        return "Echo: " + prompt;
    }

    @Override
    public void processStreaming(String prompt, AgentStreamingResponseHandler handler) {
        for (char c : ("Echo: " + prompt).toCharArray()) {
            handler.onToken(String.valueOf(c));
        }
        handler.onComplete();
    }

    @Override
    public String startSession(Map<String, Object> context) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, context != null ? new HashMap<>(context) : new HashMap<>());
        return sessionId;
    }

    @Override
    public AgentResponse sendMessage(String sessionId, String message, List<AgentMessage> history) {
        return new AgentResponse("Echo: " + message, Map.of("sessionId", sessionId));
    }

    @Override
    public void sendMessageStreaming(String sessionId, String message, List<AgentMessage> history, AgentStreamingResponseHandler handler) {
        String response = "Echo: " + message;
        for (char c : response.toCharArray()) {
            handler.onToken(String.valueOf(c));
        }
        handler.onComplete();
    }

    @Override
    public void endSession(String sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public Map<String, Object> getSessionContext(String sessionId) {
        return sessions.getOrDefault(sessionId, Collections.emptyMap());
    }
}

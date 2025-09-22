package com.manorrock.assistant.api;

import java.util.Map;

/**
 * Represents a response from an agent in a chat session.
 */
public class AgentResponse {
    private final String content;
    private final Map<String, Object> metadata;

    public AgentResponse(String content, Map<String, Object> metadata) {
        this.content = content;
        this.metadata = metadata;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}

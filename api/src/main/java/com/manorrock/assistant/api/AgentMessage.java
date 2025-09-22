package com.manorrock.assistant.api;

/**
 * Represents a message in a chat session with an agent.
 */
public class AgentMessage {
    private String role; // "user", "assistant", "system", etc.
    private String content;

    public AgentMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}

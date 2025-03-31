package com.manorrock.assistant.core;

import java.time.LocalDateTime;

import com.manorrock.assistant.api.AssistantMessage;

public class CoreAssistantMessage implements AssistantMessage {

    /**
     * Stores the message content.
     */
    private String content;

    /**
     * Stores the id.
     */
    private String id;

    /**
     * Stores the timestamp.
     */
    private LocalDateTime timestamp;

    /**
     * Stores the type.
     */
    private String type;

    /**
     * Constructor.
     */
    public CoreAssistantMessage() {
        this.content = "No message";
    }

    /**
     * Constructor.
     * 
     * @param content the message content
     */
    public CoreAssistantMessage(String content) {
        this.content = content;
    }

    /**
     * Get the message content.
     * 
     * @return the message content
     */
    public String getContent() {
        return content;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getType() {
        return type;
    }

    /**
     * Set the message content.
     * 
     * @param content the message content
     */
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "CoreAssistantMessage{" +
                "content='" + content + '\'' +
                ", id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", type='" + type + '\'' +
                '}';
    }
}

package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.AssistantMessage;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Default implementation of the AssistantMessage interface.
 */
public class DefaultAssistantMessage implements AssistantMessage {
    
    private String content;
    private String type;
    private LocalDateTime timestamp;
    private String id;
    
    /**
     * Default constructor.
     */
    public DefaultAssistantMessage() {
        this.timestamp = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
    }
    
    /**
     * Constructor with content and type.
     * 
     * @param content the message content
     * @param type the message type
     */
    public DefaultAssistantMessage(String content, String type) {
        this();
        this.content = content;
        this.type = type;
    }
    
    @Override
    public String getContent() {
        return content;
    }
    
    @Override
    public void setContent(String content) {
        this.content = content;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    @Override
    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    @Override
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void setId(String id) {
        this.id = id;
    }
}
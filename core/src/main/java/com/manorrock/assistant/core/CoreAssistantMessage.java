package com.manorrock.assistant.core;

import java.time.LocalDateTime;

import com.manorrock.assistant.api.AssistantMessage;

public class CoreAssistantMessage implements AssistantMessage {

    /**
     * Stores the message content.
     */
    private String content;

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

    /**
     * Set the message content.
     * 
     * @param content the message content
     */
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String getType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getType'");
    }

    @Override
    public void setType(String type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setType'");
    }

    @Override
    public LocalDateTime getTimestamp() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTimestamp'");
    }

    @Override
    public void setTimestamp(LocalDateTime timestamp) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTimestamp'");
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getId'");
    }

    @Override
    public void setId(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setId'");
    }
    
}

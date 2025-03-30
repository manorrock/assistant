package com.manorrock.assistant.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The ConversationContext interface.
 * 
 * <p>
 * This interface defines the contract for managing conversation state and flow control
 * across Assistant, LLM, and Command implementations. It maintains clear boundaries
 * between components while allowing for rich interactive features.
 * </p>
 */
public interface ConversationContext {
    
    /**
     * Get the current state of the conversation.
     * 
     * @return the current conversation state
     */
    ConversationState getState();
    
    /**
     * Set the current state of the conversation.
     * 
     * @param state the new conversation state
     */
    void setState(ConversationState state);
    
    /**
     * Get the conversation ID.
     * 
     * @return the conversation ID
     */
    String getConversationId();
    
    /**
     * Get the start time of the conversation.
     * 
     * @return the start time
     */
    LocalDateTime getStartTime();
    
    /**
     * Get the last activity time.
     * 
     * @return the last activity time
     */
    LocalDateTime getLastActivityTime();
    
    /**
     * Get metadata associated with the conversation.
     * 
     * @return map of metadata key-value pairs
     */
    Map<String, Object> getMetadata();
    
    /**
     * Set metadata for the conversation.
     * 
     * @param key the metadata key
     * @param value the metadata value
     */
    void setMetadata(String key, Object value);
    
    /**
     * Get pending user action if any.
     * 
     * @return Optional containing pending action if present
     */
    Optional<UserAction> getPendingAction();
    
    /**
     * Set pending user action.
     * 
     * @param action the pending user action
     */
    void setPendingAction(UserAction action);
    
    /**
     * Get conversation message history.
     * 
     * @return list of conversation messages
     */
    List<AssistantMessage> getHistory();
    
    /**
     * Add a message to the conversation history.
     * 
     * @param message the message to add
     */
    void addToHistory(AssistantMessage message);
    
    /**
     * Check if the conversation requires user input.
     * 
     * @return true if waiting for user input
     */
    boolean isWaitingForUser();
    
    /**
     * Resume the conversation after receiving user input.
     * 
     * @param input the user input
     */
    void resumeWithInput(String input);
    
    /**
     * Pause the conversation waiting for user input.
     * 
     * @param reason the reason for pausing
     */
    void pause(String reason);
    
    /**
     * Check if the conversation has timed out.
     * 
     * @return true if the conversation has timed out
     */
    boolean hasTimedOut();
}
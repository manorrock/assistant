package com.manorrock.assistant.api;

import java.util.List;
import java.util.Map;

/**
 * Defines the structure of actions that require user interaction.
 */
public interface UserAction {
    
    /**
     * Get the type of action required.
     * 
     * @return the action type
     */
    String getType();
    
    /**
     * Get the prompt to show to the user.
     * 
     * @return the prompt message
     */
    String getPrompt();
    
    /**
     * Get additional parameters for this action.
     * 
     * @return map of parameters
     */
    Map<String, Object> getParameters();
    
    /**
     * Get available choices if this is a choice-based action.
     * 
     * @return list of choices or empty list if not applicable
     */
    List<String> getChoices();
    
    /**
     * Check if this action has a timeout.
     * 
     * @return true if the action can timeout
     */
    boolean hasTimeout();
    
    /**
     * Get the timeout duration in seconds.
     * 
     * @return timeout in seconds, or -1 if no timeout
     */
    long getTimeoutSeconds();
    
    /**
     * Check if this action is required.
     * 
     * @return true if the action must be completed
     */
    boolean isRequired();
    
    /**
     * Get the default value if the action times out.
     * 
     * @return the default value, or null if no default
     */
    Object getDefaultValue();
}
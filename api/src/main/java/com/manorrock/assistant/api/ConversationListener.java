package com.manorrock.assistant.api;

import java.util.List;
import java.util.Map;
import java.nio.file.Path;

/**
 * Interface for handling conversation events and user interaction callbacks.
 */
public interface ConversationListener {
    
    /**
     * Called when user permission is required.
     * 
     * @param prompt the permission request prompt
     * @param details additional details about the permission request
     * @return true if permission granted, false otherwise
     */
    boolean onPermissionRequired(String prompt, Map<String, Object> details);
    
    /**
     * Called when file input is required.
     * 
     * @param prompt the file request prompt
     * @param allowedExtensions list of allowed file extensions
     * @param maxSize maximum file size in bytes
     * @return the selected file path, or null if cancelled
     */
    Path onFileRequired(String prompt, List<String> allowedExtensions, long maxSize);
    
    /**
     * Called when user needs to make a choice.
     * 
     * @param prompt the choice prompt
     * @param choices available choices
     * @param allowMultiple whether multiple selections are allowed
     * @return the selected choice(s)
     */
    List<String> onChoiceRequired(String prompt, List<String> choices, boolean allowMultiple);
    
    /**
     * Called when conversation state changes.
     * 
     * @param oldState previous state
     * @param newState new state
     * @param context current conversation context
     */
    void onStateChanged(ConversationState oldState, ConversationState newState, ConversationContext context);
    
    /**
     * Called when an error occurs in the conversation.
     * 
     * @param error the error message
     * @param details error details
     * @param context current conversation context
     */
    void onError(String error, Map<String, Object> details, ConversationContext context);
    
    /**
     * Called when conversation requires custom UI input.
     * 
     * @param prompt the UI prompt
     * @param inputType type of UI input required
     * @param parameters additional parameters for the UI
     * @return the user input
     */
    Object onCustomUiRequired(String prompt, String inputType, Map<String, Object> parameters);
    
    /**
     * Called when conversation is about to timeout.
     * 
     * @param context current conversation context
     * @param remainingSeconds seconds remaining before timeout
     */
    void onTimeoutWarning(ConversationContext context, long remainingSeconds);
}
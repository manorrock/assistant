package com.manorrock.assistant.api;

/**
 * Represents the various states a conversation can be in.
 */
public enum ConversationState {
    ACTIVE,                     // Normal conversation flow
    WAITING_FOR_INPUT,          // Waiting for user text input
    WAITING_FOR_FILE,           // Waiting for file attachment
    WAITING_FOR_PERMISSION,     // Waiting for user permission
    WAITING_FOR_CHOICE,         // Waiting for user to select from options
    PAUSED,                     // Conversation manually paused
    TIMED_OUT,                  // Conversation timed out waiting for response
    ERROR,                      // Conversation encountered error
    COMPLETED,                  // Conversation naturally completed
    CANCELLED                   // Conversation cancelled by user
}

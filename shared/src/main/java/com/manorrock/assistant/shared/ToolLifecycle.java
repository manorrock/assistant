package com.manorrock.assistant.shared;

/**
 * Represents the lifecycle states of a tool.
 */
public enum ToolLifecycle {
    /**
     * Tool is unregistered and not available for use.
     */
    UNREGISTERED,
    
    /**
     * Tool is registered and ready for use.
     */
    READY,
    
    /**
     * Tool is temporarily disabled.
     */
    DISABLED,
    
    /**
     * Tool has failed and needs attention.
     */
    FAILED,
    
    /**
     * Tool is registered with the system.
     */
    REGISTERED,
    
    /**
     * Tool is currently in use.
     */
    IN_USE,
    
    /**
     * Tool has encountered an error.
     */
    ERROR
}

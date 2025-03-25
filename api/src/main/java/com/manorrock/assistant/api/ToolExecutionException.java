package com.manorrock.assistant.api;

/**
 * Exception thrown when a tool execution fails.
 */
public class ToolExecutionException extends Exception {
    
    /**
     * Creates a new ToolExecutionException with the specified message.
     * 
     * @param message the error message
     */
    public ToolExecutionException(String message) {
        super(message);
    }
    
    /**
     * Creates a new ToolExecutionException with the specified message and cause.
     * 
     * @param message the error message
     * @param cause the cause of the exception
     */
    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

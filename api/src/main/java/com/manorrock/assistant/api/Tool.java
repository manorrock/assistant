package com.manorrock.assistant.api;

import java.util.List;
import java.util.Map;

/**
 * Interface defining a tool that can be executed by the LLM.
 * Tools provide specific functionality that can be invoked by the language model.
 */
public interface Tool {
    
    /**
     * Get the name of the tool.
     * 
     * @return the tool name
     */
    String getName();
    
    /**
     * Get the description of what the tool does.
     * This description will be provided to the LLM to understand the tool's purpose.
     * 
     * @return the tool description
     */
    String getDescription();
    
    /**
     * Get the list of parameters accepted by this tool.
     * 
     * @return list of parameter definitions
     */
    List<ToolParameter> getParameters();
    
    /**
     * Execute the tool with the provided parameters.
     * 
     * @param parameters a map of parameter names to their values
     * @return the result of the tool execution
     * @throws ToolExecutionException if the tool execution fails
     */
    ToolResult execute(Map<String, Object> parameters) throws ToolExecutionException;
    
    /**
     * Initialize the tool. Called after registration.
     * 
     * @return true if initialization successful
     */
    default boolean initialize() {
        return true;
    }
    
    /**
     * Clean up tool resources. Called before unregistration.
     */
    default void cleanup() {
    }
    
    /**
     * Get the current lifecycle state of the tool.
     * 
     * @return current lifecycle state
     */
    default ToolLifecycle getLifecycle() {
        return ToolLifecycle.READY;
    }
    
    /**
     * Set the current lifecycle state of the tool.
     * 
     * @param lifecycle new lifecycle state
     */
    default void setLifecycle(ToolLifecycle lifecycle) {
    }
}

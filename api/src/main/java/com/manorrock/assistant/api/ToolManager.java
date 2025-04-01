package com.manorrock.assistant.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for managing tools and their execution.
 * Provides methods for registering, discovering, and executing tools.
 */
public interface ToolManager {
    
    /**
     * Registers a tool with the manager.
     * 
     * @param tool the tool to register
     */
    void registerTool(Tool tool);
    
    /**
     * Unregisters a tool with the given name.
     * 
     * @param toolName the name of the tool to unregister
     * @return true if a tool was unregistered, false otherwise
     */
    boolean unregisterTool(String toolName);
    
    /**
     * Gets a list of all available tools.
     * 
     * @return list of all registered tools
     */
    List<Tool> getAvailableTools();
    
    /**
     * Finds a tool by name.
     * 
     * @param toolName the name of the tool to find
     * @return an Optional containing the tool if found, empty otherwise
     */
    Optional<Tool> findTool(String toolName);
    
    /**
     * Executes a tool with the given name and parameters.
     * 
     * @param toolName the name of the tool to execute
     * @param parameters the parameters to pass to the tool
     * @return the result of the tool execution
     * @throws IllegalArgumentException if the tool is not found or parameters are invalid
     */
    ToolResult executeTool(String toolName, Map<String, Object> parameters) 
            throws IllegalArgumentException;
    
    /**
     * Generates a description of all available tools in a format suitable for LLM consumption.
     * 
     * @return a formatted string describing all tools
     */
    String generateToolDescriptionsForLlm();
    
    /**
     * Enable a disabled tool.
     * 
     * @param toolName name of the tool to enable
     * @return true if the tool was enabled, false if an error occurred
     */
    boolean enableTool(String toolName);
    
    /**
     * Disable a tool temporarily.
     * 
     * @param toolName name of the tool to disable
     * @return true if the tool was disabled, false if an error occurred
     */
    boolean disableTool(String toolName);
}

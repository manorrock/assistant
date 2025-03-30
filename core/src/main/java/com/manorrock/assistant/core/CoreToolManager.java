package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolManager;
import com.manorrock.assistant.api.ToolResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The core ToolManager.
 * 
 * <p>
 * This class is responsible for managing the tools in the application.
 * It implements the ToolManager interface and provides methods to add, remove,
 * and retrieve tools.
 * </p>
 */
public class CoreToolManager implements ToolManager {

    /**
     * Stores the CoreAssistant instance.
     */
    private final CoreAssistant assistant;

    /**
     * Stores the list of tools.
     */
    private final List<Tool> tools = new ArrayList<>();
    
    /**
     * Stores disabled tool names.
     */
    private final List<String> disabledTools = new ArrayList<>();

    /**
     * Constructor.
     */
    public CoreToolManager(CoreAssistant assistant) {
        this.assistant = assistant;
        
        // Register the CoreToolManagerTool to expose the ToolManager itself as a tool
        CoreToolManagerTool toolManagerTool = new CoreToolManagerTool(this);
        registerTool(toolManagerTool);
    }

    @Override
    public void registerTool(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        
        if (tool.getName() == null || tool.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be null or empty");
        }
        
        if (tool.getDescription() == null || tool.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool description cannot be null or empty");
        }
        
        if (tool.getParameters() == null) {
            throw new IllegalArgumentException("Tool parameters cannot be null");
        }
        
        // Check for duplicate tool names
        if (findTool(tool.getName()).isPresent()) {
            throw new IllegalArgumentException("Tool with name '" + tool.getName() + "' is already registered");
        }
        
        // Initialize the tool
        boolean initialized = tool.initialize();
        if (!initialized) {
            throw new IllegalArgumentException("Failed to initialize tool: " + tool.getName());
        }
        
        // Add the tool to the list
        tools.add(tool);
    }

    @Override
    public boolean unregisterTool(String toolName) {
        Optional<Tool> optionalTool = findTool(toolName);
        if (optionalTool.isPresent()) {
            Tool tool = optionalTool.get();
            
            // Cleanup tool resources before unregistering
            tool.cleanup();
            
            // Remove from disabled list if present
            disabledTools.remove(toolName);
            
            // Remove the tool from the list
            return tools.removeIf(t -> t.getName().equals(toolName));
        }
        return false;
    }

    @Override
    public List<Tool> getAvailableTools() {
        // Return only tools that are not disabled
        return tools.stream()
            .filter(tool -> !disabledTools.contains(tool.getName()))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Tool> findTool(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Find the tool by name
        return tools.stream()
            .filter(tool -> tool.getName().equals(toolName))
            .findFirst();
    }

    @Override
    public ToolResult executeTool(String toolName, Map<String, Object> parameters) throws IllegalArgumentException {
        // Find the tool
        Optional<Tool> optionalTool = findTool(toolName);
        if (optionalTool.isEmpty()) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }
        
        Tool tool = optionalTool.get();
        
        // Check if the tool is disabled
        if (disabledTools.contains(toolName)) {
            return ToolResult.failure("Tool '" + toolName + "' is currently disabled");
        }
        
        // Validate required parameters
        Map<String, Object> validatedParams = new HashMap<>(parameters);
        tool.getParameters().forEach(param -> {
            String paramName = param.getName();
            Object value = validatedParams.get(paramName);
            
            // Check required parameters
            if (param.isRequired() && value == null) {
                throw new IllegalArgumentException("Missing required parameter: " + paramName);
            }
        });
        
        try {
            // Execute the tool
            return tool.execute(validatedParams);
        } catch (Exception e) {
            return ToolResult.failure("Error executing tool: " + e.getMessage());
        }
    }

    @Override
    public String generateToolDescriptionsForLlm() {
        List<Tool> availableTools = getAvailableTools();
        if (availableTools.isEmpty()) {
            return "No tools available.";
        }
        
        StringBuilder descriptions = new StringBuilder("Available tools:\n\n");
        
        for (Tool tool : availableTools) {
            descriptions.append("Tool: ").append(tool.getName()).append("\n");
            descriptions.append("Description: ").append(tool.getDescription()).append("\n");
            descriptions.append("Parameters:\n");
            
            if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
                tool.getParameters().forEach(param -> {
                    descriptions.append("  - ").append(param.getName())
                        .append(" (").append(param.getType()).append(")")
                        .append(param.isRequired() ? " [Required]" : "")
                        .append(": ").append(param.getDescription()).append("\n");
                });
            } else {
                descriptions.append("  None\n");
            }
            
            descriptions.append("\n");
        }
        
        return descriptions.toString();
    }

    @Override
    public void enableTool(String toolName) {
        // Check if tool exists
        if (findTool(toolName).isEmpty()) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }
        
        // Remove from disabled list if present
        disabledTools.remove(toolName);
    }

    @Override
    public void disableTool(String toolName) {
        // Check if tool exists
        if (findTool(toolName).isEmpty()) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }
        
        // Add to disabled list if not already present
        if (!disabledTools.contains(toolName)) {
            disabledTools.add(toolName);
        }
    }
}

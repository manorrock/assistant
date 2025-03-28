package com.manorrock.assistant.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolLifecycle;
import com.manorrock.assistant.api.ToolManager;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

/**
 * Default implementation of the ToolManager interface.
 * Provides functionality for registering, discovering, and executing tools.
 */
public class DefaultToolManager implements ToolManager {
    
    private final Map<String, Tool> tools;
    
    /**
     * Creates a new DefaultToolManager with no registered tools.
     */
    public DefaultToolManager() {
        this.tools = new ConcurrentHashMap<>();
    }
    
    @Override
    public void registerTool(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        
        String toolName = tool.getName();
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be null or blank");
        }
        
        tool.setLifecycle(ToolLifecycle.REGISTERED);
        if (tool.initialize()) {
            tool.setLifecycle(ToolLifecycle.READY);
            tools.put(toolName, tool);
        } else {
            tool.setLifecycle(ToolLifecycle.ERROR);
            throw new IllegalStateException("Tool initialization failed: " + tool.getName());
        }
    }
    
    @Override
    public boolean unregisterTool(String toolName) {
        Tool tool = tools.get(toolName);
        if (tool != null) {
            if (tool.getLifecycle() == ToolLifecycle.IN_USE) {
                throw new IllegalStateException("Cannot unregister tool that is in use: " + toolName);
            }
            tool.cleanup();
            tool.setLifecycle(ToolLifecycle.UNREGISTERED);
            return tools.remove(toolName) != null;
        }
        return false;
    }
    
    @Override
    public List<Tool> getAvailableTools() {
        return Collections.unmodifiableList(new ArrayList<>(tools.values()));
    }
    
    @Override
    public Optional<Tool> findTool(String toolName) {
        return Optional.ofNullable(tools.get(toolName));
    }
    
    @Override
    public ToolResult executeTool(String toolName, Map<String, Object> parameters) throws IllegalArgumentException {
        Tool tool = findTool(toolName)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));
        
        ToolLifecycle currentState = tool.getLifecycle();
        if (currentState != ToolLifecycle.READY) {
            return ToolResult.failure("Tool is not ready for execution. Current state: " + currentState);
        }
        
        // Validate required parameters
        List<String> missingParams = new ArrayList<>();
        for (ToolParameter param : tool.getParameters()) {
            if (param.isRequired() && 
                (parameters == null || !parameters.containsKey(param.getName()) || parameters.get(param.getName()) == null)) {
                missingParams.add(param.getName());
            }
        }
        
        if (!missingParams.isEmpty()) {
            return ToolResult.failure("Missing required parameters: " + String.join(", ", missingParams));
        }
        
        // Use empty map if parameters is null
        Map<String, Object> paramsToUse = parameters != null ? parameters : new HashMap<>();
        
        try {
            tool.setLifecycle(ToolLifecycle.IN_USE);
            ToolResult result = tool.execute(paramsToUse);
            tool.setLifecycle(ToolLifecycle.READY);
            return result;
        } catch (Exception e) {
            tool.setLifecycle(ToolLifecycle.ERROR);
            return ToolResult.failure("Tool execution failed: " + e.getMessage());
        }
    }
    
    @Override
    public void enableTool(String toolName) {
        Tool tool = tools.get(toolName);
        if (tool != null && tool.getLifecycle() == ToolLifecycle.DISABLED) {
            if (tool.initialize()) {
                tool.setLifecycle(ToolLifecycle.READY);
            } else {
                tool.setLifecycle(ToolLifecycle.ERROR);
            }
        }
    }
    
    @Override
    public void disableTool(String toolName) {
        Tool tool = tools.get(toolName);
        if (tool != null && tool.getLifecycle() == ToolLifecycle.READY) {
            tool.setLifecycle(ToolLifecycle.DISABLED);
        }
    }
    
    @Override
    public String generateToolDescriptionsForLlm() {
        StringBuilder descriptions = new StringBuilder();
        descriptions.append("[\n");
        
        boolean first = true;
        for (Tool tool : getAvailableTools()) {
            if (!first) {
                descriptions.append(",\n");
            }
            first = false;
            
            descriptions.append("  {\n");
            descriptions.append("    \"name\": \"").append(tool.getName()).append("\",\n");
            descriptions.append("    \"description\": \"").append(tool.getDescription()).append("\",\n");
            descriptions.append("    \"parameters\": [\n");
            
            List<ToolParameter> params = tool.getParameters();
            for (int i = 0; i < params.size(); i++) {
                ToolParameter param = params.get(i);
                descriptions.append("      {\n");
                descriptions.append("        \"name\": \"").append(param.getName()).append("\",\n");
                descriptions.append("        \"type\": \"").append(param.getType()).append("\",\n");
                descriptions.append("        \"description\": \"").append(param.getDescription()).append("\",\n");
                descriptions.append("        \"required\": ").append(param.isRequired()).append("\n");
                descriptions.append("      }");
                if (i < params.size() - 1) {
                    descriptions.append(",");
                }
                descriptions.append("\n");
            }
            
            descriptions.append("    ]\n");
            descriptions.append("  }");
        }
        
        descriptions.append("\n]");
        return descriptions.toString();
    }
}

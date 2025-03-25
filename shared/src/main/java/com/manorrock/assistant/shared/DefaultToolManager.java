package com.manorrock.assistant.shared;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolExecutionException;
import com.manorrock.assistant.api.ToolManager;
import com.manorrock.assistant.api.ToolResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A proxy implementation of the ToolManager for the shared module.
 * This delegates to the real DefaultToolManager from the tool module.
 */
public class DefaultToolManager implements ToolManager {
    
    private final ToolManager delegate;
    
    /**
     * Creates a new DefaultToolManager instance.
     */
    public DefaultToolManager() {
        this.delegate = new com.manorrock.assistant.tool.DefaultToolManager();
    }
    
    @Override
    public void registerTool(Tool tool) {
        delegate.registerTool(tool);
    }
    
    @Override
    public boolean unregisterTool(String toolName) {
        return delegate.unregisterTool(toolName);
    }
    
    @Override
    public List<Tool> getAvailableTools() {
        return delegate.getAvailableTools();
    }
    
    @Override
    public Optional<Tool> findTool(String toolName) {
        return delegate.findTool(toolName);
    }
    
    @Override
    public ToolResult executeTool(String toolName, Map<String, Object> parameters) 
            throws ToolExecutionException, IllegalArgumentException {
        return delegate.executeTool(toolName, parameters);
    }
    
    @Override
    public void enableTool(String toolName) {
        delegate.enableTool(toolName);
    }
    
    @Override
    public void disableTool(String toolName) {
        delegate.disableTool(toolName);
    }
    
    @Override
    public String generateToolDescriptionsForLlm() {
        return delegate.generateToolDescriptionsForLlm();
    }
}

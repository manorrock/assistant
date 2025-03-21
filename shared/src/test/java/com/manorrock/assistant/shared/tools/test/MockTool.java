package com.manorrock.assistant.shared.tools.test;

import com.manorrock.assistant.shared.Tool;
import com.manorrock.assistant.shared.ToolLifecycle;
import com.manorrock.assistant.shared.ToolParameter;
import com.manorrock.assistant.shared.ToolResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockTool implements Tool {
    
    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;
    private ToolLifecycle lifecycle = ToolLifecycle.REGISTERED;
    private boolean shouldFailInitialization = false;
    private boolean shouldFailExecution = false;
    private Map<String, Object> lastExecutionParameters;
    
    public MockTool(String name, String description) {
        this.name = name;
        this.description = description;
        this.parameters = new ArrayList<>();
        
        // Add some test parameters
        parameters.add(new ToolParameter(
            "requiredParam",
            "string",
            "A required parameter",
            true
        ));
        
        parameters.add(new ToolParameter(
            "optionalParam",
            "number", 
            "An optional parameter",
            false
        ));
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return parameters;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        this.lastExecutionParameters = new HashMap<>(parameters);
        
        if (shouldFailExecution) {
            throw new RuntimeException("Mock execution failure");
        }
        
        Map<String, Object> data = new HashMap<>(parameters);
        return ToolResult.success(data, "Mock tool executed successfully");
    }
    
    @Override
    public boolean initialize() {
        return !shouldFailInitialization;
    }
    
    @Override
    public void cleanup() {
        this.lastExecutionParameters = null;
    }
    
    @Override
    public ToolLifecycle getLifecycle() {
        return lifecycle;
    }
    
    @Override
    public void setLifecycle(ToolLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }
    
    public void setShouldFailInitialization(boolean shouldFail) {
        this.shouldFailInitialization = shouldFail;
    }
    
    public void setShouldFailExecution(boolean shouldFail) {
        this.shouldFailExecution = shouldFail;
    }
    
    public Map<String, Object> getLastExecutionParameters() {
        return lastExecutionParameters;
    }
}

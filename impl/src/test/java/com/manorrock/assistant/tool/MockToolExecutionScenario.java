package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Mock scenario for simulating tool execution sequences.
 */
public class MockToolExecutionScenario implements Tool {
    
    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;
    private final List<Function<Map<String, Object>, ToolResult>> scenarioSteps;
    private int currentStep;
    
    public MockToolExecutionScenario(String name, String description) {
        this.name = name;
        this.description = description;
        this.parameters = new ArrayList<>();
        this.scenarioSteps = new ArrayList<>();
        this.currentStep = 0;
    }
    
    public MockToolExecutionScenario addParameter(ToolParameter parameter) {
        parameters.add(parameter);
        return this;
    }
    
    public MockToolExecutionScenario addStep(Function<Map<String, Object>, ToolResult> step) {
        scenarioSteps.add(step);
        return this;
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
        if (currentStep >= scenarioSteps.size()) {
            return ToolResult.failure("Scenario completed - no more steps");
        }
        
        Function<Map<String, Object>, ToolResult> step = scenarioSteps.get(currentStep++);
        return step.apply(parameters);
    }
    
    public void reset() {
        currentStep = 0;
    }
}

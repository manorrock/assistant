package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.util.List;
import java.util.Map;

/**
 * Base class for tool implementations providing common functionality.
 */
public abstract class AbstractTool implements Tool {
    
    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;
    
    /**
     * Creates a new AbstractTool with the specified metadata.
     * 
     * @param name the tool name
     * @param description the tool description
     * @param parameters the tool parameters
     */
    protected AbstractTool(String name, String description, List<ToolParameter> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
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
    
    /**
     * Execute the tool with the provided parameters.
     * This implementation wraps the executeInternal method with exception handling.
     * 
     * @param parameters a map of parameter names to their values
     * @return the result of the tool execution
     */
    @Override
    public final ToolResult execute(Map<String, Object> parameters) {
        try {
            return executeInternal(parameters);
        } catch (Exception e) {
            return ToolResult.failure("Tool execution failed with exception: " + e.getMessage());
        }
    }
    
    /**
     * Internal execution method that may throw exceptions.
     * Implementations should perform their work here.
     * 
     * @param parameters a map of parameter names to their values
     * @return the result of the tool execution
     * @throws Exception if execution fails
     */
    protected abstract ToolResult executeInternal(Map<String, Object> parameters) throws Exception;
}

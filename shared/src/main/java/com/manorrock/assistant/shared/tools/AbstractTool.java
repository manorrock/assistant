package com.manorrock.assistant.shared.tools;

import com.manorrock.assistant.shared.Tool;
import com.manorrock.assistant.shared.ToolParameter;
import com.manorrock.assistant.shared.ToolResult;
import java.util.List;

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
}

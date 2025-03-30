package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolManager;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tool that provides access to the ToolManager.
 * 
 * <p>
 * This tool allows querying information about available tools and performing
 * operations on the tool registry through the Tool interface.
 * </p>
 */
public class CoreToolManagerTool implements Tool {

    /**
     * The name of this tool.
     */
    private static final String NAME = "tool_manager";

    /**
     * The description of this tool.
     */
    private static final String DESCRIPTION = "Provides access to the tool manager, allowing operations such as listing, enabling, and disabling tools";

    /**
     * The parameters accepted by this tool.
     */
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
        new ToolParameter("operation", "string", "The operation to perform (list, enable, disable, info)", true),
        new ToolParameter("toolName", "string", "The name of the tool to operate on (for enable, disable, info operations)", false)
    );

    /**
     * The tool manager instance.
     */
    private final ToolManager toolManager;

    /**
     * Constructor.
     *
     * @param toolManager the tool manager instance
     */
    public CoreToolManagerTool(ToolManager toolManager) {
        this.toolManager = toolManager;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public List<ToolParameter> getParameters() {
        return PARAMETERS;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        if (!parameters.containsKey("operation")) {
            return ToolResult.failure("Missing required parameter: operation");
        }

        String operation = parameters.get("operation").toString().toLowerCase();
        
        try {
            switch (operation) {
                case "list":
                    return listTools();
                case "enable":
                    return enableTool(parameters);
                case "disable":
                    return disableTool(parameters);
                case "info":
                    return getToolInfo(parameters);
                default:
                    return ToolResult.failure("Unknown operation: " + operation + 
                        ". Supported operations are: list, enable, disable, info");
            }
        } catch (Exception e) {
            return ToolResult.failure("Error executing operation: " + e.getMessage());
        }
    }

    /**
     * List all available tools.
     *
     * @return the result containing the list of available tools
     */
    private ToolResult listTools() {
        List<Tool> availableTools = toolManager.getAvailableTools();
        List<Map<String, Object>> toolsInfo = new ArrayList<>();
        
        for (Tool tool : availableTools) {
            Map<String, Object> toolInfo = new HashMap<>();
            toolInfo.put("name", tool.getName());
            toolInfo.put("description", tool.getDescription());
            toolsInfo.add(toolInfo);
        }
        
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("tools", toolsInfo);
        resultData.put("count", toolsInfo.size());
        
        return ToolResult.success(resultData, "Retrieved " + toolsInfo.size() + " available tools");
    }

    /**
     * Enable a tool.
     *
     * @param parameters the parameters containing the tool name
     * @return the result of the operation
     */
    private ToolResult enableTool(Map<String, Object> parameters) {
        if (!parameters.containsKey("toolName")) {
            return ToolResult.failure("Missing required parameter for enable operation: toolName");
        }
        
        String toolName = parameters.get("toolName").toString();
        
        // Check if the tool exists
        if (toolManager.findTool(toolName).isEmpty()) {
            return ToolResult.failure("Tool not found: " + toolName);
        }
        
        toolManager.enableTool(toolName);
        
        return ToolResult.success(
            Map.of("toolName", toolName, "status", "enabled"),
            "Tool '" + toolName + "' has been enabled"
        );
    }

    /**
     * Disable a tool.
     *
     * @param parameters the parameters containing the tool name
     * @return the result of the operation
     */
    private ToolResult disableTool(Map<String, Object> parameters) {
        if (!parameters.containsKey("toolName")) {
            return ToolResult.failure("Missing required parameter for disable operation: toolName");
        }
        
        String toolName = parameters.get("toolName").toString();
        
        // Check if the tool exists
        if (toolManager.findTool(toolName).isEmpty()) {
            return ToolResult.failure("Tool not found: " + toolName);
        }
        
        // Don't allow disabling this tool itself
        if (toolName.equals(NAME)) {
            return ToolResult.failure("Cannot disable the tool_manager tool");
        }
        
        toolManager.disableTool(toolName);
        
        return ToolResult.success(
            Map.of("toolName", toolName, "status", "disabled"),
            "Tool '" + toolName + "' has been disabled"
        );
    }

    /**
     * Get information about a specific tool.
     *
     * @param parameters the parameters containing the tool name
     * @return the result containing tool information
     */
    private ToolResult getToolInfo(Map<String, Object> parameters) {
        if (!parameters.containsKey("toolName")) {
            return ToolResult.failure("Missing required parameter for info operation: toolName");
        }
        
        String toolName = parameters.get("toolName").toString();
        
        return toolManager.findTool(toolName)
            .map(tool -> {
                Map<String, Object> toolInfo = new HashMap<>();
                toolInfo.put("name", tool.getName());
                toolInfo.put("description", tool.getDescription());
                
                // Get parameter information
                List<Map<String, Object>> parametersInfo = new ArrayList<>();
                for (ToolParameter param : tool.getParameters()) {
                    Map<String, Object> paramInfo = new HashMap<>();
                    paramInfo.put("name", param.getName());
                    paramInfo.put("type", param.getType());
                    paramInfo.put("description", param.getDescription());
                    paramInfo.put("required", param.isRequired());
                    parametersInfo.add(paramInfo);
                }
                
                toolInfo.put("parameters", parametersInfo);
                
                return ToolResult.success(toolInfo, "Tool information retrieved successfully");
            })
            .orElse(ToolResult.failure("Tool not found: " + toolName));
    }
}
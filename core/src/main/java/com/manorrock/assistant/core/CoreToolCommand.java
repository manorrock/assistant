package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core Tool command for the Core Assistant.
 * 
 * <p>
 * This command provides management and execution of available tools in the Core Assistant.
 * It allows users to list, get information about, and execute tools.
 * </p>
 */
public class CoreToolCommand implements Command {

    private final CoreAssistant assistant;

    /**
     * Constructor.
     *
     * @param assistant The core assistant instance
     */
    public CoreToolCommand(CoreAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public String execute(String input) {
        if (input == null || input.trim().isEmpty()) {
            // List available tools if no argument provided
            return listTools();
        }

        String[] parts = input.trim().split("\\s+", 2);
        String subCommand = parts[0].toLowerCase();

        switch (subCommand) {
            case "disable":
                boolean disabled = assistant.getToolManager().disableTool(parts[1].trim());
                return disabled ? "Tool disabled successfully" : "Failed to disable tool";
            case "enable":
                boolean enabled = assistant.getToolManager().enableTool(parts[1].trim());
                return enabled ? "Tool enabled successfully" : "Failed to enable tool";
            case "list":
                return listTools();

            case "execute":
                if (parts.length < 2) {
                    return "Error: Tool name required\n" +
                            "Usage: tool execute <tool-name> [param1=value1 param2=value2 ...]";
                }
                String[] execArgs = parts[1].trim().split("\\s+", 2);
                String toolName = execArgs[0];

                // Parse parameters if provided
                Map<String, Object> params = new HashMap<>();
                if (execArgs.length > 1) {
                    params = parseParameters(execArgs[1]);
                }

                return executeTool(toolName, params);

            case "info":
                if (parts.length < 2) {
                    return "Error: Tool name required\n" +
                            "Usage: tool info <tool-name>";
                }
                return getToolInfo(parts[1].trim());

            case "status":
                return getToolStatus();

            default:
                return "Unknown subcommand: " + subCommand + "\n" +
                        "Available subcommands: list, execute, info, status";
        }
    }

    /**
     * Get the current status of available tools.
     *
     * @return Status message
     */
    private String getToolStatus() {
        List<Tool> tools = assistant.getToolManager().getAvailableTools();
        return "Tool system status: " + tools.size() + " available tools";
    }

    /**
     * List all available tools.
     *
     * @return String representation of available tools
     */
    private String listTools() {
        List<Tool> tools = assistant.getToolManager().getAvailableTools();

        if (tools.isEmpty()) {
            return "No tools available";
        }

        StringBuilder result = new StringBuilder("Available tools:\n");
        for (Tool tool : tools) {
            result.append("  - ").append(tool.getName())
                    .append(": ").append(tool.getDescription()).append("\n");
        }

        result.append("\nUse '/tool info <tool-name>' for details about a specific tool.");
        result.append("\nUse '/tool execute <tool-name> [params]' to execute a tool.");

        return result.toString();
    }

    /**
     * Get detailed information about a specific tool.
     *
     * @param toolName Name of the tool
     * @return String representation of tool details
     */
    private String getToolInfo(String toolName) {
        var optionalTool = assistant.getToolManager().findTool(toolName);
        
        if (optionalTool.isPresent()) {
            Tool tool = optionalTool.get();
            StringBuilder result = new StringBuilder()
                    .append("Tool: ").append(tool.getName()).append("\n")
                    .append("Description: ").append(tool.getDescription()).append("\n")
                    .append("Parameters:\n");

            List<ToolParameter> params = tool.getParameters();
            if (params == null || params.isEmpty()) {
                result.append("  None\n");
            } else {
                for (ToolParameter param : params) {
                    result.append("  - ").append(param.getName())
                            .append(" (").append(param.getType()).append(")")
                            .append(param.isRequired() ? " [Required]" : "")
                            .append(": ").append(param.getDescription()).append("\n");
                }
            }

            result.append("\nUsage: /tool execute ").append(toolName).append(" [param1=value1 param2=value2 ...]");
            return result.toString();
        }

        return "Tool not found: " + toolName;
    }

    /**
     * Execute a tool with the given parameters.
     *
     * @param toolName   Name of the tool to execute
     * @param parameters Parameters to pass to the tool
     * @return String representation of the execution result
     */
    private String executeTool(String toolName, Map<String, Object> parameters) {
        try {
            ToolResult result = assistant.getToolManager().executeTool(toolName, parameters);

            StringBuilder output = new StringBuilder();
            output.append("Tool execution ").append(result.isSuccess() ? "succeeded" : "failed").append("\n");
            output.append("Message: ").append(result.getMessage()).append("\n");

            if (result.getData() != null && !result.getData().isEmpty()) {
                output.append("Result data:\n");
                for (Map.Entry<String, Object> entry : result.getData().entrySet()) {
                    output.append("  ").append(entry.getKey())
                            .append(": ").append(entry.getValue()).append("\n");
                }
            }

            return output.toString();
        } catch (Exception e) {
            return "Error executing tool: " + e.getMessage();
        }
    }

    /**
     * Parse parameters from a string in format "param1=value1 param2=value2".
     *
     * @param paramsString String containing parameters
     * @return Map of parameter names to values
     */
    private Map<String, Object> parseParameters(String paramsString) {
        Map<String, Object> params = new HashMap<>();
        String[] parts = paramsString.split("\\s+");

        for (String part : parts) {
            int eqIdx = part.indexOf('=');
            if (eqIdx > 0) {
                String name = part.substring(0, eqIdx);
                String value = part.substring(eqIdx + 1);
                params.put(name, value);
            }
        }

        return params;
    }

    @Override
    public String getDescription() {
        return "Manage and execute tools";
    }

    @Override
    public String getShortDescription() {
        return "Manage and execute tools";
    }
}
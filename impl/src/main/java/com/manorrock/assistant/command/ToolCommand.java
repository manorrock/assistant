package com.manorrock.assistant.command;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

/**
 * Command to interact with tool management system.
 */
public class ToolCommand implements Command {

    private final Supplier<List<Tool>> toolListSupplier;
    private final BiFunction<String, Map<String, Object>, ToolResult> toolExecutor;
    private final Consumer<Boolean> integrationToggler;
    private final BooleanSupplier integrationStatusSupplier;

    /**
     * Create a new tool command.
     *
     * @param toolListSupplier Function to retrieve the list of available tools
     * @param toolExecutor     Function to execute a tool with given parameters
     */
    public ToolCommand(Supplier<List<Tool>> toolListSupplier,
            BiFunction<String, Map<String, Object>, ToolResult> toolExecutor) {
        this(toolListSupplier, toolExecutor, null, null);
    }

    /**
     * Create a new tool command with LLM integration support.
     *
     * @param toolListSupplier          Function to retrieve the list of available
     *                                  tools
     * @param toolExecutor              Function to execute a tool with given
     *                                  parameters
     * @param integrationToggler        Function to toggle LLM tool integration
     * @param integrationStatusSupplier Function to get current integration status
     */
    public ToolCommand(Supplier<List<Tool>> toolListSupplier,
            BiFunction<String, Map<String, Object>, ToolResult> toolExecutor,
            Consumer<Boolean> integrationToggler,
            BooleanSupplier integrationStatusSupplier) {
        this.toolListSupplier = toolListSupplier;
        this.toolExecutor = toolExecutor;
        this.integrationToggler = integrationToggler;
        this.integrationStatusSupplier = integrationStatusSupplier;
    }

    @Override
    public String getDescription() {
        return "Manage and execute tools";
    }

    @Override
    public String executeToString(String input) {
        if (input == null || input.trim().isEmpty()) {
            // List available tools if no argument provided
            return listTools();
        }

        String[] parts = input.trim().split("\\s+", 2);
        String subCommand = parts[0].toLowerCase();

        switch (subCommand) {
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

            case "integration":
                return toggleToolIntegration(parts.length > 1 ? parts[1].trim() : null);

            case "status":
                return getToolIntegrationStatus();

            default:
                return "Unknown subcommand: " + subCommand + "\n" +
                        "Available subcommands: list, execute, info, integration, status";
        }
    }

    @Override
    public InputStream executeToStream(String input) {
        return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Toggle LLM tool integration.
     *
     * @param param Optional parameter (on/off)
     * @return Status message
     */
    private String toggleToolIntegration(String param) {
        if (integrationToggler == null || integrationStatusSupplier == null) {
            return "LLM tool integration is not supported in this environment";
        }

        boolean currentStatus = integrationStatusSupplier.getAsBoolean();
        boolean newStatus;

        if (param == null) {
            // Toggle current status
            newStatus = !currentStatus;
        } else if (param.equalsIgnoreCase("on")) {
            newStatus = true;
        } else if (param.equalsIgnoreCase("off")) {
            newStatus = false;
        } else {
            return "Invalid parameter. Use 'on', 'off', or no parameter to toggle.";
        }

        integrationToggler.accept(newStatus);

        return "LLM tool integration " + (newStatus ? "enabled" : "disabled") +
                " with " + toolListSupplier.get().size() + " available tools";
    }

    /**
     * Get the current status of LLM tool integration.
     *
     * @return Status message
     */
    private String getToolIntegrationStatus() {
        if (integrationStatusSupplier == null) {
            return "LLM tool integration status is not available in this environment";
        }

        boolean status = integrationStatusSupplier.getAsBoolean();
        return "LLM tool integration is currently " + (status ? "enabled" : "disabled") +
                " with " + toolListSupplier.get().size() + " available tools";
    }

    /**
     * List all available tools.
     *
     * @return String representation of available tools
     */
    private String listTools() {
        List<Tool> tools = toolListSupplier.get();

        if (tools.isEmpty()) {
            return "No tools available";
        }

        StringBuilder result = new StringBuilder("Available tools:\n");
        for (Tool tool : tools) {
            result.append("  - ").append(tool.getName())
                    .append(": ").append(tool.getDescription()).append("\n");
        }

        result.append("\nUse '/tool info <tool-name>' for details about a specific tool.");

        // Add integration status if available
        if (integrationStatusSupplier != null) {
            boolean status = integrationStatusSupplier.getAsBoolean();
            result.append("\n\nLLM tool integration is currently ")
                    .append(status ? "enabled" : "disabled")
                    .append(". Use '/tool integration [on|off]' to change.");
        }

        return result.toString();
    }

    /**
     * Get detailed information about a specific tool.
     *
     * @param toolName Name of the tool
     * @return String representation of tool details
     */
    private String getToolInfo(String toolName) {
        List<Tool> tools = toolListSupplier.get();

        for (Tool tool : tools) {
            if (tool.getName().equals(toolName)) {
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
            ToolResult result = toolExecutor.apply(toolName, parameters);

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
}

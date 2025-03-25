package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Tool for executing Docker commands as processes.
 */
public class DockerContainerTool implements Tool {
    
    private static final String NAME = "docker_container";
    private static final String DESCRIPTION = "Executes Docker container operations";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
        new ToolParameter("command", "string", "Docker command to execute", true),
        new ToolParameter("args", "string", "Command arguments", false)
    );
    
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
        try {
            String command = (String) parameters.get("command");
            if (command == null) {
                return ToolResult.failure("Command parameter is required");
            }
            
            // Build command list
            List<String> cmdList = new ArrayList<>();
            cmdList.add("docker");
            cmdList.add(command);
            
            // Add optional arguments
            String args = (String) parameters.get("args");
            if (args != null && !args.trim().isEmpty()) {
                cmdList.addAll(Arrays.asList(args.split("\\s+")));
            }
            
            // Execute command
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            Process process = pb.start();
            
            // Get output and error streams
            String output = new String(process.getInputStream().readAllBytes());
            String error = new String(process.getErrorStream().readAllBytes());
            
            // Wait for completion
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                return ToolResult.failure("Command failed: " + error);
            }
            
            return ToolResult.success(
                Map.of(
                    "output", output.trim(),
                    "command", String.join(" ", cmdList)
                ),
                "Docker command executed successfully"
            );
            
        } catch (Exception e) {
            return ToolResult.failure("Error executing Docker command: " + e.getMessage());
        }
    }
}

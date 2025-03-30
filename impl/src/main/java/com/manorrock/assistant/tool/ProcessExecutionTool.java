package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A tool that executes local system processes.
 */
public class ProcessExecutionTool implements Tool {
    
    private static final String NAME = "process_execution";
    private static final String DESCRIPTION = "Executes a command on the local system and returns the output. "
        + "IMPORTANT: Use this tool ONLY for directly executing specific binaries or commands WITHOUT shell features "
        + "(like pipes, redirections, or shell builtins). "
        + "GOOD examples: Running 'java', 'git', 'python', or other executables directly. "
        + "DO NOT use for: Shell commands with pipes (|), redirections (>, >>), environment variable expansion ($VAR), "
        + "or shell built-ins. For those cases, use shell_execution instead. "
        + "Can be configured with environment variables, working directory (must exist), and timeout.";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
        new ToolParameter("command", "string", "The command to execute", true),
        new ToolParameter("args", "string", "Command arguments", false),
        new ToolParameter("workingDirectory", "string", "The working directory to execute the command in. Must exist. Default is the current directory.", false),
        new ToolParameter("environmentVariables", "object", "Map of environment variables to set for the process.", false),
        new ToolParameter("timeoutSeconds", "number", "Maximum time in seconds to wait for the command to complete. Default is 60 seconds.", false)
    );
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    public ProcessExecutionTool() {
        // No initialization needed
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
        try {
            // Extract parameters
            String command = (String) parameters.get("command");
            String args = (String) parameters.getOrDefault("args", "");
            String workingDirectory = (String) parameters.getOrDefault("workingDirectory", System.getProperty("user.dir"));
            long timeoutSeconds = parameters.containsKey("timeoutSeconds") ? 
                    ((Number) parameters.get("timeoutSeconds")).longValue() : 60L;
            
            // Validate working directory exists
            File workDir = new File(workingDirectory);
            if (!workDir.exists() || !workDir.isDirectory()) {
                return ToolResult.failure("Working directory '" + workingDirectory + "' does not exist or is not a directory");
            }
            
            // Build command list with proper shell handling
            List<String> commandAndArgs = new ArrayList<>();
            commandAndArgs.add(command);
            
            // Handle shell command arguments carefully
            if (!args.isEmpty()) {
                if (command.endsWith("sh") || command.endsWith("bash") || command.endsWith("cmd.exe")) {
                    // For shell commands, keep the arguments as a single string
                    commandAndArgs.add(args);
                } else {
                    // For normal commands, split on spaces
                    commandAndArgs.addAll(Arrays.asList(args.split("\\s+")));
                }
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(commandAndArgs);
            
            // Set working directory
            processBuilder.directory(new File(workingDirectory));
            
            // Inherit environment variables by default
            Map<String, String> environment = processBuilder.environment();
            
            // Add custom environment variables if provided
            if (parameters.containsKey("environmentVariables")) {
                Object envVarsObj = parameters.get("environmentVariables");
                if (envVarsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> envVars = (Map<String, Object>) envVarsObj;
                    for (Map.Entry<String, Object> entry : envVars.entrySet()) {
                        environment.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                } else if (envVarsObj instanceof String && !((String) envVarsObj).trim().isEmpty()) {
                    // Try to parse as JSON if it's a non-empty string
                    try {
                        JsonNode jsonNode = MAPPER.readTree((String) envVarsObj);
                        Iterator<String> fieldNames = jsonNode.fieldNames();
                        while (fieldNames.hasNext()) {
                            String key = fieldNames.next();
                            environment.put(key, jsonNode.get(key).asText());
                        }
                    } catch (Exception e) {
                        return ToolResult.failure("Invalid environment variables format: " + e.getMessage());
                    }
                }
            }
            
            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);
            
            // Start process
            Process process = processBuilder.start();
            
            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Wait for process to complete with timeout
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                return ToolResult.failure("Process execution timed out after " + timeoutSeconds + " seconds");
            }
            
            int exitCode = process.exitValue();
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("exitCode", exitCode);
            resultData.put("output", output.toString());
            
            return exitCode == 0 
                ? ToolResult.success(resultData, "Process executed successfully")
                : ToolResult.failure("Process exited with non-zero code: " + exitCode);

        } catch (Exception e) {
            String command = (String) parameters.get("command");
            if (command != null && command.contains(" ")) {
                return ToolResult.failure("Command contains whitespace, are you sure you should not be using shell_execution instead?");
            } else {
                return ToolResult.failure("Error executing process: " + e.getMessage());
            }
        }
    }
}

package com.manorrock.assistant.shared.tools;

import com.manorrock.assistant.shared.Tool;
import com.manorrock.assistant.shared.ToolParameter;
import com.manorrock.assistant.shared.ToolResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A tool that executes local system processes.
 */
public class ProcessExecutionTool implements Tool {
    
    private static final String NAME = "process_execution";
    private static final String DESCRIPTION = "Executes a command on the local system and returns the output. Can be configured to run with specific environment variables, working directory, and timeout.";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
        new ToolParameter("command", "string", "The command to execute", true),
        new ToolParameter("args", "string", "Command arguments", false),
        new ToolParameter("workingDirectory", "string", "The working directory to execute the command in. Default is the current directory.", false),
        new ToolParameter("environmentVariables", "object", "Map of environment variables to set for the process.", false),
        new ToolParameter("timeoutSeconds", "number", "Maximum time in seconds to wait for the command to complete. Default is 60 seconds.", false)
    );
    
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
                @SuppressWarnings("unchecked")
                Map<String, Object> envVarsObj = (Map<String, Object>) parameters.get("environmentVariables");
                for (Map.Entry<String, Object> entry : envVarsObj.entrySet()) {
                    environment.put(entry.getKey(), String.valueOf(entry.getValue()));
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
            return ToolResult.failure("Error executing process: " + e.getMessage());
        }
    }
}

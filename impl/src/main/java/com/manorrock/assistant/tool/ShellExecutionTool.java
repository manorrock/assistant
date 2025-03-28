package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public class ShellExecutionTool implements Tool {
    
    private static final String NAME = "shell_execution";
    private static final String DESCRIPTION = "Executes a shell command on the local system with shell-specific behavior.";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
        new ToolParameter("command", "string", "The shell command to execute", true),
        new ToolParameter("shell", "string", "The shell to use (e.g., 'bash', 'sh', 'cmd.exe', 'powershell'). If not specified, uses system default.", false),
        new ToolParameter("workingDirectory", "string", "The working directory to execute the command in. Default is the current directory.", false),
        new ToolParameter("environmentVariables", "map", "Map of environment variables to set for the process.", false),
        new ToolParameter("timeoutSeconds", "number", "Maximum time in seconds to wait for the command to complete. Default is 60 seconds.", false)
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
            String workingDirectory = (String) parameters.getOrDefault("workingDirectory", System.getProperty("user.dir"));
            long timeoutSeconds = parameters.containsKey("timeoutSeconds") ? 
                    ((Number) parameters.get("timeoutSeconds")).longValue() : 60L;
            
            // Determine the shell to use
            String shell = determineShell(parameters);
            List<String> shellCommand = buildShellCommand(shell, command);
            
            ProcessBuilder processBuilder = new ProcessBuilder(shellCommand);
            processBuilder.directory(new File(workingDirectory));
            
            // Handle environment variables
            Map<String, String> environment = processBuilder.environment();
            if (parameters.containsKey("environmentVariables")) {
                Object envVars = parameters.get("environmentVariables");
                addEnvironmentVariables(environment, envVars);
            }
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                return ToolResult.failure("Shell command execution timed out after " + timeoutSeconds + " seconds");
            }
            
            int exitCode = process.exitValue();
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("exitCode", exitCode);
            resultData.put("output", output.toString());
            resultData.put("shell", shell);
            
            return exitCode == 0 
                ? ToolResult.success(resultData, "Shell command executed successfully")
                : ToolResult.failure("Shell command exited with non-zero code: " + exitCode);
                
        } catch (Exception e) {
            return ToolResult.failure("Error executing shell command: " + e.getMessage());
        }
    }
    
    private String determineShell(Map<String, Object> parameters) {
        // Use specified shell if provided
        if (parameters.containsKey("shell")) {
            return (String) parameters.get("shell");
        }
        
        // Otherwise determine from system
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "cmd.exe";
        } else if (System.getenv("SHELL") != null) {
            return System.getenv("SHELL");
        } else {
            return "/bin/sh"; // Fallback for Unix-like systems
        }
    }
    
    private List<String> buildShellCommand(String shell, String command) {
        List<String> shellCommand = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            if (shell.toLowerCase().contains("powershell")) {
                shellCommand.addAll(Arrays.asList(shell, "-Command", command));
            } else {
                shellCommand.addAll(Arrays.asList(shell, "/c", command));
            }
        } else {
            shellCommand.addAll(Arrays.asList(shell, "-c", command));
        }
        
        return shellCommand;
    }
    
    private void addEnvironmentVariables(Map<String, String> environment, Object envVars) {
        if (envVars instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> envMap = (Map<String, Object>) envVars;
            for (Map.Entry<String, Object> entry : envMap.entrySet()) {
                environment.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        } else if (envVars instanceof String && !((String) envVars).trim().isEmpty()) {
            try {
                JSONObject jsonObj = new JSONObject((String) envVars);
                for (String key : jsonObj.keySet()) {
                    environment.put(key, String.valueOf(jsonObj.get(key)));
                }
            } catch (Exception e) {
                // Ignore parsing errors and keep existing environment
                System.err.println("Warning: Failed to parse environment variables: " + e.getMessage());
            }
        }
    }
}

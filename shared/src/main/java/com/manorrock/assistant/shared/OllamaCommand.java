package com.manorrock.assistant.shared;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.llm.LlmConfiguration;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command for executing Ollama operations.
 */
public class OllamaCommand implements Command {
    
    private static final String DEFAULT_HOST = "localhost:11434";
    private final java.util.function.Supplier<LlmConfiguration> configSupplier;
    
    public OllamaCommand(java.util.function.Supplier<LlmConfiguration> configSupplier) {
        this.configSupplier = configSupplier;
    }
    
    @Override
    public String executeToString(String args) {
        try {
            // Parse host parameter if present
            String host = DEFAULT_HOST;
            String commandArgs = args;
            
            if (args != null && args.startsWith("--host ")) {
                String[] parts = args.split(" ", 3);
                if (parts.length >= 2) {
                    host = parts[1];
                    commandArgs = parts.length == 3 ? parts[2] : "";
                }
            } else {
                // If no host specified, check if current vendor is OLLAMA
                LlmConfiguration config = configSupplier.get();
                if ("OLLAMA".equalsIgnoreCase(config.vendor())) {
                    String endpoint = config.endpoint();
                    if (endpoint != null && endpoint.contains("/api/chat")) {
                        host = endpoint.substring(0, endpoint.indexOf("/api/chat")).replace("http://", "").replace("https://", "");
                    }
                }
            }
            
            ProcessBuilder pb = new ProcessBuilder("ollama");
            
            // Set environment variables
            Map<String, String> env = new HashMap<>(pb.environment());
            env.put("OLLAMA_HOST", host);
            pb.environment().putAll(env);
            
            // Add command arguments if present
            if (commandArgs != null && !commandArgs.trim().isEmpty()) {
                List<String> command = new ArrayList<>(pb.command());
                command.addAll(List.of(commandArgs.trim().split("\\s+")));
                pb.command(command);
            }
            
            pb.redirectErrorStream(true); // Merge error stream with output stream
            Process process = pb.start();
            
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                return "Error executing Ollama command (exit code " + exitCode + "): " + output;
            }
            
            return output.trim();
            
        } catch (Exception e) {
            return "Failed to execute Ollama command: " + e.getMessage();
        }
    }
    
    @Override
    public InputStream executeToStream(String input) {
        return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    public String getDescription() {
        return """
               Execute Ollama commands directly.
               
               Usage: ollama [--host <host:port>] <command> [arguments]
               
               Examples:
                 ollama list
                 ollama run llama2
                 ollama --host remote:11434 list
               
               The --host parameter is optional and defaults to localhost:11434""";
    }
}

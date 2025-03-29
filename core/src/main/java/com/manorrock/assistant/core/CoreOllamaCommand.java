package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.Llm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Core Ollama command for the Core Assistant.
 * 
 * <p>
 * This command provides specific functionality for Ollama models and server.
 * It allows users to list available models, pull new models, and manage Ollama-specific
 * settings. This command is designed to work with the CoreAssistant.
 * </p>
 */
public class CoreOllamaCommand implements Command {
    
    /**
     * Stores the core assistant.
     */
    private final CoreAssistant assistant;
    
    /**
     * Default Ollama endpoint URL.
     */
    private static final String DEFAULT_ENDPOINT = "http://localhost:11434";

    /**
     * Stores the current Ollama endpoint URL.
     */
    private String endpoint = DEFAULT_ENDPOINT;

    /**
     * Constructor.
     *
     * @param assistant The core assistant instance
     */
    public CoreOllamaCommand(CoreAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public String getDescription() {
        return """
               Manage Ollama models and server settings.
               
               Usage:
                 /ollama exec <command>  - Executes the given Ollama command
                 /ollama endpoint <url>  - Set Ollama server endpoint (default: http://localhost:11434)
               """;
    }

    @Override
    public String getShortDescription() {
        return "Manages Ollama models and settings";
    }

    @Override
    public String execute(String input) {
        return executeToString(input);
    }
    
    @Override
    public String executeToString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Endpoint URL: " + getEndpoint() + "\n" +
                   "Usage: /ollama <subcommand> [args]\n" +
                   "Available subcommands: exec, endpoint";
        }
        
        String[] parts = input.trim().split("\\s+", 2);
        String subCommand = parts[0].toLowerCase();
        
        switch (subCommand) {
            case "exec":
                if (parts.length < 2) {
                    return "Usage: /ollama exec <command>";
                }
                return executeOllamaCommand(parts[1]);

            case "endpoint":
                if (parts.length < 2) {
                    // Return just the URL value without the prefix text
                    return getEndpoint();
                }
                endpoint = parts[1].trim();
                return endpoint;
                
            default:
                return "Unknown subcommand: " + subCommand + "\n" +
                       "Available subcommands: exec, endpoint";
        }
    }
    
    /**
     * Execute an Ollama command using the ollama CLI.
     *
     * @param commandArgs the command arguments to pass to the ollama CLI
     * @return the output of the command
     */
    private String executeOllamaCommand(String commandArgs) {
        try {
            // Parse the host from the current endpoint
            String host = getHost(getEndpoint());
            
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
    
    /**
     * Extract host and port from an endpoint URL.
     * 
     * @param endpointUrl the endpoint URL
     * @return the host:port part of the URL
     */
    private String getHost(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isEmpty()) {
            return "localhost:11434";
        }
        
        // Remove protocol part (http:// or https://)
        String hostPart = endpointUrl
            .replace("http://", "")
            .replace("https://", "");
        
        // Remove path part if any
        int pathIndex = hostPart.indexOf('/');
        if (pathIndex > 0) {
            hostPart = hostPart.substring(0, pathIndex);
        }
        
        return hostPart;
    }

    @Override
    public InputStream executeToStream(String input) {
        return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Get the current endpoint URL.
     *
     * @return The current endpoint URL
     */
    private String getEndpoint() {
        // Check if there's an active LLM first
        String activeLlmName = assistant.getActiveLlm();

        if (activeLlmName != null) {
            // Get the active LLM
            Llm activeLlm = assistant.getLlmManager().getLlm(activeLlmName);
            if (activeLlm != null) {
                // Get the LLM properties
                Properties props = activeLlm.getProperties();
                // Check if it's an Ollama model
                String vendor = props.getProperty("vendor");
                if ("OLLAMA".equalsIgnoreCase(vendor)) {
                    // If it's an Ollama model, get its endpoint
                    String baseUrl = props.getProperty("baseUrl");
                    if (baseUrl != null && !baseUrl.isEmpty()) {
                        return baseUrl;
                    }
                }
            }
        }

        // If no active LLM with Ollama properties, use the explicitly set endpoint.
        if (endpoint != null && !endpoint.isEmpty()) {
            return endpoint;
        }

        // If no active LLM and no endpoint was set, return the default endpoint.
        return DEFAULT_ENDPOINT;
    }
}

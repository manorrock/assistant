package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.llm.LlmConfiguration;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.function.Consumer;

/**
 * Consolidated command to interact with LLM configuration.
 * This command replaces the individual commands:
 * - /llmModel
 * - /llmApiKey
 * - /llmEndpoint
 * - /llmVendor
 * - /llmTemperature
 */
public class LlmCommand implements Command {

    private final Supplier<LlmConfiguration> configSupplier;
    private final Consumer<LlmConfiguration> configUpdater;

    /**
     * Create a new LLM command.
     *
     * @param configSupplier Function to retrieve the current LLM configuration
     * @param configUpdater Function to update the LLM configuration
     */
    public LlmCommand(Supplier<LlmConfiguration> configSupplier, 
                    Consumer<LlmConfiguration> configUpdater) {
        this.configSupplier = configSupplier;
        this.configUpdater = configUpdater;
    }

    @Override
    public String getDescription() {
        return """
               Display and manage LLM configuration.
               
               Usage:
                 /llm                     - Show current configuration
                 /llm vendor <name>       - Set LLM vendor (OPENAI, OLLAMA, AZURE_OPENAI)
                 /llm model <name>        - Set LLM model name
                 /llm endpoint <url>      - Set LLM API endpoint
                 /llm apikey <key>        - Set API key for authentication
                 /llm temperature <value> - Set temperature parameter (0.0-1.0)
               """;
    }

    @Override
    public String execute(String input) {
        return executeToString(input);
    }
    
    @Override
    public String executeToString(String input) {
        if (input == null || input.trim().isEmpty()) {
            // Show current configuration if no arguments provided
            return getCurrentConfiguration();
        }
        
        String[] parts = input.trim().split("\\s+", 2);
        String subCommand = parts[0].toLowerCase();
        
        switch (subCommand) {
            case "status":
            case "info":
            case "show":
                return getCurrentConfiguration();
                
            case "vendor":
                if (parts.length < 2) {
                    return "Current vendor: " + configSupplier.get().vendor();
                }
                return setVendor(parts[1].trim());
                
            case "model":
                if (parts.length < 2) {
                    return "Current model: " + configSupplier.get().model();
                }
                return setModel(parts[1].trim());
                
            case "endpoint":
                if (parts.length < 2) {
                    return "Current endpoint: " + configSupplier.get().endpoint();
                }
                return setEndpoint(parts[1].trim());
                
            case "apikey":
                if (parts.length < 2) {
                    return "Current API key: " + maskApiKey(configSupplier.get().apiKey());
                }
                return setApiKey(parts[1].trim());
                
            case "temperature":
                if (parts.length < 2) {
                    return "Current temperature: " + configSupplier.get().temperature();
                }
                return setTemperature(parts[1].trim());
                
            default:
                return "Unknown subcommand: " + subCommand + "\n" +
                       "Available subcommands: status, vendor, model, endpoint, apikey, temperature";
        }
    }

    @Override
    public InputStream executeToStream(String input) {
        return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Get the current LLM configuration as a formatted string.
     *
     * @return Formatted string with current configuration
     */
    private String getCurrentConfiguration() {
        LlmConfiguration config = configSupplier.get();
        StringBuilder result = new StringBuilder("Current LLM Configuration:\n");
        result.append("  Vendor: ").append(config.vendor()).append("\n");
        result.append("  Model: ").append(config.model()).append("\n");
        result.append("  Endpoint: ").append(config.endpoint()).append("\n");
        result.append("  API Key: ").append(maskApiKey(config.apiKey())).append("\n");
        result.append("  Temperature: ").append(config.temperature()).append("\n");
        
        result.append("\nUse '/llm <setting> <value>' to update a specific setting");
        return result.toString();
    }
    
    /**
     * Set the LLM vendor.
     *
     * @param vendor The new vendor value
     * @return Result message
     */
    private String setVendor(String vendor) {
        try {
            LlmConfiguration oldConfig = configSupplier.get();
            LlmConfiguration newConfig = new LlmConfiguration(
                oldConfig.endpoint(), 
                oldConfig.model(), 
                vendor.toUpperCase(),
                oldConfig.apiKey(), 
                oldConfig.temperature()
            );
            configUpdater.accept(newConfig);
            return "Vendor updated to: " + vendor.toUpperCase();
        } catch (Exception e) {
            return "Error updating vendor: " + e.getMessage();
        }
    }
    
    /**
     * Set the LLM model.
     *
     * @param model The new model value
     * @return Result message
     */
    private String setModel(String model) {
        try {
            LlmConfiguration oldConfig = configSupplier.get();
            LlmConfiguration newConfig = new LlmConfiguration(
                oldConfig.endpoint(), 
                model,
                oldConfig.vendor(), 
                oldConfig.apiKey(), 
                oldConfig.temperature()
            );
            configUpdater.accept(newConfig);
            return "Model updated to: " + model;
        } catch (Exception e) {
            return "Error updating model: " + e.getMessage();
        }
    }
    
    /**
     * Set the LLM endpoint.
     *
     * @param endpoint The new endpoint value
     * @return Result message
     */
    private String setEndpoint(String endpoint) {
        try {
            // Add http:// prefix if missing
            String updatedEndpoint = endpoint;
            if (!updatedEndpoint.startsWith("http://") && !updatedEndpoint.startsWith("https://")) {
                updatedEndpoint = "http://" + updatedEndpoint;
            }
            
            // Add /api/chat suffix if missing
            if (!updatedEndpoint.endsWith("/api/chat")) {
                if (!updatedEndpoint.endsWith("/")) {
                    updatedEndpoint += "/";
                }
                updatedEndpoint += "api/chat";
            }
            
            LlmConfiguration oldConfig = configSupplier.get();
            LlmConfiguration newConfig = new LlmConfiguration(
                updatedEndpoint,
                oldConfig.model(), 
                oldConfig.vendor(), 
                oldConfig.apiKey(), 
                oldConfig.temperature()
            );
            configUpdater.accept(newConfig);
            return "Endpoint updated to: " + updatedEndpoint;
        } catch (Exception e) {
            return "Error updating endpoint: " + e.getMessage();
        }
    }
    
    /**
     * Set the LLM API key.
     *
     * @param apiKey The new API key value
     * @return Result message
     */
    private String setApiKey(String apiKey) {
        try {
            LlmConfiguration oldConfig = configSupplier.get();
            LlmConfiguration newConfig = new LlmConfiguration(
                oldConfig.endpoint(), 
                oldConfig.model(), 
                oldConfig.vendor(), 
                apiKey,
                oldConfig.temperature()
            );
            configUpdater.accept(newConfig);
            return "API key updated";
        } catch (Exception e) {
            return "Error updating API key: " + e.getMessage();
        }
    }
    
    /**
     * Set the LLM temperature.
     *
     * @param temperatureStr The new temperature value as a string
     * @return Result message
     */
    private String setTemperature(String temperatureStr) {
        try {
            double temperature = Double.parseDouble(temperatureStr);
            if (temperature < 0.0 || temperature > 1.0) {
                return "Temperature must be between 0.0 and 1.0";
            }
            
            LlmConfiguration oldConfig = configSupplier.get();
            LlmConfiguration newConfig = new LlmConfiguration(
                oldConfig.endpoint(), 
                oldConfig.model(), 
                oldConfig.vendor(), 
                oldConfig.apiKey(),
                temperature
            );
            configUpdater.accept(newConfig);
            return "Temperature updated to: " + temperature;
        } catch (NumberFormatException e) {
            return "Invalid temperature format. Must be a number between 0.0 and 1.0";
        } catch (Exception e) {
            return "Error updating temperature: " + e.getMessage();
        }
    }
    
    /**
     * Mask the API key for display purposes.
     *
     * @param apiKey The API key to mask
     * @return Masked API key
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "(not set)";
        }
        
        if (apiKey.length() <= 8) {
            return "*".repeat(apiKey.length());
        }
        
        // Show first 4 and last 4 characters, mask the rest
        return apiKey.substring(0, 4) + "*".repeat(apiKey.length() - 8) + apiKey.substring(apiKey.length() - 4);
    }
}

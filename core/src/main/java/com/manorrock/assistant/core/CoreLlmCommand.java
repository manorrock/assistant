package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.Llm;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Core LLM command for the Core Assistant.
 * 
 * <p>
 * This command provides configuration management for the Core LLM implementation.
 * It allows users to view and update settings like model, endpoint, vendor, API key,
 * and temperature. This command is designed to work with the CoreAssistant.
 * </p>
 */
public class CoreLlmCommand implements Command {
    
    /**
     * Stores the core assistant.
     */
    private final CoreAssistant assistant;

    /**
     * Constructor.
     *
     * @param assistant The core assistant instance
     */
    public CoreLlmCommand(CoreAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public String getDescription() {
        return """
               Display and manage LLM configuration.
               
               Usage:
                 /llm                          - Show current configuration
                 /llm apiKey <string>          - Set API key for authentication
                 /llm endpoint <url>           - Set LLM endpoint
                 /llm functionCalling <on|off> - Enable or disable function calling
                 /llm model <string>           - Set LLM model name
                 /llm temperature <number>.    - Set temperature parameter (0.0-1.0)
                 /llm vendor <string>          - Set LLM vendor (OPENAI, OLLAMA, AZURE_OPENAI)
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
                    return "Current vendor: " + getProperty("vendor");
                }
                return setVendor(parts[1].trim());
                
            case "model":
                if (parts.length < 2) {
                    return "Current model: " + getProperty("modelName");
                }
                return setModel(parts[1].trim());
                
            case "endpoint":
                if (parts.length < 2) {
                    return "Current endpoint: " + getProperty("baseUrl");
                }
                return setEndpoint(parts[1].trim());
                
            case "apikey":
                if (parts.length < 2) {
                    return "Current API key: " + maskApiKey(getProperty("apiKey"));
                }
                return setApiKey(parts[1].trim());
                
            case "temperature":
                if (parts.length < 2) {
                    String temp = getProperty("temperature");
                    return "Current temperature: " + (temp != null ? temp : "0.7");
                }
                return setTemperature(parts[1].trim());
                
            case "functioncalling":
                if (parts.length < 2) {
                    return "Function calling is currently: " + (isFunctionCallingEnabled() ? "ON" : "OFF");
                }
                return setFunctionCalling(parts[1].trim());
                
            default:
                return "Unknown subcommand: " + subCommand + "\n" +
                       "Available subcommands: status, vendor, model, endpoint, apiKey, temperature, functionCalling";
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
        StringBuilder result = new StringBuilder("Current LLM Configuration:\n");
        result.append("  API Key: ").append(maskApiKey(getProperty("apiKey", ""))).append("\n");
        result.append("  Endpoint: ").append(getProperty("baseUrl", "http://localhost:11434")).append("\n");
        result.append("  Function Calling: ").append(isFunctionCallingEnabled() ? "ON" : "OFF").append("\n");
        result.append("  Model: ").append(getProperty("modelName", "llama3.2")).append("\n");
        result.append("  Temperature: ").append(getProperty("temperature", "0.7")).append("\n");
        result.append("  Vendor: ").append(getProperty("vendor", "OLLAMA")).append("\n");
        
        result.append("\nUse '/llm <setting> <value>' to update a specific setting");
        return result.toString();
    }
    
    /**
     * Get a property from the current active LLM.
     *
     * @param key The property key to retrieve
     * @return The property value, or null if not found
     */
    private String getProperty(String key) {
        return getProperty(key, null);
    }
    
    /**
     * Get a property from the current active LLM with a default value.
     *
     * @param key The property key to retrieve
     * @param defaultValue The default value if property is not found
     * @return The property value, or defaultValue if not found
     */
    private String getProperty(String key, String defaultValue) {
        String activeLlmName = assistant.getActiveLlm();
        if (activeLlmName == null) {
            return defaultValue;
        }
        
        Llm activeLlm = assistant.getLlmManager().getLlm(activeLlmName);
        if (activeLlm == null) {
            return defaultValue;
        }
        
        Properties props = activeLlm.getProperties();
        String value = props.getProperty(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Set the LLM vendor.
     *
     * @param vendor The new vendor value
     * @return Result message
     */
    private String setVendor(String vendor) {
        String error = updateProperty("vendor", vendor.toUpperCase());
        if (error != null) {
            return error;
        }
        return "Vendor updated to: " + vendor.toUpperCase();
    }
    
    /**
     * Set the LLM model.
     *
     * @param model The new model value
     * @return Result message
     */
    private String setModel(String model) {
        String error = updateProperty("modelName", model);
        if (error != null) {
            return error;
        }
        return "Model updated to: " + model;
    }
    
    /**
     * Set the LLM endpoint.
     *
     * @param endpoint The new endpoint value
     * @return Result message
     */
    private String setEndpoint(String endpoint) {
        // Add http:// prefix if missing
        String updatedEndpoint = endpoint;
        if (!updatedEndpoint.startsWith("http://") && !updatedEndpoint.startsWith("https://")) {
            updatedEndpoint = "http://" + updatedEndpoint;
        }
        
        String error = updateProperty("baseUrl", updatedEndpoint);
        if (error != null) {
            return error;
        }
        return "Endpoint updated to: " + updatedEndpoint;
    }
    
    /**
     * Set the LLM API key.
     *
     * @param apiKey The new API key value
     * @return Result message
     */
    private String setApiKey(String apiKey) {
        String error = updateProperty("apiKey", apiKey);
        if (error != null) {
            return error;
        }
        return "API key updated";
    }
    
    /**
     * Set the LLM temperature.
     *
     * @param temperatureStr The new temperature value as a string
     * @return Result message
     */
    private String setTemperature(String temperatureStr) {
        double temperature;
        
        try {
            temperature = Double.parseDouble(temperatureStr);
        } catch (NumberFormatException e) {
            return "Invalid temperature format. Must be a number between 0.0 and 1.0";
        }
        
        if (temperature < 0.0 || temperature > 1.0) {
            return "Temperature must be between 0.0 and 1.0";
        }
        
        String error = updateProperty("temperature", Double.toString(temperature));
        if (error != null) {
            return error;
        }
        return "Temperature updated to: " + temperature;
    }
    
    /**
     * Update a property in the active LLM and apply the changes.
     *
     * @param key The property key to update
     * @param value The new value for the property
     * @return null if successful, or an error message if failed
     */
    private String updateProperty(String key, String value) {
        String activeLlmName = assistant.getActiveLlm();
        if (activeLlmName == null) {
            return "No active LLM set. Use '/llm set <llm-name>' to set an active LLM first.";
        }
        
        Llm activeLlm = assistant.getLlmManager().getLlm(activeLlmName);
        if (activeLlm == null) {
            return "Active LLM '" + activeLlmName + "' is not registered.";
        }
        
        // Update the property
        Properties props = activeLlm.getProperties();
        props.setProperty(key, value);
        activeLlm.setProperties(props);
        
        // Ensure the changes take effect
        activeLlm.destroy();
        activeLlm.init();
        return null; // Success
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

    @Override
    public String getShortDescription() {
        return "Manages LLms and their configuration";
    }
    
    /**
     * Set function calling on or off.
     *
     * @param setting "on" or "off"
     * @return Result message
     */
    private String setFunctionCalling(String setting) {
        String activeLlmName = assistant.getActiveLlm();
        if (activeLlmName == null) {
            return "No active LLM set. Use '/llm set <llm-name>' to set an active LLM first.";
        }
        
        Llm activeLlm = assistant.getLlmManager().getLlm(activeLlmName);
        if (activeLlm == null) {
            return "Active LLM '" + activeLlmName + "' is not registered.";
        }
        
        if (!(activeLlm instanceof CoreLlm)) {
            return "Function calling is only supported by Core LLM.";
        }
        
        CoreLlm coreLlm = (CoreLlm) activeLlm;
        
        setting = setting.toLowerCase();
        if ("on".equals(setting)) {
            coreLlm.setFunctionCallingEnabled(true);
            return "Function calling is now ON";
        } else if ("off".equals(setting)) {
            coreLlm.setFunctionCallingEnabled(false);
            return "Function calling is now OFF";
        } else {
            return "Invalid value. Use 'on' or 'off'";
        }
    }
    
    /**
     * Check if function calling is enabled for the current active LLM.
     *
     * @return true if function calling is enabled, false otherwise
     */
    private boolean isFunctionCallingEnabled() {
        String activeLlmName = assistant.getActiveLlm();
        if (activeLlmName == null) {
            return false;
        }
        
        Llm activeLlm = assistant.getLlmManager().getLlm(activeLlmName);
        if (activeLlm == null || !(activeLlm instanceof CoreLlm)) {
            return false;
        }
        
        return ((CoreLlm) activeLlm).isFunctionCallingEnabled();
    }
}

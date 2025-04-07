package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.Llm;
import java.util.Map;
import java.util.Properties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
     * Stores the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CoreLlmCommand.class.getName());
    
    /**
     * Stores the core assistant.
     */
    private final CoreAssistant assistant;
    
    /**
     * Stores the templates for different coding tasks.
     */
    private final Map<String, String> systemMessageTemplates = new HashMap<>();
    
    /**
     * The path to the templates directory.
     */
    private final Path templatesDir;
    
    /**
     * Object mapper for JSON parsing.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor.
     *
     * @param assistant The core assistant instance
     */
    public CoreLlmCommand(CoreAssistant assistant) {
        this.assistant = assistant;
        this.templatesDir = Paths.get(System.getProperty("user.home"), ".manorrock", "assistant", "templates");
        
        // Initialize with default templates
        systemMessageTemplates.put("code-review", "You are a senior developer conducting a code review. Focus on identifying bugs, security issues, performance problems, and adherence to best practices. Be thorough but constructive in your feedback.");
        systemMessageTemplates.put("debug", "You are a debugging specialist. Analyze the code or error messages provided and help identify the root cause of issues. Suggest specific fixes and explain your reasoning step by step.");
        systemMessageTemplates.put("refactor", "You are a refactoring expert. Analyze the code provided and suggest improvements to make it more maintainable, efficient, and aligned with best practices. Preserve functionality while enhancing code quality.");
        systemMessageTemplates.put("document", "You are a technical documentation specialist. Create clear, concise documentation for the code provided. Focus on explaining purpose, functionality, parameters, return values, and usage examples.");
        systemMessageTemplates.put("test", "You are a test-driven development expert. Create comprehensive test cases for the provided code, focusing on edge cases, error conditions, and complete coverage. Suggest appropriate testing frameworks and methodologies.");
        systemMessageTemplates.put("optimize", "You are a performance optimization specialist. Analyze the code for performance bottlenecks and suggest specific improvements to enhance efficiency, reduce resource consumption, and improve scalability.");
        systemMessageTemplates.put("security", "You are a security expert. Analyze the code for potential security vulnerabilities including injection attacks, authentication issues, access control problems, and data validation concerns. Suggest specific security improvements.");
        systemMessageTemplates.put("design-patterns", "You are a design pattern specialist. Analyze the code and suggest appropriate design patterns that could improve its structure, scalability, and maintainability. Explain the benefits of your suggestions.");
        
        // Load additional templates from config
        loadTemplatesFromConfig();
    }

    @Override
    public String getDescription() {
        return """
               Display and manage LLM configuration.
               
               Usage:
                 /llm                          - Show the help message
                 /llm add <string>             - Add a new unconfigured LLM with the given name
                 /llm apiKey <string>          - Set API key for authentication
                 /llm endpoint <url>           - Set LLM endpoint
                 /llm functionCalling <on|off> - Enable or disable function calling
                 /llm info                     - Show current configuration
                 /llm list                     - List all available LLMs
                 /llm model <string>           - Set LLM model name
                 /llm remove <string>          - Remove an LLM with the specified name
                 /llm reset                    - Reset the LLM and clear memory
                 /llm temperature <number>     - Set temperature parameter (0.0-1.0)
                 /llm use <string>             - Set the active LLM by name
                 /llm vendor <string>          - Set LLM vendor (OPENAI, OLLAMA, AZURE_OPENAI)
                 /llm systemMessage <string>   - Set system message template
               """;
    }

    @Override
    public String execute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return getDescription();
        }
        
        String[] parts = input.trim().split("\\s+", 2);
        String subCommand = parts[0].toLowerCase();
        
        switch (subCommand) {
            case "info":
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
                
            case "add":
                if (parts.length < 2) {
                    return "Please provide a name for the new LLM. Usage: /llm add <name>";
                }
                return addLlm(parts[1].trim());
            
            case "list":
                return listLlms();
                
            case "reset":
                return resetLlm();
                
            case "remove":
                if (parts.length < 2) {
                    return "Please provide the name of the LLM to remove. Usage: /llm remove <llm-name>";
                }
                return removeLlm(parts[1].trim());

            case "use":
                if (parts.length < 2) {
                    return "Current active LLM: " + (assistant.getActiveLlm() != null ? assistant.getActiveLlm() : "(none)");
                }
                return setActiveLlm(parts[1].trim());
                
            case "systemmessage":
                if (parts.length < 2) {
                    return "Available system message templates: " + String.join(", ", systemMessageTemplates.keySet());
                }
                return setSystemMessage(parts[1].trim());
                
            default:
                return "Unknown subcommand: " + subCommand + "\n" +
                       "Available subcommands: status, vendor, model, endpoint, apiKey, temperature, functionCalling, add, list, reset, remove, use, systemMessage";
        }
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
    
    /**
     * Reset the LLM and clear its memory.
     *
     * @return Result message
     */
    private String resetLlm() {
        String activeLlmName = assistant.getActiveLlm();
        if (activeLlmName == null) {
            return "No active LLM set. Use '/llm set <llm-name>' to set an active LLM first.";
        }
        
        Llm activeLlm = assistant.getLlmManager().getLlm(activeLlmName);
        if (activeLlm == null) {
            return "Active LLM '" + activeLlmName + "' is not registered.";
        }
        
        activeLlm.destroy();
        activeLlm.init();
        return "LLM has been reset and memory cleared.";
    }
    
    /**
     * Set the active LLM by name.
     *
     * @param llmName The name of the LLM to set as active
     * @return Result message
     */
    private String setActiveLlm(String llmName) {
        if (assistant.getLlmManager().getLlm(llmName) != null) {
            assistant.setActiveLlm(llmName);
            return "Active LLM set to: " + llmName;
        } else {
            return "LLM '" + llmName + "' is not available.";
        }
    }
    
    /**
     * List all available LLMs.
     *
     * @return Formatted string with all LLMs
     */
    private String listLlms() {
        Map<String, Llm> llms = assistant.getLlmManager().getLlms();
        
        if (llms.isEmpty()) {
            return "No LLMs are currently registered.";
        }
        
        StringBuilder result = new StringBuilder("Available LLMs:\n");
        String activeLlm = assistant.getActiveLlm();
        
        for (Map.Entry<String, Llm> entry : llms.entrySet()) {
            String llmName = entry.getKey();
            Llm llm = entry.getValue();
            
            result.append("  ");
            
            // Mark the active LLM with an asterisk
            if (llmName.equals(activeLlm)) {
                result.append("* ");
            } else {
                result.append("  ");
            }
            
            result.append(llmName);
            
            // Add basic information if it's a CoreLlm
            if (llm instanceof CoreLlm) {
                Properties props = llm.getProperties();
                String vendor = props.getProperty("vendor", "unknown");
                String model = props.getProperty("modelName", "unknown");
                
                result.append(" (").append(vendor).append(", ").append(model).append(")");
            }
            
            result.append("\n");
        }
        
        if (activeLlm != null) {
            result.append("\n* = active LLM\n");
        }
        
        result.append("\nUse '/llm set <name>' to set the active LLM");
        
        return result.toString();
    }
    
    /**
     * Add a new LLM with the given name.
     *
     * @param name The name of the new LLM
     * @return Result message
     */
    private String addLlm(String name) {
        if (assistant.getLlmManager().getLlm(name) != null) {
            return "LLM with name '" + name + "' already exists.";
        }
        
        Llm newLlm = new CoreLlm(assistant.getLlmManager());
        newLlm.init();
        assistant.getLlmManager().registerLlm(name, newLlm);
        return "New LLM '" + name + "' has been added. Use '/llm set " + name + "' to activate it.";
    }
    
    /**
     * Remove an LLM with the given name.
     *
     * @param name The name of the LLM to remove
     * @return Result message
     */
    private String removeLlm(String name) {
        if (assistant.getLlmManager().getLlm(name) == null) {
            return "LLM with name '" + name + "' does not exist.";
        }
        
        // If the LLM to be removed is the active one, unset it first
        if (name.equals(assistant.getActiveLlm())) {
            assistant.setActiveLlm(null);
        }
        
        // Get the LLM and destroy it before removing
        Llm llmToRemove = assistant.getLlmManager().getLlm(name);
        llmToRemove.destroy();
        
        // Unregister the LLM using the interface method
        assistant.getLlmManager().unregisterLlm(name);
        
        return "LLM '" + name + "' has been removed.";
    }
    
    /**
     * Set the system message template.
     *
     * @param input The user input (template name or subcommand)
     * @return Result message
     */
    private String setSystemMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "Available templates: " + String.join(", ", systemMessageTemplates.keySet());
        }

        String[] parts = input.trim().split("\\s+", 2);
        String subCommand = parts[0];
        
        // Handle special cases
        if (subCommand.equals("list")) {
            return listTemplates();
        } else if (subCommand.equals("create") && parts.length > 1) {
            return createTemplate(parts[1]);
        } else if (subCommand.equals("view") && parts.length > 1) {
            return viewTemplate(parts[1]);
        } else if (subCommand.equals("help")) {
            return getTemplateHelpText();
        }
        
        // Otherwise, treat it as a template name
        String template = systemMessageTemplates.get(subCommand);
        if (template == null) {
            return "Template '" + subCommand + "' does not exist. Available templates: " + String.join(", ", systemMessageTemplates.keySet());
        }
        
        String error = updateProperty("systemMessage", template);
        if (error != null) {
            return error;
        }
        return "System message template set to: " + subCommand;
    }
    
    /**
     * List all available templates with descriptions.
     * 
     * @return A formatted string listing all templates.
     */
    private String listTemplates() {
        StringBuilder sb = new StringBuilder("Available templates:\n\n");
        
        systemMessageTemplates.forEach((name, template) -> {
            sb.append("- ").append(name).append(": ");
            // Extract first sentence for description
            String firstSentence = template.split("\\. ")[0];
            if (firstSentence.length() > 100) {
                firstSentence = firstSentence.substring(0, 97) + "...";
            }
            sb.append(firstSentence).append("\n");
        });
        
        sb.append("\nUse '/llm systemMessage view <name>' to see the full template.");
        return sb.toString();
    }
    
    /**
     * View the full content of a template.
     * 
     * @param name The name of the template to view.
     * @return The template content or an error message.
     */
    private String viewTemplate(String name) {
        if (systemMessageTemplates.containsKey(name)) {
            return "Template '" + name + "':\n\n" + systemMessageTemplates.get(name);
        }
        return "Template not found: " + name;
    }
    
    /**
     * Create a new template from a JSON file.
     * 
     * @param args Format: <name> <path_to_json>
     * @return Success or error message.
     */
    private String createTemplate(String args) {
        String[] parts = args.trim().split("\\s+", 2);
        if (parts.length < 2) {
            return "Usage: /llm systemMessage create <name> <path_to_json_file>";
        }
        
        String name = parts[0];
        String filePath = parts[1];
        
        try {
            // Read and validate the JSON file
            JsonNode root = objectMapper.readTree(new File(filePath));
            if (!root.has("template")) {
                return "Error: JSON file must contain a 'template' field";
            }
            
            String template = root.get("template").asText();
            if (template == null || template.isEmpty()) {
                return "Error: Template content cannot be empty";
            }
            
            // Save the template
            systemMessageTemplates.put(name, template);
            
            // Write to disk
            try {
                if (!Files.exists(templatesDir)) {
                    Files.createDirectories(templatesDir);
                }
                
                Map<String, String> templateMap = new HashMap<>();
                templateMap.put("name", name);
                templateMap.put("template", template);
                
                objectMapper.writeValue(templatesDir.resolve(name + ".json").toFile(), templateMap);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error saving template to disk: " + e.getMessage(), e);
                // Continue anyway since we've already updated the in-memory template
            }
            
            return "Template '" + name + "' created successfully";
        } catch (IOException e) {
            return "Error creating template: " + e.getMessage();
        }
    }
    
    /**
     * Get help text for the template command.
     * 
     * @return The help text.
     */
    private String getTemplateHelpText() {
        return """
               System Message Template Help:
               
               /llm systemMessage                     - List available templates
               /llm systemMessage <name>              - Use the specified template
               /llm systemMessage list                - List all templates with descriptions
               /llm systemMessage view <name>         - View the full content of a template
               /llm systemMessage create <name> <file>- Create a new template from a JSON file
               /llm systemMessage help                - Show this help text
               
               Template JSON format:
               {
                   "template": "You are a [role]. Focus on [specific instructions]..."
               }
               
               Templates are stored in: """ + templatesDir;
    }
    
    /**
     * Load templates from the configuration directory.
     */
    private void loadTemplatesFromConfig() {
        try {
            // Create templates directory if it doesn't exist
            if (!Files.exists(templatesDir)) {
                Files.createDirectories(templatesDir);
                LOGGER.info("Created templates directory: " + templatesDir);
                
                // Save default templates to files for reference
                saveDefaultTemplatesToDisk();
            }
            
            // Load all .json files from the templates directory
            Files.list(templatesDir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(this::loadTemplateFile);
                
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading templates from config: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load a single template file.
     * 
     * @param path the path to the template file.
     */
    private void loadTemplateFile(Path path) {
        try {
            JsonNode root = objectMapper.readTree(path.toFile());
            String name = root.path("name").asText();
            String template = root.path("template").asText();
            
            if (name != null && !name.isEmpty() && template != null && !template.isEmpty()) {
                systemMessageTemplates.put(name, template);
                LOGGER.fine("Loaded template: " + name + " from " + path);
            } else {
                LOGGER.warning("Invalid template file: " + path + " - missing name or template content");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error parsing template file: " + path, e);
        }
    }
    
    /**
     * Save default templates to disk for reference.
     */
    private void saveDefaultTemplatesToDisk() {
        systemMessageTemplates.forEach((name, template) -> {
            try {
                Map<String, String> templateMap = new HashMap<>();
                templateMap.put("name", name);
                templateMap.put("template", template);
                
                objectMapper.writeValue(templatesDir.resolve(name + ".json").toFile(), templateMap);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error saving default template: " + name, e);
            }
        });
    }
}

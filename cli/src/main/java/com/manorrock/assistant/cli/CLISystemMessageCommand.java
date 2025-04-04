package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.Llm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Command implementation that manages system message templates for different coding tasks.
 */
public class CLISystemMessageCommand implements Command {
    
    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CLISystemMessageCommand.class.getName());
    
    /**
     * Stores the assistant instance.
     */
    private final Assistant assistant;
    
    /**
     * Stores the templates for different coding tasks.
     */
    private final Map<String, String> templates = new HashMap<>();
    
    /**
     * The path to the templates directory.
     */
    private final Path templatesDir;
    
    /**
     * Object mapper for JSON parsing.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Constructor to initialize the command with the assistant.
     * 
     * @param assistant the assistant.
     */
    public CLISystemMessageCommand(Assistant assistant) {
        this.assistant = assistant;
        this.templatesDir = Paths.get(System.getProperty("user.home"), ".manorrock", "assistant", "templates");
        
        // Initialize with default templates
        templates.put("code-review", "You are a senior developer conducting a code review. Focus on identifying bugs, security issues, performance problems, and adherence to best practices. Be thorough but constructive in your feedback.");
        templates.put("debug", "You are a debugging specialist. Analyze the code or error messages provided and help identify the root cause of issues. Suggest specific fixes and explain your reasoning step by step.");
        templates.put("refactor", "You are a refactoring expert. Analyze the code provided and suggest improvements to make it more maintainable, efficient, and aligned with best practices. Preserve functionality while enhancing code quality.");
        templates.put("document", "You are a technical documentation specialist. Create clear, concise documentation for the code provided. Focus on explaining purpose, functionality, parameters, return values, and usage examples.");
        templates.put("test", "You are a test-driven development expert. Create comprehensive test cases for the provided code, focusing on edge cases, error conditions, and complete coverage. Suggest appropriate testing frameworks and methodologies.");
        templates.put("optimize", "You are a performance optimization specialist. Analyze the code for performance bottlenecks and suggest specific improvements to enhance efficiency, reduce resource consumption, and improve scalability.");
        templates.put("security", "You are a security expert. Analyze the code for potential security vulnerabilities including injection attacks, authentication issues, access control problems, and data validation concerns. Suggest specific security improvements.");
        templates.put("design-patterns", "You are a design pattern specialist. Analyze the code and suggest appropriate design patterns that could improve its structure, scalability, and maintainability. Explain the benefits of your suggestions.");
        
        // Load additional templates from config
        loadTemplatesFromConfig();
    }
    
    @Override
    public String execute(String input) {
        // If no input provided, list available templates
        if (input == null || input.isEmpty()) {
            return "Available templates: " + String.join(", ", templates.keySet());
        }
        
        // Split input into template name and potential arguments
        String[] parts = input.trim().split("\\s+", 2);
        String templateName = parts[0];
        
        // Handle special cases
        if (templateName.equals("list")) {
            return listTemplates();
        } else if (templateName.equals("create") && parts.length > 1) {
            return createTemplate(parts[1]);
        } else if (templateName.equals("view") && parts.length > 1) {
            return viewTemplate(parts[1]);
        } else if (templateName.equals("help")) {
            return getHelpText();
        }
        
        // Check if template exists and set it as system message
        if (templates.containsKey(templateName)) {
            try {
                Llm activeLlm = assistant.getLlmManager().getLlm(assistant.getActiveLlm());
                activeLlm.getProperties().setProperty("systemMessage", templates.get(templateName));
                return "System message set to template: " + templateName;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error setting system message: " + e.getMessage(), e);
                return "Error setting system message: " + e.getMessage();
            }
        }
        
        return "Unknown template: " + templateName + ". Type '/template' to see available templates or '/template help' for more options.";
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
                templates.put(name, template);
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
        templates.forEach((name, template) -> {
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
    
    /**
     * List all available templates with descriptions.
     * 
     * @return A formatted string listing all templates.
     */
    private String listTemplates() {
        StringBuilder sb = new StringBuilder("Available templates:\n\n");
        
        templates.forEach((name, template) -> {
            sb.append("- ").append(name).append(": ");
            // Extract first sentence for description
            String firstSentence = template.split("\\. ")[0];
            if (firstSentence.length() > 100) {
                firstSentence = firstSentence.substring(0, 97) + "...";
            }
            sb.append(firstSentence).append("\n");
        });
        
        sb.append("\nUse '/template view <name>' to see the full template.");
        return sb.toString();
    }
    
    /**
     * View the full content of a template.
     * 
     * @param name The name of the template to view.
     * @return The template content or an error message.
     */
    private String viewTemplate(String name) {
        if (templates.containsKey(name)) {
            return "Template '" + name + "':\n\n" + templates.get(name);
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
            return "Usage: /template create <name> <path_to_json_file>";
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
            templates.put(name, template);
            
            // Write to disk
            Map<String, String> templateMap = new HashMap<>();
            templateMap.put("name", name);
            templateMap.put("template", template);
            objectMapper.writeValue(templatesDir.resolve(name + ".json").toFile(), templateMap);
            
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
    private String getHelpText() {
        return """
               Template Command Help:
               
               /template                     - List available templates
               /template <name>              - Use the specified template
               /template list                - List all templates with descriptions
               /template view <name>         - View the full content of a template
               /template create <name> <file>- Create a new template from a JSON file
               /template help                - Show this help text
               
               Template JSON format:
               {
                   "template": "You are a [role]. Focus on [specific instructions]..."
               }
               
               Templates are stored in: """ + templatesDir;
    }
    
    @Override
    public String getDescription() {
        return "Manage and apply specialized system message templates for different coding tasks";
    }
    
    @Override
    public String getShortDescription() {
        return "Manage system message templates";
    }
}
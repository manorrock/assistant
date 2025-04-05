package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.Llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;

/**
 * Command implementation that loads and applies the issue implementation template.
 * This command enables the CLI to effectively implement features from issue descriptions.
 */
public class CLIIssueTemplateCommand implements Command {
    
    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CLIIssueTemplateCommand.class.getName());
    
    /**
     * Stores the assistant instance.
     */
    private final Assistant assistant;
    
    /**
     * Stores the issue implementation template.
     */
    private String issueTemplate;
    
    /**
     * Protected setter for the issue template, used in tests.
     * 
     * @param template The template text.
     */
    protected void setIssueTemplate(String template) {
        this.issueTemplate = template;
    }
    
    /**
     * The path to the resources templates directory.
     */
    private final Path resourceTemplatesDir;
    
    /**
     * The path to the template file.
     */
    private final Path templateFilePath;
    
    /**
     * Project context information to fill in template placeholders.
     */
    private final Map<String, String> projectContext = new HashMap<>();
    
    /**
     * Constructor to initialize the command with the assistant.
     * 
     * @param assistant the assistant.
     */
    public CLIIssueTemplateCommand(Assistant assistant) {
        this.assistant = assistant;
        this.resourceTemplatesDir = Paths.get(getClass().getClassLoader().getResource("templates").getPath());
        this.templateFilePath = resourceTemplatesDir.resolve("issue_implementation.txt");
        
        // Initialize with default project context values
        projectContext.put("PROJECT_NAME", "Unknown Project");
        projectContext.put("LANGUAGE", "Java");
        projectContext.put("FRAMEWORKS", "None");
        projectContext.put("ARCHITECTURE_PATTERN", "Unknown");
        projectContext.put("CURRENT_DIRECTORY", System.getProperty("user.dir"));
        projectContext.put("ADDITIONAL_CONTEXT", "");
        
        // Load the issue implementation template
        loadIssueTemplate();
    }
    
    /**
     * Load the issue implementation template from resources.
     */
    protected void loadIssueTemplate() {
        try {
            if (Files.exists(templateFilePath)) {
                issueTemplate = Files.readString(templateFilePath);
                LOGGER.info("Loaded issue implementation template from: " + templateFilePath);
            } else {
                // Fall back to reading from classpath if file doesn't exist
                try (InputStream is = getClass().getClassLoader().getResourceAsStream("templates/issue_implementation.txt")) {
                    if (is != null) {
                        issueTemplate = new String(is.readAllBytes());
                        LOGGER.info("Loaded issue implementation template from classpath");
                    } else {
                        LOGGER.warning("Issue implementation template not found in classpath");
                        issueTemplate = "You are an expert developer tasked with implementing features from issue descriptions.";
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading issue implementation template: " + e.getMessage(), e);
            // Set a fallback template if loading fails
            issueTemplate = "You are an expert developer tasked with implementing features from issue descriptions.";
        }
    }
    
    @Override
    public String execute(String input) {
        if (input == null || input.isEmpty() || input.equals("help")) {
            return getHelpText();
        }
        
        // Parse command arguments
        String[] parts = input.trim().split("\\s+", 2);
        String command = parts[0];
        
        switch (command) {
            case "apply":
                return applyTemplate();
            case "view":
                return viewTemplate();
            case "set-context":
                if (parts.length > 1) {
                    return setProjectContext(parts[1]);
                } else {
                    return "Usage: /issue-template set-context <key>=<value>";
                }
            case "show-context":
                return showProjectContext();
            case "reload":
                loadIssueTemplate();
                return "Issue implementation template reloaded";
            default:
                return "Unknown command: " + command + ". Type '/issue-template help' for usage information.";
        }
    }
    
    /**
     * Apply the issue implementation template as the system message.
     * 
     * @return Success or error message.
     */
    private String applyTemplate() {
        try {
            // Fill in template placeholders with project context
            String filledTemplate = fillTemplateWithContext();
            
            // Set the template as the system message for the active LLM
            Llm activeLlm = assistant.getLlmManager().getLlm(assistant.getActiveLlm());
            activeLlm.getProperties().setProperty("systemMessage", filledTemplate);
            
            return "Issue implementation template applied as system message";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error applying issue implementation template: " + e.getMessage(), e);
            return "Error applying issue implementation template: " + e.getMessage();
        }
    }
    
    /**
     * View the issue implementation template with placeholders filled in.
     * 
     * @return The template content.
     */
    private String viewTemplate() {
        return "Issue Implementation Template:\n\n" + fillTemplateWithContext();
    }
    
    /**
     * Set a project context value to be used in template placeholders.
     * 
     * @param contextSetting Format: key=value
     * @return Success or error message.
     */
    private String setProjectContext(String contextSetting) {
        try {
            String[] setting = contextSetting.split("=", 2);
            if (setting.length < 2) {
                return "Invalid format. Use: key=value";
            }
            
            String key = setting[0].trim().toUpperCase();
            String value = setting[1].trim();
            
            // Validate key is a supported placeholder
            if (!isValidContextKey(key)) {
                return "Invalid context key: " + key + ". Supported keys: " + 
                       String.join(", ", projectContext.keySet());
            }
            
            // Update the project context
            projectContext.put(key, value);
            
            return "Project context updated: " + key + " = " + value;
        } catch (Exception e) {
            return "Error setting project context: " + e.getMessage();
        }
    }
    
    /**
     * Show the current project context values.
     * 
     * @return Formatted project context.
     */
    private String showProjectContext() {
        StringBuilder sb = new StringBuilder("Current Project Context:\n\n");
        
        projectContext.forEach((key, value) -> {
            sb.append(key).append(": ").append(value).append("\n");
        });
        
        return sb.toString();
    }
    
    /**
     * Fill in the template placeholders with project context values.
     * 
     * @return The filled template.
     */
    private String fillTemplateWithContext() {
        String filledTemplate = issueTemplate;
        
        // Replace each placeholder with its corresponding value
        for (Map.Entry<String, String> entry : projectContext.entrySet()) {
            filledTemplate = filledTemplate.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        
        return filledTemplate;
    }
    
    /**
     * Check if a context key is valid.
     * 
     * @param key The key to validate.
     * @return true if the key is valid, false otherwise.
     */
    private boolean isValidContextKey(String key) {
        return projectContext.containsKey(key);
    }
    
    /**
     * Get help text for the issue-template command.
     * 
     * @return The help text.
     */
    private String getHelpText() {
        return """
               Issue Template Command Help:
               
               /issue-template                   - Show this help text
               /issue-template apply             - Apply the issue implementation template as system message
               /issue-template view              - View the current issue implementation template
               /issue-template set-context k=v   - Set project context value for template placeholders
               /issue-template show-context      - Show current project context values
               /issue-template reload            - Reload the template from file
               /issue-template help              - Show this help text
               
               Available context placeholders:
               - PROJECT_NAME: The name of the project
               - LANGUAGE: The primary programming language
               - FRAMEWORKS: Frameworks used in the project
               - ARCHITECTURE_PATTERN: The architectural pattern
               - CURRENT_DIRECTORY: The current working directory
               - ADDITIONAL_CONTEXT: Any additional project context
               
               Example:
               /issue-template set-context LANGUAGE=TypeScript
               /issue-template set-context FRAMEWORKS=React,Next.js
               /issue-template apply
               """;
    }
    
    @Override
    public String getDescription() {
        return "Loads and applies the issue implementation template for implementing features from issue descriptions";
    }
    
    @Override
    public String getShortDescription() {
        return "Manage issue implementation template";
    }
}
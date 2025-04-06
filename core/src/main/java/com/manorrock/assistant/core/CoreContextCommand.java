package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Core Context Manager command for the Core Assistant.
 * 
 * <p>
 * This command provides functionality for managing context files that can be
 * used when generating prompts. It allows users to add files to context,
 * list the current context, clear the context, and use context in prompts.
 * Context state is persisted between sessions.
 * </p>
 */
public class CoreContextCommand implements Command {
    
    private static final Logger LOGGER = Logger.getLogger(CoreContextCommand.class.getName());
    private final CoreAssistant assistant;
    private final List<String> contextFiles = new ArrayList<>();
    private final Path stateDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CoreContextCommand(CoreAssistant assistant) {
        this.assistant = assistant;
        this.stateDir = Paths.get(System.getProperty("user.home"), ".manorrock", "assistant");
        loadContext();
        applyContext(); // Apply context on initialization
    }

    @Override
    public String execute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return getHelp();
        }
        
        String[] parts = input.trim().split("\\s+", 2);
        String subCommand = parts[0].toLowerCase();
        String arguments = parts.length > 1 ? parts[1].trim() : "";
        
        String result;
        switch (subCommand) {
            case "add":
                result = addToContext(arguments);
                if (!result.startsWith("Error")) {
                    applyContext();
                }
                return result;
            case "list":
                return listContext();
            case "clear":
                result = clearContext();
                applyContext();
                return result;
            case "remove":
                result = removeFromContext(arguments);
                if (!result.startsWith("Error")) {
                    applyContext();
                }
                return result;
            case "help":
            default:
                return getHelp();
        }
    }

    private void applyContext() {
        // Build context content from files
        StringBuilder contextContent = new StringBuilder();
        for (String filePath : contextFiles) {
            try {
                File file = new File(filePath);
                if (file.exists() && file.isFile()) {
                    String content = Files.readString(file.toPath());
                    contextContent.append("===== File: ").append(filePath).append(" =====\n");
                    contextContent.append(content).append("\n\n");
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error reading context file: " + filePath, e);
            }
        }

        // If we have context, set it in the assistant
        if (contextContent.length() > 0) {
            assistant.setContext(contextContent.toString());
        } else {
            assistant.clearContext();
        }
    }

    /**
     * Add a file to the context.
     * 
     * @param filePath File path to add
     * @return Result message
     */
    private String addToContext(String filePath) {
        if (filePath.isEmpty()) {
            return "Error: No file path provided";
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            return "Error: File does not exist: " + filePath;
        }
        
        if (!file.isFile()) {
            return "Error: Not a file: " + filePath;
        }
        
        if (!contextFiles.contains(filePath)) {
            contextFiles.add(filePath);
            saveContext();
            return "Added to context: " + filePath;
        } else {
            return "File already in context: " + filePath;
        }
    }
    
    /**
     * List the current context files.
     * 
     * @return List of context files
     */
    private String listContext() {
        if (contextFiles.isEmpty()) {
            return "Context is empty. Use '/context add <file>' to add files.";
        }
        
        StringBuilder result = new StringBuilder("Current context files:\n");
        for (int i = 0; i < contextFiles.size(); i++) {
            result.append(i + 1).append(". ").append(contextFiles.get(i)).append("\n");
        }
        return result.toString();
    }
    
    /**
     * Clear the context (remove all files).
     * 
     * @return Result message
     */
    private String clearContext() {
        int count = contextFiles.size();
        contextFiles.clear();
        saveContext();
        return "Cleared context (" + count + " files removed)";
    }
    
    /**
     * Remove a file from the context.
     * 
     * @param argument Index or file path to remove
     * @return Result message
     */
    private String removeFromContext(String argument) {
        if (argument.isEmpty()) {
            return "Error: Please specify a file number or path to remove";
        }
        
        if (contextFiles.isEmpty()) {
            return "Context is already empty";
        }
        
        // Try to parse as index first
        try {
            int index = Integer.parseInt(argument) - 1;
            if (index >= 0 && index < contextFiles.size()) {
                String removed = contextFiles.remove(index);
                saveContext();
                return "Removed from context: " + removed;
            } else {
                return "Error: Invalid file number. Use '/context list' to see valid numbers.";
            }
        } catch (NumberFormatException e) {
            // Not a number, try as a file path
            if (contextFiles.remove(argument)) {
                saveContext();
                return "Removed from context: " + argument;
            } else {
                return "Error: File not found in context: " + argument;
            }
        }
    }
    
    /**
     * Load context from persistent storage.
     */
    private void loadContext() {
        try {
            Path contextFile = stateDir.resolve("context.json");
            if (Files.exists(contextFile)) {
                String content = Files.readString(contextFile);
                ArrayNode contextArray = (ArrayNode) objectMapper.readTree(content);
                contextFiles.clear();
                contextArray.forEach(node -> contextFiles.add(node.asText()));
                LOGGER.info("Loaded " + contextFiles.size() + " context files from " + contextFile);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading context, starting with empty context: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save context to persistent storage.
     */
    private void saveContext() {
        try {
            Files.createDirectories(stateDir);
            Path contextFile = stateDir.resolve("context.json");
            
            ArrayNode contextArray = objectMapper.createArrayNode();
            for (String file : contextFiles) {
                contextArray.add(file);
            }
            
            Files.writeString(contextFile, objectMapper.writeValueAsString(contextArray));
            LOGGER.info("Saved " + contextFiles.size() + " context files to " + contextFile);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error saving context: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get help information for the context command.
     * 
     * @return Help text
     */
    private String getHelp() {
        return """
               Manage context files for prompts.
               
               Usage:
                 /context add <file>     - Add a file to the context
                 /context list           - List current context files
                 /context remove <n|path> - Remove a file by number or path
                 /context clear          - Clear all context files
               """;
    }

    @Override
    public String getDescription() {
        return """
               Manage context files for prompts.
               
               This command allows you to maintain a list of files that provide context
               for your prompts. You can add files to the context, list the current context,
               remove files, and clear the context. Context files are automatically included
               in all conversations with the assistant.
               
               The context is persisted between sessions, so you can close and reopen
               the assistant without losing your context.
               
               Usage:
                 /context add <file>     - Add a file to the context
                 /context list           - List current context files
                 /context remove <n|path> - Remove a file by number or path
                 /context clear          - Clear all context files
               """;
    }

    @Override
    public String getShortDescription() {
        return "Manages context files for prompts";
    }
}
package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;

/**
 * Core Session command for the Core Assistant.
 * 
 * <p>
 * This command provides functionality for session management including starting 
 * a new session, which clears conversation history and resets context.
 * </p>
 */
public class CoreSessionCommand implements Command {
    
    /**
     * Stores the core assistant reference.
     */
    private final CoreAssistant assistant;

    /**
     * Constructor.
     *
     * @param assistant The core assistant instance
     */
    public CoreSessionCommand(CoreAssistant assistant) {
        this.assistant = assistant;
    }
    
    @Override
    public String execute(String input) {
        // Parse subcommand if provided
        String subCommand = "help";
        
        if (input != null && !input.trim().isEmpty()) {
            String[] parts = input.trim().split("\\s+", 2);
            subCommand = parts[0].toLowerCase();
        }
        
        switch (subCommand) {
            case "new":
                return startNewSession();
                
            case "help":
            default:
                return getHelp();
        }
    }
    
    /**
     * Starts a new session by clearing conversation history and resetting context.
     * 
     * @return Result message
     */
    private String startNewSession() {
        assistant.reset();
        return "Started new chat session";
    }
    
    /**
     * Gets help information for the session command.
     * 
     * @return Help text
     */
    private String getHelp() {
        return """
               Session management commands.
               
               Usage:
                 /session             - Show session command help
                 /session new         - Start a new session (clear history)
               """;
    }
    
    @Override
    public String getDescription() {
        return "Manages chat sessions including creating new ones, viewing history, etc.";
    }

    @Override
    public String getShortDescription() {
        return "Manages chat sessions";
    }
}
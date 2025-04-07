package com.manorrock.assistant.desktop;

import com.manorrock.assistant.api.Command;

/**
 * Desktop implementation of the session command.
 * 
 * <p>
 * This command manages session operations with UI integration.
 * </p>
 */
public class DesktopSessionCommand implements Command {
    
    /**
     * The desktop assistant reference.
     */
    private final DesktopAssistant assistant;
    
    /**
     * Constructor.
     * 
     * @param assistant the desktop assistant.
     */
    public DesktopSessionCommand(DesktopAssistant assistant) {
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
        // First reset the core assistant
        assistant.reset();
        
        // Then clear the UI
        if (assistant.getController() != null) {
            assistant.getController().clearResponseArea();
        }
        
        return "Started a new session. Conversation history cleared.";
    }
    
    /**
     * Gets help information for the session command.
     * 
     * @return Help text
     */
    private String getHelp() {
        return """
               Manages chat sessions.
               
               Usage:
                 /session new - Start a new conversation session
               """;
    }
    
    @Override
    public String getDescription() {
        return "Manages chat sessions";
    }
    
    @Override
    public String getShortDescription() {
        return "Manages chat sessions";
    }
}
package com.manorrock.assistant.desktop;

import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.core.CoreAssistant;
import com.manorrock.assistant.core.CoreAssistantMessage;

/**
 * The Desktop Assistant.
 * 
 * <p>
 * This class extends the CoreAssistant to provide desktop-specific functionality.
 * It registers desktop-specific command handlers for UI interactions.
 * </p>
 */
public class DesktopAssistant extends CoreAssistant {
    
    /**
     * Reference to the MainWindowController.
     */
    private MainWindowController controller;
    
    /**
     * Constructor.
     */
    public DesktopAssistant() {
        super();
        // Desktop-specific command initialization will go here
    }
    
    /**
     * Set the controller reference.
     * 
     * @param controller the MainWindowController
     */
    public void setController(MainWindowController controller) {
        this.controller = controller;
        
        // Register desktop-specific commands when controller is available
        if (controller != null) {
            registerDesktopCommands();
        }
    }
    
    /**
     * Get the controller reference.
     * 
     * @return the MainWindowController
     */
    public MainWindowController getController() {
        return controller;
    }
    
    /**
     * Register desktop-specific commands.
     */
    private void registerDesktopCommands() {
        // Register explain command with UI integration
        getCommandRegistry().registerCommand("explain", new DesktopExplainCommand(this));
        
        // Register session command with UI integration
        getCommandRegistry().registerCommand("session", new DesktopSessionCommand(this));
        
        // Register theme command for toggling between light and dark modes
        getCommandRegistry().registerCommand("theme", new DesktopThemeCommand(this));
    }
    
    /**
     * Process a message content directly and return the response content.
     * This is a convenience method for command handlers.
     * 
     * @param content the message content
     * @return the response content
     */
    public String processMessageContent(String content) {
        AssistantMessage message = new CoreAssistantMessage(content);
        AssistantMessage response = processMessage(message);
        return response.getContent();
    }
}
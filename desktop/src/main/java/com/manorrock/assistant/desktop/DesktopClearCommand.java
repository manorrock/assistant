package com.manorrock.assistant.desktop;

import com.manorrock.assistant.api.Command;

/**
 * Desktop implementation of the clear command.
 * 
 * <p>
 * This command clears the response area in the desktop UI.
 * </p>
 */
public class DesktopClearCommand implements Command {
    
    /**
     * The desktop assistant reference.
     */
    private final DesktopAssistant assistant;
    
    /**
     * Constructor.
     * 
     * @param assistant the desktop assistant.
     */
    public DesktopClearCommand(DesktopAssistant assistant) {
        this.assistant = assistant;
    }
    
    @Override
    public String execute(String input) {
        // UI operation must be handled by the controller
        if (assistant.getController() != null) {
            assistant.getController().clearResponseArea();
            return "Display cleared.";
        }
        return "Could not clear display. Controller not available.";
    }
    
    @Override
    public String getDescription() {
        return "Clears the response area";
    }
    
    @Override
    public String getShortDescription() {
        return "Clears the response area";
    }
}
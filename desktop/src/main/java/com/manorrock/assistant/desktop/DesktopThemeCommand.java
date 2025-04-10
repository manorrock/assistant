package com.manorrock.assistant.desktop;

import com.manorrock.assistant.api.Command;

/**
 * Desktop implementation of the theme command.
 * 
 * <p>
 * This command toggles between light and dark themes in the desktop UI.
 * </p>
 */
public class DesktopThemeCommand implements Command {
    
    /**
     * The desktop assistant reference.
     */
    private final DesktopAssistant assistant;
    
    /**
     * Constructor.
     * 
     * @param assistant the desktop assistant.
     */
    public DesktopThemeCommand(DesktopAssistant assistant) {
        this.assistant = assistant;
    }
    
    @Override
    public String execute(String input) {
        String[] parts = input.trim().split("\\s+", 2);
        String theme = parts.length > 1 ? parts[1].trim().toLowerCase() : "toggle";
        
        return switch (theme) {
            case "light" -> {
                assistant.getController().setTheme("light");
                yield "Theme set to light mode.";
            }
            case "dark" -> {
                assistant.getController().setTheme("dark");
                yield "Theme set to dark mode.";
            }
            case "toggle" -> {
                boolean isDarkTheme = assistant.getController().toggleTheme();
                yield "Theme toggled to " + (isDarkTheme ? "dark" : "light") + " mode.";
            }
            default -> "Unknown theme: " + theme + ". Available themes: light, dark, toggle.";
        };
    }
    
    @Override
    public String getDescription() {
        return "Toggles between light and dark themes, or sets a specific theme.\n" +
               "Usage:\n" +
               "/theme [light|dark|toggle]\n" +
               "- No argument or 'toggle': Toggle between light and dark theme\n" +
               "- 'light': Set light theme\n" +
               "- 'dark': Set dark theme";
    }
    
    @Override
    public String getShortDescription() {
        return "Toggles between light and dark themes";
    }
}

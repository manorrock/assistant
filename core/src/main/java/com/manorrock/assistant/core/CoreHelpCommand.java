package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Core Help command for the Core Assistant.
 * 
 * <p>
 * This command provides help by listing all available commands in the Core Assistant.
 * </p>
 */
public class CoreHelpCommand implements Command {

    private final CoreAssistant assistant;

    /**
     * Constructor.
     *
     * @param assistant The core assistant instance
     */
    public CoreHelpCommand(CoreAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public String execute(String input) {
        return executeToString(input);
    }

    @Override
    public String executeToString(String input) {
        StringBuilder result = new StringBuilder();
        result.append("Manorrock Assistant 25.3.7\n\n");
        result.append("Available commands:\n");
        
        var commandNames = assistant.getCommandRegistry().getCommandNames();
        var sortedNames = new ArrayList<>(commandNames);
        sortedNames.sort(String::compareTo); // Sort alphabetically
        
        // Find the longest command name to properly align descriptions
        int maxLength = sortedNames.stream()
            .mapToInt(String::length)
            .max()
            .orElse(0);
        
        // Format string with proper spacing
        String format = String.format("/%%-%ds - %%s%%n", maxLength);
        
        for (String name : sortedNames) {
            Command cmd = assistant.getCommandRegistry().getCommand(name);
            String description = cmd.getShortDescription();
            if (description == null || description.isEmpty()) {
                description = "No description available";
            }
            result.append(String.format(format, name, description));
        }
        
        return result.toString();
    }

    @Override
    public InputStream executeToStream(String input) {
        return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getDescription() {
        return "Show available commands";
    }

    @Override
    public String getShortDescription() {
        return "Show available commands";
    }
}
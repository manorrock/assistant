package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Core Explain command for the Core Assistant.
 * 
 * <p>
 * This command provides functionality to explain text from clipboard or a specified file.
 * It passes the text to the LLM for analysis and explanation.
 * </p>
 */
public class CoreExplainCommand implements Command {
    
    private final CoreAssistant assistant;

    public CoreExplainCommand(CoreAssistant assistant) {
        this.assistant = assistant;
    }
    
    @Override
    public String execute(String input) {
        return executeToString(input);
    }
    
    @Override
    public String executeToString(String input) {
        String textToExplain = null;
        StringBuilder resultBuilder = new StringBuilder();
        
        // Parse optional file path if provided
        String filePath = input != null ? input.trim() : "";
        
        if (!filePath.isEmpty()) {
            try {
                textToExplain = Files.readString(Paths.get(filePath));
                resultBuilder.append("Explaining content from file: ").append(filePath).append("\n\n");
            } catch (IOException e) {
                return "Error reading file: " + e.getMessage();
            }
        } else {
            try {
                textToExplain = getClipboardContent();
                resultBuilder.append("Explaining content from clipboard:\n\n");
            } catch (Exception e) {
                return "Failed to access clipboard: " + e.getMessage() + 
                       "\nUsage: /explain [file_path] - Explains text from clipboard or specified file";
            }
        }
        
        if (textToExplain == null || textToExplain.trim().isEmpty()) {
            return "No content found to explain.";
        }
        
        String promptPrefix = "Please explain the following text in a clear and concise manner:\n\n";
        var response = assistant.processMessage(new CoreAssistantMessage(promptPrefix + textToExplain));
        if (response == null) {
            return "Failed to get explanation from assistant.";
        }
        
        return resultBuilder.toString() + response.getContent();
    }
    
    @Override
    public InputStream executeToStream(String input) {
        return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    public String getDescription() {
        return "Explains text from clipboard or specified file";
    }
    
    @Override 
    public String getShortDescription() {
        return "Explains text from clipboard or file";
    }
    
    /**
     * Gets content from the system clipboard. This is a placeholder since this is a core implementation
     * and doesn't have access to platform-specific clipboard APIs.
     * 
     * @return Content from clipboard
     * @throws Exception if clipboard access fails
     */
    private String getClipboardContent() throws Exception {
        // Core implementation can't access clipboard
        throw new UnsupportedOperationException("Clipboard access is not supported in core implementation");
    }
}
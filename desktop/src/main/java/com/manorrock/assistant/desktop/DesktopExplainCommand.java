package com.manorrock.assistant.desktop;

import com.manorrock.assistant.api.Command;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.scene.input.Clipboard;

/**
 * Desktop implementation of the explain command.
 * 
 * <p>
 * This command explains text from clipboard or from a file in the desktop UI.
 * </p>
 */
public class DesktopExplainCommand implements Command {
    
    /**
     * The desktop assistant reference.
     */
    private final DesktopAssistant assistant;
    
    /**
     * Constructor.
     * 
     * @param assistant the desktop assistant.
     */
    public DesktopExplainCommand(DesktopAssistant assistant) {
        this.assistant = assistant;
    }
    
    @Override
    public String execute(String input) {
        String[] parts = input.trim().split("\\s+", 2);
        boolean hasFilePath = parts.length > 1 && !parts[1].trim().isEmpty();
        
        if (hasFilePath) {
            return explainFileContent(parts[1].trim());
        }
        
        return explainClipboard();
    }
    
    /**
     * Explain content from the clipboard.
     * 
     * @return the result
     */
    private String explainClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String clipboardContent = clipboard.getString();

        if (clipboardContent != null && !clipboardContent.isEmpty()) {
            String prompt = "Please explain the content below the line\n-----------------------------------------\n"
                + clipboardContent;
            
            // Send the prompt to the LLM for processing
            return assistant.processMessageContent(prompt);
        } else {
            return "No text found in clipboard. Copy some text and try again.";
        }
    }
    
    /**
     * Explain content from a file.
     * 
     * @param filePath the file path
     * @return the result
     */
    private String explainFileContent(String filePath) {
        Path file = Paths.get(filePath);
        
        try {
            String fileContent = Files.readString(file);
            String prompt = "Please explain the content below the line\n-----------------------------------------\n"
                + fileContent;
            
            // Send the prompt to the LLM for processing
            return assistant.processMessageContent(prompt);
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }
    
    @Override
    public String getDescription() {
        return "Explains text from clipboard or file";
    }
    
    @Override
    public String getShortDescription() {
        return "Explains text from clipboard or file";
    }
}
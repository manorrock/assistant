package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.core.CoreAssistantMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Command implementation that explains text from clipboard or file.
 */
public class CLIExplainCommand implements Command {

    /**
     * Stores the assistant instance.
     */
    private final Assistant assistant;

    /**
     * Constuctor to initialize the command with the assistant.
     * 
     * @param assistant the assistant.
     */
    public CLIExplainCommand(Assistant assistant) {
        this.assistant = assistant;
    }
    
    @Override
    public String execute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return getDescription();
        }

        String[] parts = input.trim().split("\\s+", 2);
        String subCommand = parts[0];
        String textToExplain = null;

        switch (subCommand) {
            case "clipboard":
                try {
                    textToExplain = getClipboardContent();
                } catch (Exception e) {
                    return "Failed to access clipboard: " + e.getMessage();
                }
                break;
            case "file":
                if (parts.length < 2) {
                    return "Error: No file path provided.\nUsage: /explain file <filepath>";
                }
                try {
                    textToExplain = Files.readString(Paths.get(parts[1]));
                } catch (IOException e) {
                    return "Error reading file: " + e.getMessage();
                }
                break;
            default:
                return getDescription();
        }
        
        if (textToExplain == null || textToExplain.trim().isEmpty()) {
            return "No content found to explain.";
        }

        String promptPrefix = """
                Please explain the following in a clear and concise manner.
                Do NOT go into making ANY suggestions. Do not add ANY conclusions.
                Response format MUST be structured as follows:
                Original text:
                <original text>
                Explanation:
                <explanation>
                """;

        var response = assistant.processMessage(new CoreAssistantMessage(promptPrefix + textToExplain));
        if (response == null) {
            return "Failed to get explanation from assistant.";
        }
        
        return response.getContent();
    }
    
    @Override
    public String getDescription() {
        return """
               Explains text from clipboard or a file.
               
               Usage:
                 /explain              - Show this help text
                 /explain clipboard    - Explain text from clipboard
                 /explain file <path>  - Explain text from specified file
               """;
    }
    
    @Override
    public String getShortDescription() {
        return "Explains text from clipboard or file";
    }
    
    /**
     * Gets content from the system clipboard using platform-specific commands.
     * 
     * @return String content from clipboard
     * @throws Exception if clipboard access fails
     */
    private String getClipboardContent() throws Exception {
        // For Mac/Linux, we can use the 'pbpaste' or 'xclip' commands
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        
        if (os.contains("mac")) {
            pb = new ProcessBuilder("pbpaste");
        } else if (os.contains("nix") || os.contains("nux")) {
            pb = new ProcessBuilder("xclip", "-selection", "clipboard", "-o");
        } else if (os.contains("win")) {
            pb = new ProcessBuilder("powershell.exe", "-command", "Get-Clipboard");
        } else {
            throw new UnsupportedOperationException("Clipboard access not supported on this OS");
        }
        
        Process process = pb.start();
        String content = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to get clipboard content, exit code: " + exitCode);
        }
        
        return content;
    }
}

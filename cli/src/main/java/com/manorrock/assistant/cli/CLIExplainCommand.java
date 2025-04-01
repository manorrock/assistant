package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.core.CoreAssistantMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        return executeToString(input);
    }
    
    @Override
    public String executeToString(String input) {
        String textToExplain = null;
        
        // Parse optional file path if provided
        String filePath = null;
        if (input != null && !input.trim().isEmpty()) {
            filePath = input.trim();
        }
        
        StringBuilder resultBuilder = new StringBuilder();
        
        if (filePath != null && !filePath.isEmpty()) {
            // Read from file
            try {
                textToExplain = Files.readString(Paths.get(filePath));
            } catch (IOException e) {
                return "Error reading file: " + e.getMessage();
            }
        } else {
            // Read from clipboard
            try {
                textToExplain = getClipboardContent();
            } catch (Exception e) {
                return "Failed to access clipboard: " + e.getMessage();
            }
        }
        
        if (textToExplain != null && !textToExplain.trim().isEmpty()) {
            String promptPrefix = """
                        Please explain the following in a clear and concise manner.\n
                        Do NOT go into making ANY suggestions. Do not add ANY conclusions.\n
                        Response format MUST be structured as follows:\n
                        Original text:\n
                        <original text>\n
                        Explanation:\n
                        <explanation>\n
                        """;
            var response = assistant.processMessage(new CoreAssistantMessage(promptPrefix + textToExplain));
            if (response == null) {
                return "Failed to get explanation from assistant.";
            }
        
            return resultBuilder.toString() + response.getContent();            
        } else {
            return "No content found to explain.";
        }
    }
    
    @Override
    public InputStream executeToStream(String input) {
        return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    public String getDescription() {
        return "Explains text from clipboard or specified file";
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

    @Override
    public String getShortDescription() {
        return "Explains text from clipboard or specified file";
    }
}

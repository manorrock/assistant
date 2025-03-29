package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.Command;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Command implementation that explains text from clipboard or file.
 */
public class CLIExplainCommand implements Command {
    
    private final Consumer<String> messageProcessor;
    
    /**
     * Constructor that takes a message processor function.
     * 
     * @param messageProcessor Function to process the explain prompt
     */
    public CLIExplainCommand(Consumer<String> messageProcessor) {
        this.messageProcessor = messageProcessor;
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
                resultBuilder.append("Explaining content from file: ").append(filePath);
            } catch (IOException e) {
                return "Error reading file: " + e.getMessage();
            }
        } else {
            // Read from clipboard
            try {
                textToExplain = getClipboardContent();
                resultBuilder.append("Explaining content from clipboard");
            } catch (Exception e) {
                return "Failed to access clipboard: " + e.getMessage() + 
                       "\nUsage: /explain [file_path] - Explains text from clipboard or specified file";
            }
        }
        
        if (textToExplain != null && !textToExplain.trim().isEmpty()) {
            String promptPrefix = "Please explain the following text in a clear and concise manner:\n\n";
            // Process the message asynchronously
            messageProcessor.accept(promptPrefix + textToExplain);
            return resultBuilder.toString();
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

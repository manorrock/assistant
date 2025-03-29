package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.AssistantMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Core Source command for the Core Assistant.
 * 
 * <p>
 * This command executes commands from a file by reading each line
 * and processing it as either a command or a prompt message.
 * </p>
 */
public class CoreSourceCommand implements Command {

    /**
     * Stores the core assistant reference.
     */
    private final CoreAssistant assistant;

    /**
     * Constructor.
     *
     * @param assistant The core assistant instance
     */
    public CoreSourceCommand(CoreAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public String execute(String input) {
        return executeToString(input);
    }

    @Override
    public String executeToString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Usage: /source <file_path>";
        }

        String filePath = input.trim();
        Path path = Paths.get(filePath);

        try {
            String content = Files.readString(path);
            StringBuilder messageBuilder = new StringBuilder();

            for (String line : content.split("\n")) {
                String trimmedLine = line.trim();

                // Handle line continuation with backslash
                if (line.endsWith("\\") || trimmedLine.endsWith("\\")) {
                    messageBuilder.append(line, 0, line.lastIndexOf('\\')).append("\n");
                    continue;
                }

                messageBuilder.append(line);

                String fullMessage = messageBuilder.toString().trim();
                if (!fullMessage.isEmpty()) {
                    processMessage(fullMessage);
                }
                messageBuilder.setLength(0);
            }

            // Process any remaining content
            String remaining = messageBuilder.toString().trim();
            if (!remaining.isEmpty()) {
                processMessage(remaining);
            }

            return "Successfully executed commands from " + filePath;
        } catch (IOException e) {
            return "Error reading source file: " + e.getMessage();
        }
    }

    /**
     * Process a message from the file, either as a command or a prompt.
     * 
     * @param message The message to process
     */
    private void processMessage(String message) {
        AssistantMessage assistantMessage = new CoreAssistantMessage(message);
        assistant.processMessage(assistantMessage);
    }

    @Override
    public InputStream executeToStream(String input) {
        return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getDescription() {
        return "Execute commands from a file";
    }

    @Override
    public String getShortDescription() {
        return "Execute commands from a file";
    }
}
package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.api.Command;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Command implementation that exports conversation history to text or JSON format.
 */
public class CLIExportCommand implements Command {

    /**
     * Stores the CLI instance.
     */
    private final CLI cli;

    /**
     * Constructor to initialize the command with the CLI instance.
     * 
     * @param cli the CLI instance to access conversation history.
     */
    public CLIExportCommand(CLI cli) {
        this.cli = cli;
    }
    
    @Override
    public String execute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return getDescription();
        }

        String[] parts = input.trim().split("\\s+", 3);
        if (parts.length < 2) {
            return getDescription();
        }

        String format = parts[0].toLowerCase();
        String filePath = parts[1];
        
        // Generate default filename if not provided
        if (filePath.equals("auto")) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            filePath = "conversation_" + timestamp + (format.equals("json") ? ".json" : ".txt");
        }
        
        List<AssistantMessage> history = cli.getConversationHistory();
        if (history.isEmpty()) {
            return "No conversation history to export.";
        }
        
        try {
            switch (format) {
                case "text":
                    return exportToText(history, filePath);
                case "json":
                    return exportToJson(history, filePath);
                default:
                    return "Unsupported format. Use 'text' or 'json'.";
            }
        } catch (IOException e) {
            return "Failed to export conversation: " + e.getMessage();
        }
    }
    
    /**
     * Exports conversation history to text format.
     * 
     * @param history the conversation history
     * @param filePath the file path to save to
     * @return status message
     * @throws IOException if file writing fails
     */
    private String exportToText(List<AssistantMessage> history, String filePath) throws IOException {
        File file = new File(filePath);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Manorrock Assistant Conversation\n");
            writer.write("Date: " + LocalDateTime.now().toString() + "\n\n");
            
            for (AssistantMessage message : history) {
                String role = "user".equals(message.getType()) ? "You" : "Assistant";
                writer.write(role + ": " + message.getContent() + "\n\n");
            }
        }
        
        return "Conversation exported to text file: " + Paths.get(filePath).toAbsolutePath();
    }
    
    /**
     * Exports conversation history to JSON format.
     * 
     * @param history the conversation history
     * @param filePath the file path to save to
     * @return status message
     * @throws IOException if file writing fails
     */
    private String exportToJson(List<AssistantMessage> history, String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        
        rootNode.put("timestamp", LocalDateTime.now().toString());
        rootNode.put("format", "ManorrockAssistantConversation");
        
        ArrayNode messagesNode = rootNode.putArray("messages");
        for (AssistantMessage message : history) {
            ObjectNode messageNode = messagesNode.addObject();
            messageNode.put("role", message.getType());
            messageNode.put("content", message.getContent());
            
            // Handle null timestamp
            LocalDateTime timestamp = message.getTimestamp();
            if (timestamp != null) {
                messageNode.put("timestamp", timestamp.toString());
            } else {
                messageNode.put("timestamp", LocalDateTime.now().toString());
            }
        }
        
        File file = new File(filePath);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, rootNode);
        
        return "Conversation exported to JSON file: " + Paths.get(filePath).toAbsolutePath();
    }
    
    @Override
    public String getDescription() {
        return """
               Exports conversation history to text or JSON format.
               
               Usage:
                 /export                 - Show this help text
                 /export text <filepath> - Export to text file
                 /export json <filepath> - Export to JSON file
                 /export text auto       - Export to auto-generated text file
                 /export json auto       - Export to auto-generated JSON file
               
               Examples:
                 /export text conversation.txt
                 /export json history.json
                 /export text auto
               """;
    }
    
    @Override
    public String getShortDescription() {
        return "Exports conversation history to text or JSON format";
    }
}

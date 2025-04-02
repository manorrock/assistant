package com.manorrock.assistant.core;

import org.junit.jupiter.api.Test;

import com.manorrock.assistant.api.AssistantMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreSourceCommandTest {

    @Test
    void testExecute_withValidFile() throws IOException {
        // Arrange
        CoreAssistant mockAssistant = new CoreAssistant() {
            @Override
            public AssistantMessage processMessage(AssistantMessage message) {
                // Mock implementation
                assertTrue(message.getContent().startsWith("Test"));
                return new CoreAssistantMessage("Processed: " + message.getContent());
            }
        };
        CoreSourceCommand command = new CoreSourceCommand(mockAssistant);

        Path tempFile = Files.createTempFile("test", ".txt");
        Files.writeString(tempFile, "Test command 1\nTest command 2");

        // Act
        String result = command.execute(tempFile.toString());

        // Assert
        assertEquals("Successfully executed commands from " + tempFile.toString(), result);

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testExecute_withInvalidFile() {
        // Arrange
        CoreAssistant mockAssistant = new CoreAssistant() {
            @Override
            public AssistantMessage processMessage(AssistantMessage message) {
                // Mock implementation
                return new CoreAssistantMessage("Processed: " + message.getContent());
            }
        };
        CoreSourceCommand command = new CoreSourceCommand(mockAssistant);

        // Act
        String result = command.execute("nonexistent_file.txt");

        // Assert
        assertTrue(result.startsWith("Error reading source file:"));
    }

    @Test
    void testExecute_withEmptyInput() {
        // Arrange
        CoreAssistant mockAssistant = new CoreAssistant() {
            @Override
            public AssistantMessage processMessage(AssistantMessage message) {
                // Mock implementation
                return new CoreAssistantMessage("Processed: " + message.getContent());
            }
        };
        CoreSourceCommand command = new CoreSourceCommand(mockAssistant);

        // Act
        String result = command.execute("");

        // Assert
        assertEquals("Usage: /source <file_path>", result);
    }
}
package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CoreExplainCommandTest {

    private CoreAssistant mockAssistant;
    private CoreExplainCommand explainCommand;
    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        mockAssistant = mock(CoreAssistant.class);
        explainCommand = new CoreExplainCommand(mockAssistant);
    }

    @Test
    public void testExplainFromFile() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        String testContent = "Test content to explain";
        Files.write(testFile, testContent.getBytes(StandardCharsets.UTF_8));

        // Mock the assistant's response
        when(mockAssistant.processMessage(any())).thenReturn(
            new CoreAssistantMessage("Explanation: " + testContent)
        );

        // Execute the command
        String result = explainCommand.executeToString(testFile.toString());

        // Verify results
        assertTrue(result.contains("Explaining content from file:"));
        assertTrue(result.contains("Explanation: " + testContent));
    }

    @Test
    public void testExplainFromFileNotFound() {
        String result = explainCommand.executeToString("/nonexistent/file.txt");
        assertTrue(result.startsWith("Error reading file:"));
    }

    @Test
    public void testExplainEmptyInput() {
        // This should try to read from clipboard, which will likely fail in test environment
        String result = explainCommand.executeToString("");
        assertTrue(result.contains("Failed to access clipboard:") || 
                  result.contains("No content found to explain."));
    }

    @Test
    public void testExplainEmptyFile() throws IOException {
        // Create an empty test file
        Path testFile = tempDir.resolve("empty.txt");
        Files.write(testFile, new byte[0]);

        String result = explainCommand.executeToString(testFile.toString());
        assertEquals("No content found to explain.", result);
    }

    @Test
    public void testGetDescription() {
        String description = explainCommand.getDescription();
        assertEquals("Explains text from clipboard or specified file", description);
    }

    @Test
    public void testGetShortDescription() {
        String shortDescription = explainCommand.getShortDescription();
        assertEquals("Explains text from clipboard or file", shortDescription);
    }

    @Test
    public void testExecuteToStream() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("streamTest.txt");
        String testContent = "Test content for stream";
        Files.write(testFile, testContent.getBytes(StandardCharsets.UTF_8));

        // Mock the assistant's response
        when(mockAssistant.processMessage(any())).thenReturn(
            new CoreAssistantMessage("Stream explanation: " + testContent)
        );

        // Execute the command
        InputStream result = explainCommand.executeToStream(testFile.toString());

        // Read the result
        String streamContent = new String(result.readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(streamContent.contains("Explaining content from file:"));
        assertTrue(streamContent.contains("Stream explanation: " + testContent));
    }

    @Test
    public void testExecuteDelegatesToExecuteToString() {
        // Create a spy to verify the delegation
        CoreExplainCommand spy = spy(explainCommand);
        String testInput = "test input";
        
        spy.execute(testInput);
        
        verify(spy).executeToString(testInput);
    }
}
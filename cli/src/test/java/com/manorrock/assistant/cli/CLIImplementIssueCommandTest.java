package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.Llm;
import com.manorrock.assistant.api.LlmManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for the CLIImplementIssueCommand class.
 */
class CLIImplementIssueCommandTest {

    /**
     * The command to test.
     */
    private CLIImplementIssueCommand command;

    /**
     * Mock for the Assistant.
     */
    @Mock
    private Assistant assistant;

    /**
     * Mock for the LlmManager.
     */
    @Mock
    private LlmManager llmManager;

    /**
     * Mock for the Llm.
     */
    @Mock
    private Llm llm;

    /**
     * Set up the test.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Configure the mocks
        when(assistant.getLlmManager()).thenReturn(llmManager);
        when(assistant.getActiveLlm()).thenReturn("testLlm");
        when(llmManager.getLlm("testLlm")).thenReturn(llm);
        when(llm.getProperties()).thenReturn(new Properties());
        when(llm.process(anyString())).thenReturn("Test implementation response from LLM");
        
        // Create the command
        command = new CLIImplementIssueCommand(assistant);
    }

    /**
     * Test getting help text.
     */
    @Test
    void testGetHelpText() {
        String result = command.execute("help");
        assertTrue(result.contains("Implement Issue Command Help:"));
        assertTrue(result.contains("/implement-issue file <path>"));
    }

    /**
     * Test implementing an issue from text.
     */
    @Test
    void testImplementIssueFromText() {
        String issueText = """
                           # Test Issue
                           
                           ## Description
                           This is a test issue description.
                           
                           ## Acceptance Criteria
                           - Criterion A
                           - Criterion B
                           
                           ## Priority
                           MEDIUM
                           
                           ## Labels
                           - test
                           - example
                           """;
        
        String result = command.execute(issueText);
        
        assertTrue(result.contains("Implementation for Issue: Test Issue"));
        assertTrue(result.contains("Implementation Result"));
        assertTrue(result.contains("Test implementation response from LLM"));
        assertTrue(result.contains("Verification Against Acceptance Criteria"));
        assertTrue(result.contains("- [ ] Criterion A"));
        assertTrue(result.contains("- [ ] Criterion B"));
    }

    /**
     * Test implementing an issue from a file.
     */
    @Test
    void testImplementIssueFromFile() throws IOException {
        // Create a temporary file with issue content
        Path tempFile = Files.createTempFile("test-issue", ".md");
        String issueText = """
                           # File Issue
                           
                           ## Description
                           This is a test issue from a file.
                           
                           ## Acceptance Criteria
                           - File Criterion A
                           - File Criterion B
                           
                           ## Priority
                           HIGH
                           """;
        
        Files.writeString(tempFile, issueText);
        
        try {
            String result = command.execute("file " + tempFile.toString());
            
            assertTrue(result.contains("Implementation for Issue: File Issue"));
            assertTrue(result.contains("File Criterion A"));
            assertTrue(result.contains("File Criterion B"));
        } finally {
            // Clean up the temporary file
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Test setting implementation options.
     */
    @Test
    void testSetImplementationOptions() {
        // Create path to target/test-output relative to the project root
        Path projectDir = Path.of(System.getProperty("user.dir"));
        Path targetTestOutput = projectDir.resolve("target").resolve("test-output");
        
        // Ensure the target directory exists
        try {
            Files.createDirectories(projectDir.resolve("target"));
        } catch (IOException e) {
            // Directory likely already exists, continue
        }
        
        String result = command.execute("options output_dir=" + targetTestOutput + ",verbose=true,max_tokens=1000");
        
        assertTrue(result.contains("Successfully set options:"));
        assertTrue(result.contains("verbose=true"));
        assertTrue(result.contains("max_tokens=1000"));
        
        // Verify the options were set
        assertEquals(targetTestOutput.toString(), System.getProperty("implement.issue.output.dir"));
        assertEquals("true", System.getProperty("implement.issue.verbose"));
        assertEquals("1000", System.getProperty("implement.issue.max.tokens"));
        
        // Verify the directory was created
        assertTrue(Files.exists(targetTestOutput));
        
        // Test with invalid options
        result = command.execute("options invalid_option=value");
        
        assertTrue(result.contains("Invalid options: invalid_option=value"));
        assertTrue(result.contains("Valid options are:"));
    }

    /**
     * Test for the command description.
     */
    @Test
    void testGetDescription() {
        assertTrue(command.getDescription().contains("Implements features or fixes from issue descriptions"));
    }

    /**
     * Test for the short description.
     */
    @Test
    void testGetShortDescription() {
        assertEquals("Implement issues automatically", command.getShortDescription());
    }
}
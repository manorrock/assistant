package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.Llm;
import com.manorrock.assistant.api.LlmManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for CLIIssueTemplateCommand class.
 */
class CLIIssueTemplateCommandTest {

    @Mock
    private Assistant assistant;
    
    @Mock
    private LlmManager llmManager;
    
    @Mock
    private Llm llm;
    
    private CLIIssueTemplateCommand command;
    
    private Properties properties;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        properties = new Properties();
        
        // Setup mock behaviors
        when(assistant.getLlmManager()).thenReturn(llmManager);
        when(assistant.getActiveLlm()).thenReturn("test-llm");
        when(llmManager.getLlm("test-llm")).thenReturn(llm);
        when(llm.getProperties()).thenReturn(properties);
        
        // Create command instance with mocked assistant
        command = new CLIIssueTemplateCommand(assistant) {
            // Override to avoid classpath resource loading issues in tests
            @Override
            protected void loadIssueTemplate() {
                setIssueTemplate("Test template with {{PROJECT_NAME}} and {{LANGUAGE}}");
            }
        };
    }
    
    @Test
    void testGetHelpText() {
        String result = command.execute("help");
        assertNotNull(result);
        assertTrue(result.contains("Issue Template Command Help"));
        assertTrue(result.contains("/issue-template apply"));
    }
    
    @Test
    void testApplyTemplate() {
        String result = command.execute("apply");
        
        assertTrue(result.contains("Issue implementation template applied"));
        assertEquals("Test template with Unknown Project and Java", properties.getProperty("systemMessage"));
        verify(llm).getProperties();
    }
    
    @Test
    void testViewTemplate() {
        String result = command.execute("view");
        
        assertTrue(result.contains("Issue Implementation Template"));
        assertTrue(result.contains("Test template with Unknown Project and Java"));
    }
    
    @Test
    void testSetProjectContext() {
        // Set the project name
        String setResult = command.execute("set-context PROJECT_NAME=TestProject");
        assertTrue(setResult.contains("Project context updated"));
        
        // Verify it was applied
        String viewResult = command.execute("view");
        assertTrue(viewResult.contains("Test template with TestProject and Java"));
        
        // Update language
        setResult = command.execute("set-context LANGUAGE=Python");
        assertTrue(setResult.contains("Project context updated"));
        
        // Verify both updates were applied
        viewResult = command.execute("view");
        assertTrue(viewResult.contains("Test template with TestProject and Python"));
    }
    
    @Test
    void testShowProjectContext() {
        // Set some context values
        command.execute("set-context PROJECT_NAME=TestProject");
        command.execute("set-context LANGUAGE=TypeScript");
        
        // Get the context
        String result = command.execute("show-context");
        
        assertTrue(result.contains("Current Project Context"));
        assertTrue(result.contains("PROJECT_NAME: TestProject"));
        assertTrue(result.contains("LANGUAGE: TypeScript"));
    }
    
    @Test
    void testInvalidCommand() {
        String result = command.execute("invalid-command");
        assertTrue(result.contains("Unknown command"));
    }
    
    @Test
    void testInvalidContextKey() {
        String result = command.execute("set-context INVALID_KEY=Value");
        assertTrue(result.contains("Invalid context key"));
    }
    
    @Test
    void testDescription() {
        assertNotNull(command.getDescription());
        assertFalse(command.getDescription().isEmpty());
    }
    
    @Test
    void testShortDescription() {
        assertNotNull(command.getShortDescription());
        assertFalse(command.getShortDescription().isEmpty());
    }
}
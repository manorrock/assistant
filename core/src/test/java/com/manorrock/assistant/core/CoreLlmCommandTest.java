package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.manorrock.assistant.api.Llm;
import com.manorrock.assistant.api.LlmManager;

/**
 * Unit tests for the CoreLlmCommand class.
 */
public class CoreLlmCommandTest {

    private CoreAssistant mockAssistant;
    private LlmManager mockLlmManager;
    private CoreLlmCommand command;

    @BeforeEach
    public void setUp() {
        mockAssistant = mock(CoreAssistant.class);
        mockLlmManager = mock(LlmManager.class);
        when(mockAssistant.getLlmManager()).thenReturn(mockLlmManager);
        command = new CoreLlmCommand(mockAssistant);
    }

    @Test
    public void testGetDescription() {
        String description = command.getDescription();
        assert(description.contains("/llm reset"));
        assert(description.contains("Reset the LLM and clear memory"));
    }

    @Test
    public void testResetLlm_withNoActiveLlm() {
        // Setup no active LLM
        when(mockAssistant.getActiveLlm()).thenReturn(null);
        
        // Execute the reset command
        String result = command.execute("reset");
        
        // Verify the result indicates no active LLM is set
        assertEquals("No active LLM set. Use '/llm set <llm-name>' to set an active LLM first.", result);
    }

    @Test
    public void testResetLlm_withNonExistentLlm() {
        // Setup an active LLM that doesn't exist
        String llmName = "nonExistentLlm";
        when(mockAssistant.getActiveLlm()).thenReturn(llmName);
        when(mockLlmManager.getLlm(llmName)).thenReturn(null);
        
        // Execute the reset command
        String result = command.execute("reset");
        
        // Verify the result indicates the LLM is not registered
        assertEquals("Active LLM '" + llmName + "' is not registered.", result);
    }

    @Test
    public void testResetLlm_successful() {
        // Setup a mock LLM
        String llmName = "testLlm";
        Llm mockLlm = mock(Llm.class);
        
        when(mockAssistant.getActiveLlm()).thenReturn(llmName);
        when(mockLlmManager.getLlm(llmName)).thenReturn(mockLlm);
        
        // Execute the reset command
        String result = command.execute("reset");
        
        // Verify that destroy and init were called on the LLM
        verify(mockLlm).destroy();
        verify(mockLlm).init();
        
        // Verify the result message
        assertEquals("LLM has been reset and memory cleared.", result);
    }

    @Test
    public void testUseActiveLlm_withValidLlm() {
        // Setup a valid LLM
        String llmName = "validLlm";
        Llm mockLlm = mock(Llm.class);
        
        when(mockLlmManager.getLlm(llmName)).thenReturn(mockLlm);
        
        // Execute the use command
        String result = command.execute("use " + llmName);
        
        // Verify that setActiveLlm was called with the correct name
        verify(mockAssistant).setActiveLlm(llmName);
        
        // Verify the result message
        assertEquals("Active LLM set to: " + llmName, result);
    }

    @Test
    public void testUseActiveLlm_withInvalidLlm() {
        // Setup an invalid LLM
        String llmName = "invalidLlm";
        
        when(mockLlmManager.getLlm(llmName)).thenReturn(null);
        
        // Execute the use command
        String result = command.execute("use " + llmName);
        
        // Verify that setActiveLlm was NOT called
        verify(mockAssistant, never()).setActiveLlm(any());
        
        // Verify the result message
        assertEquals("LLM '" + llmName + "' is not available.", result);
    }

    @Test
    public void testListLlms_empty() {
        // Setup an empty LLM list
        when(mockLlmManager.getLlms()).thenReturn(java.util.Collections.emptyMap());
        
        // Execute the list command
        String result = command.execute("list");
        
        // Verify the result message for empty list
        assertEquals("No LLMs are currently registered.", result);
    }
    
    @Test
    public void testListLlms_withMultipleLlms() {
        // Setup multiple mock LLMs
        String activeLlmName = "activeLlm";
        Llm activeMockLlm = mock(CoreLlm.class);
        Llm otherMockLlm = mock(CoreLlm.class);
        
        // Create properties for the LLMs
        java.util.Properties activeProps = new java.util.Properties();
        activeProps.setProperty("vendor", "OLLAMA");
        activeProps.setProperty("modelName", "llama3.2");
        
        java.util.Properties otherProps = new java.util.Properties();
        otherProps.setProperty("vendor", "OPENAI");
        otherProps.setProperty("modelName", "gpt-4");
        
        // Setup the mocks to return the properties
        when(activeMockLlm.getProperties()).thenReturn(activeProps);
        when(otherMockLlm.getProperties()).thenReturn(otherProps);
        
        // Create the LLM map
        java.util.Map<String, Llm> llmMap = new java.util.HashMap<>();
        llmMap.put(activeLlmName, activeMockLlm);
        llmMap.put("otherLlm", otherMockLlm);
        
        // Set the active LLM
        when(mockAssistant.getActiveLlm()).thenReturn(activeLlmName);
        when(mockLlmManager.getLlms()).thenReturn(llmMap);
        
        // Execute the list command
        String result = command.execute("list");
        
        // Print the result to debug
        System.out.println("Result: " + result);
        
        // Verify the result contains all expected elements
        assert(result.contains("Available LLMs:"));
        assert(result.contains("* " + activeLlmName));
        assert(result.contains("OLLAMA"));
        assert(result.contains("llama3.2"));
        assert(result.contains("otherLlm"));
        assert(result.contains("OPENAI"));
        assert(result.contains("gpt-4"));
        assert(result.contains("* = active LLM"));
        assert(result.contains("Use '/llm set"));
    }

    @Test
    public void testAddLlm_successful() {
        // Setup for a new LLM name that doesn't exist yet
        String newLlmName = "newLlm";
        when(mockLlmManager.getLlm(newLlmName)).thenReturn(null);
        
        // Execute the add command
        String result = command.execute("add " + newLlmName);
        
        // Verify that registerLlm was called with the correct parameters
        verify(mockLlmManager).registerLlm(eq(newLlmName), any(CoreLlm.class));
        
        // Verify the result message
        assert(result.contains("New LLM '" + newLlmName + "' has been added"));
        assert(result.contains("Use '/llm set " + newLlmName + "' to activate it"));
    }
    
    @Test
    public void testAddLlm_alreadyExists() {
        // Setup for an LLM name that already exists
        String existingLlmName = "existingLlm";
        Llm mockLlm = mock(Llm.class);
        when(mockLlmManager.getLlm(existingLlmName)).thenReturn(mockLlm);
        
        // Execute the add command
        String result = command.execute("add " + existingLlmName);
        
        // Verify that registerLlm was NOT called
        verify(mockLlmManager, never()).registerLlm(anyString(), any(Llm.class));
        
        // Verify the result message
        assertEquals("LLM with name '" + existingLlmName + "' already exists.", result);
    }

    @Test
    public void testRemoveLlm_withNonExistentLlm() {
        // Setup for an LLM name that doesn't exist
        String nonExistentLlmName = "nonExistentLlm";
        when(mockLlmManager.getLlm(nonExistentLlmName)).thenReturn(null);
        
        // Execute the remove command
        String result = command.execute("remove " + nonExistentLlmName);
        
        // Verify the result message
        assertEquals("LLM with name '" + nonExistentLlmName + "' does not exist.", result);
    }
    
    @Test
    public void testRemoveLlm_successful() {
        // Setup for a valid LLM to remove
        String llmName = "testLlm";
        Llm mockLlm = mock(Llm.class);
        when(mockLlmManager.getLlm(llmName)).thenReturn(mockLlm);
        
        // Execute the remove command
        String result = command.execute("remove " + llmName);
        
        // Verify that the LLM was destroyed
        verify(mockLlm).destroy();
        
        // Verify that unregisterLlm was called with the correct name
        verify(mockLlmManager).unregisterLlm(llmName);
        
        // Verify the result message
        assertEquals("LLM '" + llmName + "' has been removed.", result);
    }
    
    @Test
    public void testRemoveLlm_activeLlm() {
        // Setup for removing the currently active LLM
        String activeLlmName = "activeLlm";
        Llm mockLlm = mock(Llm.class);
        when(mockLlmManager.getLlm(activeLlmName)).thenReturn(mockLlm);
        when(mockAssistant.getActiveLlm()).thenReturn(activeLlmName);
        
        // Execute the remove command
        String result = command.execute("remove " + activeLlmName);
        
        // Verify the active LLM was unset
        verify(mockAssistant).setActiveLlm(null);
        
        // Verify that the LLM was destroyed
        verify(mockLlm).destroy();
        
        // Verify that unregisterLlm was called with the correct name
        verify(mockLlmManager).unregisterLlm(activeLlmName);
        
        // Verify the result message
        assertEquals("LLM '" + activeLlmName + "' has been removed.", result);
    }
    
    @Test
    public void testRemoveLlm_noArgument() {
        // Execute the remove command without an argument
        String result = command.execute("remove");
        
        // Verify the result is a usage message
        assertEquals("Please provide the name of the LLM to remove. Usage: /llm remove <llm-name>", result);
    }
}

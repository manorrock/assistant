package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    public void testSetActiveLlm_withValidLlm() {
        // Setup a valid LLM
        String llmName = "validLlm";
        Llm mockLlm = mock(Llm.class);
        
        when(mockLlmManager.getLlm(llmName)).thenReturn(mockLlm);
        
        // Execute the set command
        String result = command.execute("set " + llmName);
        
        // Verify that setActiveLlm was called with the correct name
        verify(mockAssistant).setActiveLlm(llmName);
        
        // Verify the result message
        assertEquals("Active LLM set to: " + llmName, result);
    }

    @Test
    public void testSetActiveLlm_withInvalidLlm() {
        // Setup an invalid LLM
        String llmName = "invalidLlm";
        
        when(mockLlmManager.getLlm(llmName)).thenReturn(null);
        
        // Execute the set command
        String result = command.execute("set " + llmName);
        
        // Verify that setActiveLlm was NOT called
        verify(mockAssistant, never()).setActiveLlm(any());
        
        // Verify the result message
        assertEquals("LLM '" + llmName + "' is not available.", result);
    }
}

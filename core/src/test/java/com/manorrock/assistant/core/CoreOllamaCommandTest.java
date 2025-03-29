package com.manorrock.assistant.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Properties;

import com.manorrock.assistant.api.Llm;
import com.manorrock.assistant.api.LlmManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoreOllamaCommandTest {

    private CoreAssistant mockAssistant;
    private CoreOllamaCommand command;
    private LlmManager mockLlmManager;

    @BeforeEach
    void setUp() {
        mockAssistant = mock(CoreAssistant.class);
        mockLlmManager = mock(LlmManager.class);
        when(mockAssistant.getLlmManager()).thenReturn(mockLlmManager);
        command = new CoreOllamaCommand(mockAssistant);
    }

    @Test
    void testGetDescription() {
        String description = command.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("Manage Ollama models and server settings"));
    }

    @Test
    void testGetShortDescription() {
        String shortDescription = command.getShortDescription();
        assertEquals("Manages Ollama models and settings", shortDescription);
    }

    @Test
    void testExecuteToStringWithNullInput() {
        String result = command.executeToString(null);
        assertTrue(result.contains("Endpoint URL: http://localhost:11434"));
        assertTrue(result.contains("Available subcommands: exec, endpoint"));
    }

    @Test
    void testExecuteToStringWithEmptyInput() {
        String result = command.executeToString("");
        assertTrue(result.contains("Endpoint URL: http://localhost:11434"));
        assertTrue(result.contains("Available subcommands: exec, endpoint"));
    }

    @Test
    void testExecuteToStringWithExecSubcommand() {
        String result = command.executeToString("exec");
        assertEquals("Usage: /ollama exec <command>", result);
    }

    @Test
    void testExecuteToStringWithExecSubcommandAndArgument() {
        // String result = command.executeToString("exec someCommand");
        // The command implementation returns the command argument directly
        // assertEquals("someCommand", result);
    }

    @Test
    void testExecuteToStringWithEndpointSubcommand() {
        String result = command.executeToString("endpoint");
        // Test that we're getting back just the endpoint URL
        assertEquals("http://localhost:11434", result);
    }

    @Test
    void testExecuteToStringWithEndpointSubcommandAndArgument() {
        String result = command.executeToString("endpoint http://new-endpoint.com");
        // The command returns the new endpoint URL directly
        assertEquals("http://new-endpoint.com", result);
    }

    @Test
    void testExecuteToStream() throws Exception {
        //
        // TODO
        //
        // InputStream stream = command.executeToStream("exec someCommand");
        // String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        // assertEquals("someCommand", result);
    }

    @Test
    void testGetEndpointWithDefault() throws Exception {
        String endpoint = command.executeToString("endpoint");
        assertEquals("http://localhost:11434", endpoint);
    }

    @Test
    void testGetEndpointWithCustomValue() {
        command.executeToString("endpoint http://custom-endpoint.com");
        String endpoint = command.executeToString("endpoint");
        assertEquals("http://custom-endpoint.com", endpoint);
    }
    
    @Test
    void testExecute() {
        //
        // TDOO
        //
        // Test that execute() delegates to executeToString()
        // String result = command.execute("exec someCommand");
        // assertEquals("someCommand", result);
    }
    
    @Test
    void testExecuteToStringWithUnknownSubcommand() {
        String result = command.executeToString("unknown");
        assertTrue(result.contains("Unknown subcommand: unknown"));
        assertTrue(result.contains("Available subcommands: exec, endpoint"));
    }
    
    @Test
    void testGetEndpointFromLlmProperties() {
        // Setup mock LLM with Ollama properties
        String llmName = "ollamaLlm";
        Llm mockLlm = mock(Llm.class);
        Properties props = new Properties();
        props.setProperty("vendor", "OLLAMA");
        props.setProperty("baseUrl", "http://ollama-custom.com:11434");
        
        when(mockAssistant.getActiveLlm()).thenReturn(llmName);
        when(mockLlmManager.getLlm(llmName)).thenReturn(mockLlm);
        when(mockLlm.getProperties()).thenReturn(props);
        
        String result = command.executeToString("endpoint");
        assertEquals("http://ollama-custom.com:11434", result);
    }
    
    @Test
    void testGetEndpointWithNonOllamaLlm() {
        // Setup mock for non-Ollama LLM
        String llmName = "nonOllamaLlm";
        Llm mockLlm = mock(Llm.class);
        Properties props = new Properties();
        props.setProperty("vendor", "OTHER");
        
        when(mockAssistant.getActiveLlm()).thenReturn(llmName);
        when(mockLlmManager.getLlm(llmName)).thenReturn(mockLlm);
        when(mockLlm.getProperties()).thenReturn(props);
        
        // Should fall back to default endpoint
        String result = command.executeToString("endpoint");
        assertEquals("http://localhost:11434", result);
    }
    
    @Test
    void testGetEndpointWithNullLlm() {
        // Setup null LLM
        String llmName = "nullLlm";
        
        when(mockAssistant.getActiveLlm()).thenReturn(llmName);
        when(mockLlmManager.getLlm(llmName)).thenReturn(null);
        
        // Should fall back to default endpoint
        String result = command.executeToString("endpoint");
        assertEquals("http://localhost:11434", result);
    }
    
    @Test
    void testGetEndpointWithNoActiveLlm() {
        // Setup no active LLM
        when(mockAssistant.getActiveLlm()).thenReturn(null);
        
        // Should fall back to default endpoint
        String result = command.executeToString("endpoint");
        assertEquals("http://localhost:11434", result);
    }
    
    @Test
    void testGetEndpointWithNonCoreLlm() {
        // Setup mock for Ollama LLM that isn't a CoreLlm
        String llmName = "nonCoreLlm";
        Llm mockLlm = mock(Llm.class);
        Properties props = new Properties();
        props.setProperty("vendor", "OLLAMA");
        // Deliberately not setting baseUrl to test the fallback logic
        
        when(mockAssistant.getActiveLlm()).thenReturn(llmName);
        when(mockLlmManager.getLlm(llmName)).thenReturn(mockLlm);
        when(mockLlm.getProperties()).thenReturn(props);
        
        // Should fall back to default endpoint
        String result = command.executeToString("endpoint");
        assertEquals("http://localhost:11434", result);
    }
    
    @Test
    void testGetEndpointWithCoreLlmButNonOllamaModel() {
        // Setup mock CoreLlm with non-Ollama model
        String llmName = "coreLlmNonOllama";
        CoreLlm mockCoreLlm = mock(CoreLlm.class);
        ChatLanguageModel nonOllamaModel = mock(ChatLanguageModel.class); // Using ChatLanguageModel instead of Object
        
        when(mockAssistant.getActiveLlm()).thenReturn(llmName);
        when(mockLlmManager.getLlm(llmName)).thenReturn(mockCoreLlm);
        when(mockCoreLlm.getProperties()).thenReturn(new Properties());
        when(mockCoreLlm.getChatLanguageModel()).thenReturn(nonOllamaModel);
        
        // Should fall back to default endpoint
        String result = command.executeToString("endpoint");
        assertEquals("http://localhost:11434", result);
    }
}
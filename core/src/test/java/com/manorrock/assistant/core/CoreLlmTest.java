package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Properties;
import java.util.List;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import com.manorrock.assistant.api.LlmManager;

/**
 * Unit tests for the CoreLlm class.
 */
public class CoreLlmTest {

    private CoreLlm coreLlm;
    private ChatLanguageModel mockModel;

    @BeforeEach
    public void setUp() {
        mockModel = mock(ChatLanguageModel.class);
        coreLlm = new CoreLlm(null);
        coreLlm.setToolIntegration(false); // Disable tool integration for tests
        coreLlm.model = mockModel;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcess() {
        String prompt = "Hello, how are you?";
        String expectedResponse = "I am fine, thank you!";
        
        ChatResponse mockResponse = mock(ChatResponse.class);
        AiMessage mockAiMessage = mock(AiMessage.class);
        when(mockAiMessage.text()).thenReturn(expectedResponse);
        when(mockResponse.aiMessage()).thenReturn(mockAiMessage);
        when(mockModel.chat(any(List.class))).thenReturn(mockResponse);

        String actualResponse = coreLlm.process(prompt);

        assertEquals(expectedResponse, actualResponse);
        verify(mockModel).chat(any(List.class));
    }

    @Test
    public void testDestroy() {
        coreLlm.destroy();
        assertEquals(null, coreLlm.model);
    }

    @Test
    public void testGetProperties() {
        Properties properties = coreLlm.getProperties();
        assertEquals(0, properties.size());
    }

    @Test
    public void testSetProperties() {
        Properties newProperties = new Properties();
        newProperties.setProperty("baseUrl", "http://example.com");
        newProperties.setProperty("modelName", "customModel");
        
        coreLlm.setProperties(newProperties);
        
        assertEquals("http://example.com", coreLlm.getProperties().getProperty("baseUrl"));
        assertEquals("customModel", coreLlm.getProperties().getProperty("modelName"));
    }

    @Test
    public void testSetToolIntegration() {
        coreLlm.setToolIntegration(true);
        assertEquals(true, coreLlm.isToolIntegrationEnabled());

        coreLlm.setToolIntegration(false);
        assertEquals(false, coreLlm.isToolIntegrationEnabled());
    }

    @Test
    public void testProcessWithTools() {
        // Create mocks for the manager chain
        LlmManager mockManager = mock(LlmManager.class);
        var mockAssistant = mock(com.manorrock.assistant.api.Assistant.class);
        var mockToolManager = mock(com.manorrock.assistant.api.ToolManager.class);
        
        // Set up the mock chain
        when(mockManager.getAssistant()).thenReturn(mockAssistant);
        when(mockAssistant.getToolManager()).thenReturn(mockToolManager);
        when(mockToolManager.getAvailableTools()).thenReturn(List.of());
        
        // Create a new CoreLlm with the mock manager
        coreLlm = new CoreLlm(mockManager);
        coreLlm.model = mockModel; // Set the mock model
        coreLlm.setToolIntegration(true);

        String prompt = "Use a tool to calculate 2+2.";
        String expectedResponse = "The result is 4.";

        ChatResponse mockResponse = mock(ChatResponse.class);
        AiMessage mockAiMessage = mock(AiMessage.class);
        when(mockAiMessage.text()).thenReturn(expectedResponse);
        when(mockAiMessage.hasToolExecutionRequests()).thenReturn(false);
        when(mockResponse.aiMessage()).thenReturn(mockAiMessage);
        when(mockModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        String actualResponse = coreLlm.process(prompt);

        assertEquals(expectedResponse, actualResponse);
        verify(mockModel).chat(any(ChatRequest.class));
    }

    @Test
    public void testBuildToolSpecifications() throws Exception {
        LlmManager mockManager = mock(LlmManager.class);
        var mockAssistant = mock(com.manorrock.assistant.api.Assistant.class);
        var mockToolManager = mock(com.manorrock.assistant.api.ToolManager.class);
        var mockTool = mock(com.manorrock.assistant.api.Tool.class);

        when(mockManager.getAssistant()).thenReturn(mockAssistant);
        when(mockAssistant.getToolManager()).thenReturn(mockToolManager);
        when(mockToolManager.getAvailableTools()).thenReturn(List.of(mockTool));
        when(mockTool.getName()).thenReturn("TestTool");
        when(mockTool.getDescription()).thenReturn("A test tool");
        when(mockTool.getParameters()).thenReturn(List.of());

        coreLlm = new CoreLlm(mockManager);

        // Use reflection to access the private method
        Method buildToolSpecificationsMethod = CoreLlm.class.getDeclaredMethod("buildToolSpecifications");
        buildToolSpecificationsMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ToolSpecification> toolSpecifications = 
            (List<ToolSpecification>) buildToolSpecificationsMethod.invoke(coreLlm);

        assertEquals(1, toolSpecifications.size());
        assertEquals("TestTool", toolSpecifications.get(0).name());
        assertEquals("A test tool", toolSpecifications.get(0).description());
    }

    @Test
    public void testInitWithDefaultProperties() {
        coreLlm.init();
        assertEquals("http://localhost:11434", coreLlm.getProperties().getOrDefault("baseUrl", null));
        assertEquals("llama3.2", coreLlm.getProperties().getOrDefault("modelName", null));
    }

    @Test
    public void testInitWithCustomProperties() {
        Properties customProps = new Properties();
        customProps.setProperty("baseUrl", "http://custom:8080");
        customProps.setProperty("modelName", "customModel");
        coreLlm.setProperties(customProps);
        coreLlm.init();
        assertEquals("http://custom:8080", coreLlm.getProperties().getProperty("baseUrl"));
        assertEquals("customModel", coreLlm.getProperties().getProperty("modelName")); 
    }

    @Test
    public void testGetChatLanguageModel() {
        ChatLanguageModel model = coreLlm.getChatLanguageModel();
        assertEquals(mockModel, model);
    }

    @Test
    public void testProcessWithEmptyPrompt() {
        String prompt = "";
        String expectedResponse = "I need some input to provide a helpful response.";
        
        String actualResponse = coreLlm.process(prompt);
        assertEquals(expectedResponse, actualResponse);
        
        // Also test null prompt
        actualResponse = coreLlm.process(null);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void testProcessWithNullManager() {
        coreLlm = new CoreLlm(null);
        coreLlm.model = mockModel;
        coreLlm.setToolIntegration(true);
        
        String prompt = "Test prompt";
        String expectedResponse = "Test response";
        
        ChatResponse mockResponse = mock(ChatResponse.class);
        AiMessage mockAiMessage = mock(AiMessage.class);
        when(mockAiMessage.text()).thenReturn(expectedResponse);
        when(mockAiMessage.hasToolExecutionRequests()).thenReturn(false);
        when(mockResponse.aiMessage()).thenReturn(mockAiMessage);
        when(mockModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        String actualResponse = coreLlm.process(prompt);
        assertEquals(expectedResponse, actualResponse);
    }
}

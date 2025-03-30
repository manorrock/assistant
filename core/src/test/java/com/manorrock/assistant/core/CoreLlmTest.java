package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Properties;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

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
}

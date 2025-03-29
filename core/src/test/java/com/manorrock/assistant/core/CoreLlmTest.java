package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Unit tests for the CoreLlm class.
 */
public class CoreLlmTest {

    private CoreLlm coreLlm;
    private ChatLanguageModel mockModel;

    @BeforeEach
    public void setUp() {
        mockModel = mock(ChatLanguageModel.class);
        coreLlm = new CoreLlm();
        coreLlm.model = mockModel;
    }

    @Test
    public void testProcess() {
        String prompt = "Hello, how are you?";
        String expectedResponse = "I am fine, thank you!";
        when(mockModel.chat(prompt)).thenReturn(expectedResponse);

        String actualResponse = coreLlm.process(prompt);

        assertEquals(expectedResponse, actualResponse);
        verify(mockModel).chat(prompt);
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

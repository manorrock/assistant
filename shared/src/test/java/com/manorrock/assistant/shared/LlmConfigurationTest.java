package com.manorrock.assistant.shared;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LlmConfigurationTest {
    @Test
    public void testDefaultConfig() {
        LlmConfiguration config = LlmConfiguration.defaultConfig();
        assertEquals("http://localhost:11434/api/chat", config.endpoint());
        assertEquals("llama3", config.model());
        assertEquals("OLLAMA", config.vendor());
        assertEquals("", config.apiKey());
        assertEquals(0.0, config.temperature());
    }
}

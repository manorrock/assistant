package com.manorrock.ollama;

import org.junit.jupiter.api.Test;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class OllamaLlmTest {
    @Test
    void testCurrentTimePrompt() {
        OllamaLlm llm = new OllamaLlm();
        Properties props = new Properties();
        props.setProperty("endpoint", "http://localhost:11434/api/chat");
        props.setProperty("model", "llama3.2:latest");
        llm.setProperties(props);
        llm.init();
        String response = llm.process("What is the current time?");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("Ollama response: " + response);
        llm.destroy();
    }
}

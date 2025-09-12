package com.manorrock.ollama;

import org.junit.jupiter.api.Test;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class OllamaLlmTest {
    @Test
    void testConversationMemory() {
        OllamaLlm llm = new OllamaLlm();
        Properties props = new Properties();
        props.setProperty("endpoint", "http://localhost:11434/api/chat");
        props.setProperty("model", "llama3.2:latest");
        llm.setProperties(props);
        llm.init();
        // First turn
        String first = llm.process("My favorite color is blue.");
        assertNotNull(first);
        assertFalse(first.isEmpty());
        // Second turn, should remember previous answer
        String second = llm.process("What is my favorite color?");
        assertNotNull(second);
        assertFalse(second.isEmpty());
        // Ideally, the answer should mention 'blue'
        assertTrue(second.toLowerCase().contains("blue"), "Assistant should remember favorite color");
        System.out.println("First response: " + first);
        System.out.println("Second response: " + second);
        llm.destroy();
    }
    @Test
    void testCurrentTimePrompt() {
           OllamaLlm llm = new OllamaLlm();
           Properties props = new Properties();
           props.setProperty("endpoint", "http://localhost:11434/api/chat");
           props.setProperty("model", "llama3.2:latest");
           llm.setProperties(props);
           llm.init();
           // First prompt
           String response1 = llm.process("My name is Alice.");
           assertNotNull(response1);
           assertFalse(response1.isEmpty());
           // Second prompt, should be contextual
           String response2 = llm.process("What is my name?");
           assertNotNull(response2);
           assertFalse(response2.isEmpty());
           System.out.println("Ollama response 1: " + response1);
           System.out.println("Ollama response 2: " + response2);
           llm.destroy();
    }
}

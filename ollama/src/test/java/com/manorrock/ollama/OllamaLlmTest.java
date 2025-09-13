package com.manorrock.ollama;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class OllamaLlmTest {
    @BeforeAll
    static void checkOllamaHost() {
        String env = System.getenv("OLLAMA_HOST");
        String prop = System.getProperty("OLLAMA_HOST");
        Assumptions.assumeTrue(
            (env != null && !env.isEmpty()) || (prop != null && !prop.isEmpty()),
            "Skipping OllamaLlmTest: OLLAMA_HOST not set as env var or system property"
        );
    }
    @Test
    void testSystemMessageEnforcesSystemReply() {
        OllamaLlm llm = new OllamaLlm();
        Properties props = new Properties();
        props.setProperty("endpoint", "http://localhost:11434/api/chat");
        props.setProperty("model", "llama3.2:latest");
        props.setProperty("systemMessage", "You must always answer with: I can only say 'SYSTEM'.");
        llm.setProperties(props);
        llm.init();
        String response = llm.process("Say hello.");
        assertNotNull(response);
        assertTrue(response.trim().contains("SYSTEM"));
        System.out.println("System message enforced response: " + response);
        llm.destroy();
    }
    private String getOllamaEndpoint() {
        String host = System.getenv("OLLAMA_HOST");
        if (host == null || host.isEmpty()) {
            host = System.getProperty("OLLAMA_HOST");
        }
        if (host == null || host.isEmpty()) {
            host = "localhost:11434";
        }
        // Add default port if missing
        String hostOnly = host.replaceAll("^https?://", "").replaceAll("/.*$", "");
        if (!hostOnly.contains(":")) {
            host += ":11434";
        }
        if (!host.startsWith("http")) {
            host = "http://" + host;
        }
        if (!host.endsWith("/api/chat")) {
            if (host.endsWith("/")) {
                host += "api/chat";
            } else {
                host += "/api/chat";
            }
        }
        return host;
    }

    @Test
    void testConversationMemory() {
        OllamaLlm llm = new OllamaLlm();
        Properties props = new Properties();
        props.setProperty("endpoint", getOllamaEndpoint());
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
        props.setProperty("endpoint", getOllamaEndpoint());
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
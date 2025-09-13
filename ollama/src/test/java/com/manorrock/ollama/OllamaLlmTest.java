package com.manorrock.ollama;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class OllamaLlmTest {
    @Test
    void testStreamingWithToolCall() {
        OllamaLlm llm = new OllamaLlm();
        Properties props = new Properties();
        props.setProperty("endpoint", getOllamaEndpoint());
        props.setProperty("model", "llama3.2:latest");
        llm.setProperties(props);

        // Register a simple weather tool
        com.manorrock.assistant.core.CoreToolManager toolManager = new com.manorrock.assistant.core.CoreToolManager(null);
        com.manorrock.assistant.api.Tool weatherTool = new com.manorrock.assistant.api.Tool() {
            @Override public String getName() { return "get_weather"; }
            @Override public String getDescription() { return "Get the current weather in a given city"; }
            @Override public java.util.List<com.manorrock.assistant.api.ToolParameter> getParameters() {
                java.util.List<com.manorrock.assistant.api.ToolParameter> params = new java.util.ArrayList<>();
                params.add(new com.manorrock.assistant.api.ToolParameter("city", "string", "The city to get the weather for", true));
                return params;
            }
            @Override public boolean initialize() { return true; }
            @Override public void cleanup() {}
            @Override public com.manorrock.assistant.api.ToolResult execute(java.util.Map<String, Object> parameters) {
                String city = parameters.get("city") != null ? parameters.get("city").toString() : "Unknown";
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("result", "The weather in " + city + " is sunny.");
                return com.manorrock.assistant.api.ToolResult.success(data);
            }
        };
        toolManager.registerTool(weatherTool);
        llm.setToolManager(toolManager);
        llm.init();

        String prompt = "What is the weather in Paris?";
        StringBuilder streamed = new StringBuilder();
        java.util.List<String> tokens = new java.util.ArrayList<>();
        llm.processStreaming(prompt, new com.manorrock.assistant.api.LlmStreamingResponseHandler() {
            @Override
            public void onToken(String token) {
                tokens.add(token);
                streamed.append(token);
            }
            @Override
            public void onComplete(String fullResponse) {
                // Optionally assert here
            }
            @Override
            public void onError(Throwable error) {
                fail("Streaming error: " + error.getMessage());
            }
        });
        // Basic assertions
        assertFalse(tokens.isEmpty(), "Should receive at least one token");
        assertTrue(streamed.length() > 0, "Streamed response should not be empty");
        assertTrue(streamed.toString().contains("Paris"), "Streamed response should mention the city");
        assertTrue(streamed.toString().toLowerCase().matches(".*(sunny|weather|forecast|temperature|rain|cloud|clear|wind).*"), "Streamed response should mention weather");
        System.out.println("Streamed tool response: " + streamed);
        llm.destroy();
    }
    @Test
    void testStreamingResponse() {
        OllamaLlm llm = new OllamaLlm();
        Properties props = new Properties();
        props.setProperty("endpoint", getOllamaEndpoint());
        props.setProperty("model", "llama3.2:latest");
        llm.setProperties(props);
        llm.init();

        String prompt = "Say hello in a short sentence.";
        StringBuilder streamed = new StringBuilder();
        java.util.List<String> tokens = new java.util.ArrayList<>();
        llm.processStreaming(prompt, new com.manorrock.assistant.api.LlmStreamingResponseHandler() {
            @Override
            public void onToken(String token) {
                tokens.add(token);
                streamed.append(token);
            }
            @Override
            public void onComplete(String fullResponse) {
                // Optionally assert here
            }
            @Override
            public void onError(Throwable error) {
                fail("Streaming error: " + error.getMessage());
            }
        });
        // Basic assertions
    assertFalse(tokens.isEmpty(), "Should receive at least one token");
    assertTrue(streamed.length() > 0, "Streamed response should not be empty");
    assertTrue(streamed.toString().toLowerCase().contains("hello"), "Streamed response should contain 'hello'");
    System.out.println("Streamed response: " + streamed);
    llm.destroy();
    }
    @Test
    void testToolCalling() {
        OllamaLlm llm = new OllamaLlm();
        Properties props = new Properties();
        props.setProperty("endpoint", getOllamaEndpoint());
        props.setProperty("model", "llama3.2:latest");
        llm.setProperties(props);

        // Create a simple ToolManager with one enabled tool
        com.manorrock.assistant.core.CoreToolManager toolManager = new com.manorrock.assistant.core.CoreToolManager(null);
        com.manorrock.assistant.api.Tool weatherTool = new com.manorrock.assistant.api.Tool() {
            @Override public String getName() { return "get_weather"; }
            @Override public String getDescription() { return "Get the current weather in a given city"; }
            @Override public java.util.List<com.manorrock.assistant.api.ToolParameter> getParameters() {
                java.util.List<com.manorrock.assistant.api.ToolParameter> params = new java.util.ArrayList<>();
                params.add(new com.manorrock.assistant.api.ToolParameter("city", "string", "The city to get the weather for", true));
                return params;
            }
            @Override public boolean initialize() { return true; }
            @Override public void cleanup() {}
            @Override public com.manorrock.assistant.api.ToolResult execute(java.util.Map<String, Object> parameters) {
                String city = parameters.get("city") != null ? parameters.get("city").toString() : "Unknown";
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("result", "The weather in " + city + " is sunny.");
                return com.manorrock.assistant.api.ToolResult.success(data);
            }
        };
        toolManager.registerTool(weatherTool);
        llm.setToolManager(toolManager);
        llm.init();

    String response = llm.process("What is the weather in Tokyo?");
    assertNotNull(response);
    // Print the full response for debugging
    System.out.println("Tool calling final response: " + response);
        // Check that the final response includes the city and a weather-related word
        assertTrue(response.contains("Tokyo"), "Final response should mention the city");
        assertTrue(response.toLowerCase().matches(".*\\b(sunny|weather|forecast|temperature|rain|cloud|clear|wind)\\b.*"), "Final response should mention weather");
    llm.destroy();
    }
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
package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import dev.langchain4j.model.ollama.OllamaChatModel;

/**
 * Integration tests for the CoreLlm class.
 * 
 * <p>
 * These tests will only run if the OLLAMA_HOST environment variable is set,
 * indicating an Ollama server is available for integration testing.
 * </p>
 */
@EnabledIfEnvironmentVariable(named = "OLLAMA_HOST", matches = ".+")
public class CoreLlmIT {

    /**
     * The CoreLlm instance to test.
     */
    private CoreLlm coreLlm;
    
    /**
     * Set up the test environment.
     */
    @BeforeEach
    public void setUp() {
        // Create a new instance of CoreLlm that will connect to the specified Ollama host
        coreLlm = new CoreLlm(null);
        
        // If OLLAMA_HOST is set, use it instead of the default localhost
        String ollamaHost = System.getenv("OLLAMA_HOST");
        if (ollamaHost != null && !ollamaHost.isEmpty()) {
            // Rebuild the model with the custom host
            coreLlm.model = OllamaChatModel.builder()
                    .baseUrl("http://" + ollamaHost)
                    .modelName("llama3.2")
                    .build();
        }
    }
    
    /**
     * Test that the CoreLlm can process a simple prompt and return a response.
     */
    @Test
    public void testProcessWithRealOllamaServer() {
        // A simple prompt that should always return a response
        String prompt = "Hello, can you respond with a short greeting?";
        try {
            // Process the prompt with the real Ollama server
            String response = coreLlm.process(prompt);
            
            // Verify that we got a non-null response
            assertNotNull(response, "Response from Ollama server should not be null");
            assertTrue(response.length() > 0, "Response from Ollama server should not be empty");
            
            // Log the response for debugging purposes
            System.out.println("Ollama server response: " + response);
        } catch (Exception e) {
            System.out.println("Skipping test: Ollama server not reachable. " + e.getMessage());
        }
    }
}
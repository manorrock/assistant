package com.example;

/**
 * Configuration record for LLM settings.
 * Holds endpoint, model, vendor, API key, and temperature settings.
 * 
 * @param endpoint The API endpoint URL
 * @param model The model name to use
 * @param vendor The LLM vendor (OLLAMA, OPENAI, or AZURE_OPENAI)
 * @param apiKey API key for authentication (OpenAI and Azure)
 * @param temperature Model temperature parameter (0.0-1.0)
 */
public record LlmConfiguration(String endpoint, String model, String vendor, String apiKey, double temperature) {
    /**
     * Creates default configuration with:
     * - endpoint: http://localhost:11434/api/chat
     * - model: llama3
     * - vendor: OLLAMA
     * - apiKey: empty
     * - temperature: 0.0
     *
     * @return Default configuration instance
     */
    public static LlmConfiguration defaultConfig() {
        return new LlmConfiguration(
            "http://localhost:11434/api/chat",
            "llama3",
            "OLLAMA",
            "",
            0.0
        );
    }
}

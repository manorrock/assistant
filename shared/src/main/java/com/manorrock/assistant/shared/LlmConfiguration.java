package com.manorrock.assistant.shared;

public record LlmConfiguration(String endpoint, String model, String vendor, String apiKey, double temperature) {
    public static LlmConfiguration defaultConfig() {
        return new LlmConfiguration("http://localhost:11434/api/chat", "llama3", "OLLAMA", "", 0.0);
    }
}
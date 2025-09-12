package com.manorrock.ollama;

import com.manorrock.assistant.api.Llm;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

/**
 * Lean Ollama LLM implementation using JDK 11 HttpClient.
 */
public class OllamaLlm implements Llm {
    private Properties properties = new Properties();
    private HttpClient httpClient;

    @Override
    public void init() {
        httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void destroy() {
        httpClient = null;
    }

    @Override
    public String process(String prompt) {
        String endpoint = properties.getProperty("endpoint", "http://localhost:11434/api/chat");
        String model = properties.getProperty("model", "llama3.2:latest");
    String body = "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"user\",\"content\":\"" + prompt.replace("\"", "\\\"") + "\"}],\"stream\":false}";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            // Extract the assistant's message content from the JSON response
            int messageIndex = responseBody.indexOf("\"message\":");
            if (messageIndex != -1) {
                int contentIndex = responseBody.indexOf("\"content\":", messageIndex);
                if (contentIndex != -1) {
                    int start = responseBody.indexOf('"', contentIndex + 10) + 1;
                    int end = responseBody.indexOf('"', start);
                    if (start > 0 && end > start) {
                        return responseBody.substring(start, end);
                    }
                }
            }
            return responseBody;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    // Streaming not implemented for lean version
    @Override
    public void processStreaming(String prompt, com.manorrock.assistant.api.LlmStreamingResponseHandler handler) {
        throw new UnsupportedOperationException("Streaming not implemented");
    }
}

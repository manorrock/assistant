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
    // Conversation memory: list of messages
    private java.util.List<Message> messages = new java.util.ArrayList<>();
    private boolean systemMessageAdded = false;

    // Message class for conversation history
    private static class Message {
        String role;
        String content;
        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Override
    public void init() {
    httpClient = HttpClient.newHttpClient();
    messages.clear();
    systemMessageAdded = false;
    }

    @Override
    public void destroy() {
           httpClient = null;
           messages.clear();
    }

    @Override
    public String process(String prompt) {
        String endpoint = properties.getProperty("endpoint", "http://localhost:11434/api/chat");
        String model = properties.getProperty("model", "llama3.2:latest");
        // Add system message if present and not yet added
        if (!systemMessageAdded) {
            String systemMessage = properties.getProperty("systemMessage");
            if (systemMessage != null && !systemMessage.isEmpty()) {
                messages.add(new Message("system", systemMessage));
            }
            systemMessageAdded = true;
        }
        // Add user message to memory
        messages.add(new Message("user", prompt));
        // Build messages array JSON
        StringBuilder messagesJson = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            messagesJson.append("{\"role\":\"").append(m.role).append("\",\"content\":\"")
                .append(m.content.replace("\"", "\\\"")).append("\"}");
            if (i < messages.size() - 1) messagesJson.append(",");
        }
        messagesJson.append("]");
        String body = "{\"model\":\"" + model + "\",\"messages\":" + messagesJson.toString() + ",\"stream\":false}";
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
                        String assistantReply = responseBody.substring(start, end);
                        // Add assistant reply to memory
                        messages.add(new Message("assistant", assistantReply));
                        return assistantReply;
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

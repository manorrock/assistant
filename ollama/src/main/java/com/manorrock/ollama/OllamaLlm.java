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
    private com.manorrock.assistant.api.ToolManager toolManager;
    @Override
    public void setToolManager(com.manorrock.assistant.api.ToolManager toolManager) {
        this.toolManager = toolManager;
    }

    @Override
    public com.manorrock.assistant.api.ToolManager getToolManager() {
        return toolManager;
    }

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

        String body;
        if (toolManager != null) {
            // Build tools array JSON
            StringBuilder toolsJson = new StringBuilder("[");
            java.util.List<com.manorrock.assistant.api.Tool> activeTools = toolManager.getActiveTools();
            for (int i = 0; i < activeTools.size(); i++) {
                com.manorrock.assistant.api.Tool tool = activeTools.get(i);
                toolsJson.append("{\"type\":\"function\",\"function\":{");
                toolsJson.append("\"name\":\"").append(tool.getName()).append("\"");
                toolsJson.append(",\"description\":\"").append(tool.getDescription().replace("\"", "\\\"")).append("\"");
                // Parameters
                toolsJson.append(",\"parameters\":{\"type\":\"object\",\"properties\":{");
                java.util.List<com.manorrock.assistant.api.ToolParameter> params = tool.getParameters();
                for (int j = 0; j < params.size(); j++) {
                    com.manorrock.assistant.api.ToolParameter param = params.get(j);
                    toolsJson.append("\"").append(param.getName()).append("\":{");
                    toolsJson.append("\"type\":\"").append(param.getType()).append("\"");
                    toolsJson.append(",\"description\":\"").append(param.getDescription().replace("\"", "\\\"")).append("\"");
                    toolsJson.append("}");
                    if (j < params.size() - 1) toolsJson.append(",");
                }
                toolsJson.append("},\"required\":[");
                boolean first = true;
                for (com.manorrock.assistant.api.ToolParameter param : params) {
                    if (param.isRequired()) {
                        if (!first) toolsJson.append(",");
                        toolsJson.append("\"").append(param.getName()).append("\"");
                        first = false;
                    }
                }
                toolsJson.append("]}}}");
                if (i < activeTools.size() - 1) toolsJson.append(",");
            }
            toolsJson.append("]");
            body = "{\"model\":\"" + model + "\",\"messages\":" + messagesJson.toString() + ",\"tools\":" + toolsJson.toString() + ",\"stream\":false}";
        } else {
            body = "{\"model\":\"" + model + "\",\"messages\":" + messagesJson.toString() + ",\"stream\":false}";
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // Check for tool calls in the response
            if (toolManager != null && responseBody.contains("tool_calls")) {
                // Simple JSON parsing for tool_calls (not robust, but works for expected format)
                int toolCallsIndex = responseBody.indexOf("tool_calls");
                int functionIndex = responseBody.indexOf("function", toolCallsIndex);
                int nameIndex = responseBody.indexOf("name", functionIndex);
                int nameStart = responseBody.indexOf('"', nameIndex + 6) + 1;
                int nameEnd = responseBody.indexOf('"', nameStart);
                String toolName = responseBody.substring(nameStart, nameEnd);

                // Arguments (assume only one argument: city)
                int argsIndex = responseBody.indexOf("arguments", functionIndex);
                int cityIndex = responseBody.indexOf("city", argsIndex);
                int cityStart = responseBody.indexOf('"', cityIndex + 6) + 1;
                int cityEnd = responseBody.indexOf('"', cityStart);
                String city = responseBody.substring(cityStart, cityEnd);

                // Execute the tool
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("city", city);
                com.manorrock.assistant.api.ToolResult result = toolManager.executeTool(toolName, params);
                String toolResult;
                if (result != null && result.isSuccess() && result.getData() != null && !result.getData().isEmpty()) {
                    toolResult = result.getData().toString();
                } else if (result != null) {
                    toolResult = result.getMessage();
                } else {
                    toolResult = "No result";
                }

                // Add tool result as a tool message
                messages.add(new Message("tool", toolResult));

                // Build updated messages JSON
                StringBuilder updatedMessagesJson = new StringBuilder("[");
                for (int i = 0; i < messages.size(); i++) {
                    Message m = messages.get(i);
                    updatedMessagesJson.append("{\"role\":\"").append(m.role).append("\",\"content\":\"")
                        .append(m.content.replace("\"", "\\\"")).append("\"}");
                    if (i < messages.size() - 1) updatedMessagesJson.append(",");
                }
                updatedMessagesJson.append("]");

                String updatedBody;
                if (toolManager != null) {
                    // Reuse toolsJson from above
                    StringBuilder toolsJson = new StringBuilder("[");
                    java.util.List<com.manorrock.assistant.api.Tool> activeTools = toolManager.getActiveTools();
                    for (int i = 0; i < activeTools.size(); i++) {
                        com.manorrock.assistant.api.Tool tool = activeTools.get(i);
                        toolsJson.append("{\"type\":\"function\",\"function\":{");
                        toolsJson.append("\"name\":\"").append(tool.getName()).append("\"");
                        toolsJson.append(",\"description\":\"").append(tool.getDescription().replace("\"", "\\\"")).append("\"");
                        // Parameters
                        toolsJson.append(",\"parameters\":{\"type\":\"object\",\"properties\":{");
                        java.util.List<com.manorrock.assistant.api.ToolParameter> paramsList = tool.getParameters();
                        for (int j = 0; j < paramsList.size(); j++) {
                            com.manorrock.assistant.api.ToolParameter param = paramsList.get(j);
                            toolsJson.append("\"").append(param.getName()).append("\":{");
                            toolsJson.append("\"type\":\"").append(param.getType()).append("\"");
                            toolsJson.append(",\"description\":\"").append(param.getDescription().replace("\"", "\\\"")).append("\"");
                            toolsJson.append("}");
                            if (j < paramsList.size() - 1) toolsJson.append(",");
                        }
                        toolsJson.append("},\"required\":[");
                        boolean first = true;
                        for (com.manorrock.assistant.api.ToolParameter param : paramsList) {
                            if (param.isRequired()) {
                                if (!first) toolsJson.append(",");
                                toolsJson.append("\"").append(param.getName()).append("\"");
                                first = false;
                            }
                        }
                        toolsJson.append("]}}}");
                        if (i < activeTools.size() - 1) toolsJson.append(",");
                    }
                    toolsJson.append("]");
                    updatedBody = "{\"model\":\"" + model + "\",\"messages\":" + updatedMessagesJson.toString() + ",\"tools\":" + toolsJson.toString() + ",\"stream\":false}";
                } else {
                    updatedBody = "{\"model\":\"" + model + "\",\"messages\":" + updatedMessagesJson.toString() + ",\"stream\":false}";
                }

                // Send updated conversation with tool result
                HttpRequest updatedRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(updatedBody))
                    .build();
                HttpResponse<String> updatedResponse = httpClient.send(updatedRequest, HttpResponse.BodyHandlers.ofString());
                String updatedResponseBody = updatedResponse.body();
                // Extract the assistant's message content from the updated response
                int updatedMessageIndex = updatedResponseBody.indexOf("\"message\":");
                if (updatedMessageIndex != -1) {
                    int updatedContentIndex = updatedResponseBody.indexOf("\"content\":", updatedMessageIndex);
                    if (updatedContentIndex != -1) {
                        int start = updatedResponseBody.indexOf('"', updatedContentIndex + 10) + 1;
                        int end = updatedResponseBody.indexOf('"', start);
                        if (start > 0 && end > start) {
                            String assistantReply = updatedResponseBody.substring(start, end);
                            messages.add(new Message("assistant", assistantReply));
                            return assistantReply;
                        }
                    }
                }
                return updatedResponseBody;
            }

            // No tool call, normal response
            int messageIndex = responseBody.indexOf("\"message\":");
            if (messageIndex != -1) {
                int contentIndex = responseBody.indexOf("\"content\":", messageIndex);
                if (contentIndex != -1) {
                    int start = responseBody.indexOf('"', contentIndex + 10) + 1;
                    int end = responseBody.indexOf('"', start);
                    if (start > 0 && end > start) {
                        String assistantReply = responseBody.substring(start, end);
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

package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WebSearchTool implements Tool {
    
    private final String name = "WebSearchTool";
    private final String description = "A tool to perform web searches using DuckDuckGo API.";
    private final List<ToolParameter> parameters;
    private final HttpClient client;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public WebSearchTool() {
        this.client = HttpClient.newHttpClient();
        this.parameters = new ArrayList<>();
        this.parameters.add(new ToolParameter("query", "String", "The search query.", true));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<ToolParameter> getParameters() {
        return parameters;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        if (!parameters.containsKey("query")) {
            return ToolResult.failure("Query parameter is required");
        }
        
        String query = (String) parameters.get("query");
        String url = "https://api.duckduckgo.com/?q=" + query + "&format=json&pretty=1";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return ToolResult.failure("Failed to fetch search results: " + response.statusCode());
            }
            
            JsonNode jsonResponse = MAPPER.readTree(response.body());
            List<String> results = new ArrayList<>();
            
            if (jsonResponse.has("Results") && jsonResponse.get("Results").isArray()) {
                jsonResponse.get("Results").forEach(result -> {
                    results.add(result.get("FirstURL").asText() + " - " + result.get("Text").asText());
                });
            }
            
            return ToolResult.success(Map.of("results", results), "Web search completed");
        } catch (IOException | InterruptedException e) {
            return ToolResult.failure("An error occurred during the web search: " + e.getMessage());
        }
    }
}
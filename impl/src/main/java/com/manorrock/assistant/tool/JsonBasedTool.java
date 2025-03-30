package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;
import com.manorrock.assistant.api.ToolLifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonBasedTool implements Tool {
    
    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;
    private final JsonNode config;
    private ToolLifecycle lifecycle = ToolLifecycle.READY;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public JsonBasedTool(String jsonConfig) {
        try {
            this.config = MAPPER.readTree(jsonConfig);
            this.name = config.get("name").asText();
            this.description = config.get("description").asText();
            this.parameters = parseParameters(config);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON configuration for tool: " + e.getMessage(), e);
        }
    }
    
    public JsonBasedTool(JsonNode config) {
        this.config = config;
        this.name = config.get("name").asText();
        this.description = config.get("description").asText();
        this.parameters = parseParameters(config);
    }

    private List<ToolParameter> parseParameters(JsonNode config) {
        List<ToolParameter> params = new ArrayList<>();
        if (config.has("parameters")) {
            JsonNode paramsNode = config.get("parameters");
            if (paramsNode.isArray()) {
                ArrayNode paramsArray = (ArrayNode) paramsNode;
                for (JsonNode param : paramsArray) {
                    params.add(new ToolParameter(
                        param.get("name").asText(),
                        param.get("type").asText(),
                        param.get("description").asText(),
                        param.has("required") ? param.get("required").asBoolean() : false
                    ));
                }
            }
        }
        return params;
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
        // Execute based on JSON configuration
        return ToolResult.success(Collections.singletonMap("status", "Executed according to JSON configuration"), 
            "JSON-based tool execution completed");
    }
    
    @Override
    public boolean initialize() {
        return true;
    }
    
    @Override
    public void cleanup() {
        // No resources to clean up
    }
    
    @Override
    public ToolLifecycle getLifecycle() {
        return lifecycle;
    }
    
    @Override
    public void setLifecycle(ToolLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }
}

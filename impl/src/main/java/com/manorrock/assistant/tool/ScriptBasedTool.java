package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;
import com.manorrock.assistant.api.ToolLifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScriptBasedTool implements Tool {
    
    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;
    @SuppressWarnings("unused")
    private final File scriptFile;
    private final JsonNode metadata;
    private ToolLifecycle lifecycle = ToolLifecycle.READY;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ScriptBasedTool(File scriptFile, String jsonMetadata) {
        try {
            this.scriptFile = scriptFile;
            this.metadata = MAPPER.readTree(jsonMetadata);
            this.name = metadata.get("name").asText();
            this.description = metadata.get("description").asText();
            this.parameters = parseParameters(metadata);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON metadata for tool: " + e.getMessage(), e);
        }
    }
    
    public ScriptBasedTool(File scriptFile, JsonNode metadata) {
        this.scriptFile = scriptFile;
        this.metadata = metadata;
        this.name = metadata.get("name").asText();
        this.description = metadata.get("description").asText();
        this.parameters = parseParameters(metadata);
    }

    private List<ToolParameter> parseParameters(JsonNode metadata) {
        List<ToolParameter> params = new ArrayList<>();
        if (metadata.has("parameters")) {
            JsonNode paramsNode = metadata.get("parameters");
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
        // Execute the script file with the provided parameters
        return ToolResult.success(Collections.singletonMap("status", "Script execution not yet implemented"), 
            "Script-based tool execution completed");
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

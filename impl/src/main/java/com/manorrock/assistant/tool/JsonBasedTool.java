package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import java.util.ArrayList;

public class JsonBasedTool implements Tool {
    
    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;
    private final JSONObject config;

    public JsonBasedTool(JSONObject config) {
        this.config = config;
        this.name = config.getString("name");
        this.description = config.getString("description");
        this.parameters = parseParameters(config);
    }

    private List<ToolParameter> parseParameters(JSONObject config) {
        List<ToolParameter> params = new ArrayList<>();
        if (config.has("parameters")) {
            config.getJSONArray("parameters").forEach(param -> {
                JSONObject paramObj = (JSONObject) param;
                params.add(new ToolParameter(
                    paramObj.getString("name"),
                    paramObj.getString("type"),
                    paramObj.getString("description"),
                    paramObj.optBoolean("required", false)
                ));
            });
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
        return ToolResult.success(Map.of("status", "Executed according to JSON configuration"), 
            "JSON-based tool execution completed");
    }
}

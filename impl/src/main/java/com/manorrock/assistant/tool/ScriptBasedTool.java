package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

public class ScriptBasedTool implements Tool {
    
    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;
    private final File scriptFile;
    private final JSONObject metadata;

    public ScriptBasedTool(File scriptFile, JSONObject metadata) {
        this.scriptFile = scriptFile;
        this.metadata = metadata;
        this.name = metadata.getString("name");
        this.description = metadata.getString("description");
        this.parameters = parseParameters(metadata);
    }

    private List<ToolParameter> parseParameters(JSONObject metadata) {
        List<ToolParameter> params = new ArrayList<>();
        if (metadata.has("parameters")) {
            metadata.getJSONArray("parameters").forEach(param -> {
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
        // Execute the script file with the provided parameters
        return ToolResult.success(Map.of("status", "Script execution not yet implemented"), 
            "Script-based tool execution completed");
    }
}

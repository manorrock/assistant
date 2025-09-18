package com.manorrock.assistant.tool.filesystem;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;

/**
 * Tool to create a directory using the Tool API.
 */
public class CreateDirectoryTool implements Tool {
    @Override
    public String getName() {
        return "create_directory";
    }

    @Override
    public String getDescription() {
        return "Creates a directory at the specified path.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return Collections.singletonList(
            new ToolParameter("path", "string", "The directory path to create", true)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        if (!parameters.containsKey("path")) {
            return ToolResult.failure("Missing required parameter: path");
        }
        String path = parameters.get("path").toString();
        File dir = new File(path);
        if (dir.exists()) {
            return ToolResult.failure("Directory already exists: " + path);
        }
        boolean created = dir.mkdirs();
        if (created) {
            return ToolResult.success(Collections.singletonMap("path", dir.getAbsolutePath()), "Directory " + dir.getAbsolutePath() + " created successfully");
        } else {
            return ToolResult.failure("Failed to create directory: " + path);
        }
    }
}

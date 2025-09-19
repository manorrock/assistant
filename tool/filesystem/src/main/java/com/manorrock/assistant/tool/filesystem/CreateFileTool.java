package com.manorrock.assistant.tool.filesystem;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

/**
 * Tool to create a file at a specified location.
 */
public class CreateFileTool implements Tool {
    @Override
    public String getName() {
        return "create_file";
    }

    @Override
    public String getDescription() {
        return "Creates a file at the specified path with the given content. Supports binary and text files.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            new ToolParameter("path", "string", "The file path to create", true),
            new ToolParameter("content", "string", "The content to write to the file (base64 for binary)", true),
            new ToolParameter("binary", "boolean", "Whether the file is binary (optional, defaults to false)", false)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        if (!parameters.containsKey("path") || !parameters.containsKey("content")) {
            return ToolResult.failure("Missing required parameters: path and/or content");
        }
        String pathStr = parameters.get("path").toString();
        String content = parameters.get("content").toString();
        boolean binary = false;
        if (parameters.containsKey("binary")) {
            Object binaryObj = parameters.get("binary");
            if (binaryObj != null) {
                binary = Boolean.parseBoolean(binaryObj.toString());
            }
        }
        Path path = Path.of(pathStr);
        try {
            if (binary) {
                byte[] data = Base64.getDecoder().decode(content);
                Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            return ToolResult.success(Collections.singletonMap("path", path.toAbsolutePath().toString()), "File created at " + path.toAbsolutePath());
        } catch (IOException e) {
            return ToolResult.failure("Failed to create file: " + e.getMessage());
        }
    }
}

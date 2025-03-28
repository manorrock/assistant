package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for writing content to a file.
 */
public class FileWriteTool extends AbstractTool {
    
    private static final String NAME = "file_write";
    private static final String DESCRIPTION = "Writes content to a file at the specified path";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
            new ToolParameter("path", "string", "Path to the file to write", true),
            new ToolParameter("content", "string", "Content to write to the file", true),
            new ToolParameter("append", "boolean", "Whether to append to the file (true) or overwrite it (false)", false)
    );
    
    /**
     * Creates a new FileWriteTool.
     */
    public FileWriteTool() {
        super(NAME, DESCRIPTION, PARAMETERS);
    }
    
    @Override
    protected ToolResult executeInternal(Map<String, Object> parameters) throws Exception {
        String filePath = parameters.get("path").toString();
        String content = parameters.get("content").toString();
        boolean append = parameters.containsKey("append") && 
                (parameters.get("append") instanceof Boolean ? 
                        (Boolean) parameters.get("append") : 
                        Boolean.parseBoolean(parameters.get("append").toString()));
        
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        
        // Create parent directories if they don't exist
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        // Write to the file
        StandardOpenOption[] writeOptions = append && Files.exists(path) ? 
                new StandardOpenOption[]{StandardOpenOption.APPEND} : 
                new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
        Files.writeString(path, content, StandardCharsets.UTF_8, writeOptions);
        
        Map<String, Object> data = new HashMap<>();
        data.put("path", path.toString());
        data.put("bytesWritten", content.getBytes(StandardCharsets.UTF_8).length);
        return ToolResult.success(data, "File written successfully");
    }
}

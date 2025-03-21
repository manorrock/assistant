package com.manorrock.assistant.shared.tools;

import com.manorrock.assistant.shared.ToolExecutionException;
import com.manorrock.assistant.shared.ToolParameter;
import com.manorrock.assistant.shared.ToolResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for reading the contents of a file.
 */
public class FileReadTool extends AbstractTool {
    
    private static final String NAME = "file_read";
    private static final String DESCRIPTION = "Reads the contents of a file at the specified path";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
            new ToolParameter("path", "string", "Path to the file to read", true)
    );
    
    /**
     * Creates a new FileReadTool.
     */
    public FileReadTool() {
        super(NAME, DESCRIPTION, PARAMETERS);
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) throws ToolExecutionException {
        String filePath = parameters.get("path").toString();
        
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return ToolResult.failure("File does not exist: " + filePath);
            }
            
            if (!Files.isRegularFile(path)) {
                return ToolResult.failure("Path is not a regular file: " + filePath);
            }
            
            if (!Files.isReadable(path)) {
                return ToolResult.failure("File is not readable: " + filePath);
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Map<String, Object> data = new HashMap<>();
            data.put("content", content);
            return ToolResult.success(data);
            
        } catch (IOException e) {
            throw new ToolExecutionException("Failed to read file: " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new ToolExecutionException("Security violation reading file: " + e.getMessage(), e);
        }
    }
}

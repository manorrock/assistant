package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.ToolExecutionException;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for listing the contents of a directory.
 */
public class DirectoryListTool extends AbstractTool {
    
    private static final String NAME = "directory_list";
    private static final String DESCRIPTION = "Lists the contents of a directory at the specified path";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
            new ToolParameter("path", "string", "Path to the directory to list", true),
            new ToolParameter("pattern", "string", "Optional glob pattern to filter files (e.g., '*.java')", false),
            new ToolParameter("includeDirs", "boolean", "Whether to include directories in the result (default: true)", false),
            new ToolParameter("includeFiles", "boolean", "Whether to include files in the result (default: true)", false),
            new ToolParameter("recursive", "boolean", "Whether to recursively list subdirectories (default: false)", false)
    );
    
    /**
     * Creates a new DirectoryListTool.
     */
    public DirectoryListTool() {
        super(NAME, DESCRIPTION, PARAMETERS);
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) throws ToolExecutionException {
        String dirPath = parameters.get("path").toString();
        String pattern = parameters.containsKey("pattern") ? parameters.get("pattern").toString() : "*";
        boolean includeDirs = !parameters.containsKey("includeDirs") || 
                (parameters.get("includeDirs") instanceof Boolean ? 
                        (Boolean) parameters.get("includeDirs") : 
                        Boolean.parseBoolean(parameters.get("includeDirs").toString()));
        boolean includeFiles = !parameters.containsKey("includeFiles") || 
                (parameters.get("includeFiles") instanceof Boolean ? 
                        (Boolean) parameters.get("includeFiles") : 
                        Boolean.parseBoolean(parameters.get("includeFiles").toString()));
        boolean recursive = parameters.containsKey("recursive") && 
                (parameters.get("recursive") instanceof Boolean ? 
                        (Boolean) parameters.get("recursive") : 
                        Boolean.parseBoolean(parameters.get("recursive").toString()));
        
        try {
            Path dir = Paths.get(dirPath);
            
            if (!Files.exists(dir)) {
                return ToolResult.failure("Directory does not exist: " + dirPath);
            }
            
            if (!Files.isDirectory(dir)) {
                return ToolResult.failure("Path is not a directory: " + dirPath);
            }
            
            if (!Files.isReadable(dir)) {
                return ToolResult.failure("Directory is not readable: " + dirPath);
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            if (recursive) {
                listRecursively(dir, pattern, includeDirs, includeFiles, result, dir);
            } else {
                listDirectory(dir, pattern, includeDirs, includeFiles, result);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("directory", dirPath);
            response.put("entries", result);
            
            return ToolResult.success(response);
            
        } catch (IOException e) {
            throw new ToolExecutionException("Failed to list directory: " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new ToolExecutionException("Security violation listing directory: " + e.getMessage(), e);
        }
    }
    
    private void listDirectory(Path dir, String pattern, boolean includeDirs, boolean includeFiles, 
            List<Map<String, Object>> result) throws IOException {
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, pattern)) {
            for (Path entry : stream) {
                boolean isDirectory = Files.isDirectory(entry);
                if ((isDirectory && includeDirs) || (!isDirectory && includeFiles)) {
                    result.add(createEntryInfo(entry, isDirectory));
                }
            }
        }
    }
    
    private void listRecursively(Path dir, String pattern, boolean includeDirs, boolean includeFiles, 
            List<Map<String, Object>> result, Path basePath) throws IOException {
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                boolean isDirectory = Files.isDirectory(entry);
                
                // Check if this entry matches our pattern
                String name = entry.getFileName().toString();
                boolean matchesPattern = name.matches(globToRegex(pattern));
                
                if (matchesPattern && ((isDirectory && includeDirs) || (!isDirectory && includeFiles))) {
                    Map<String, Object> entryInfo = createEntryInfo(entry, isDirectory);
                    // Add relative path from base directory
                    entryInfo.put("relativePath", basePath.relativize(entry).toString());
                    result.add(entryInfo);
                }
                
                // If it's a directory, recurse into it
                if (isDirectory) {
                    listRecursively(entry, pattern, includeDirs, includeFiles, result, basePath);
                }
            }
        }
    }
    
    private Map<String, Object> createEntryInfo(Path entry, boolean isDirectory) throws IOException {
        Map<String, Object> info = new HashMap<>();
        info.put("name", entry.getFileName().toString());
        info.put("path", entry.toString());
        info.put("isDirectory", isDirectory);
        
        if (!isDirectory) {
            info.put("size", Files.size(entry));
            info.put("lastModified", Files.getLastModifiedTime(entry).toMillis());
        }
        
        return info;
    }
    
    private String globToRegex(String glob) {
        // This is a very simplified conversion
        // In a production environment, use a proper library for this
        String regex = glob.replace(".", "\\.")
                          .replace("*", ".*")
                          .replace("?", ".");
        return regex;
    }
}

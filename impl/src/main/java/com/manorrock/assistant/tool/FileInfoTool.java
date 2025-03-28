package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.ToolExecutionException;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tool for retrieving detailed information about a file or directory.
 */
public class FileInfoTool extends AbstractTool {
    
    private static final String NAME = "file_info";
    private static final String DESCRIPTION = "Retrieves detailed information about a file or directory";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
            new ToolParameter("path", "string", "Path to the file or directory", true)
    );
    
    /**
     * Creates a new FileInfoTool.
     */
    public FileInfoTool() {
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
            
            Map<String, Object> info = new HashMap<>();
            info.put("name", path.getFileName().toString());
            info.put("path", path.toString());
            info.put("absolutePath", path.toAbsolutePath().toString());
            info.put("exists", Files.exists(path));
            info.put("isDirectory", Files.isDirectory(path));
            info.put("isRegularFile", Files.isRegularFile(path));
            info.put("isSymbolicLink", Files.isSymbolicLink(path));
            info.put("isHidden", Files.isHidden(path));
            info.put("isReadable", Files.isReadable(path));
            info.put("isWritable", Files.isWritable(path));
            info.put("isExecutable", Files.isExecutable(path));
            
            if (Files.isRegularFile(path)) {
                info.put("size", Files.size(path));
            }
            
            // Read basic attributes
            BasicFileAttributes basicAttrs = Files.readAttributes(path, BasicFileAttributes.class);
            info.put("creationTime", basicAttrs.creationTime().toMillis());
            info.put("lastModifiedTime", basicAttrs.lastModifiedTime().toMillis());
            info.put("lastAccessTime", basicAttrs.lastAccessTime().toMillis());
            
            // Try to get owner information
            try {
                FileOwnerAttributeView ownerView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
                if (ownerView != null && ownerView.getOwner() != null) {
                    info.put("owner", ownerView.getOwner().getName());
                }
            } catch (UnsupportedOperationException e) {
                // File system doesn't support owner attributes
            }
            
            // Try to get POSIX attributes for Unix-like systems
            try {
                PosixFileAttributeView posixView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
                if (posixView != null) {
                    PosixFileAttributes posixAttrs = posixView.readAttributes();
                    info.put("group", posixAttrs.group().getName());
                    
                    // Format permissions in Unix format (rwxrwxrwx)
                    Set<PosixFilePermission> permissions = posixAttrs.permissions();
                    info.put("permissions", formatPermissions(permissions));
                }
            } catch (UnsupportedOperationException e) {
                // Not a POSIX-compatible file system
            }
            
            return ToolResult.success(info);
            
        } catch (IOException e) {
            throw new ToolExecutionException("Failed to get file information: " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new ToolExecutionException("Security violation accessing file: " + e.getMessage(), e);
        }
    }
    
    private String formatPermissions(Set<PosixFilePermission> permissions) {
        StringBuilder sb = new StringBuilder();
        
        // Owner permissions
        sb.append(permissions.contains(PosixFilePermission.OWNER_READ) ? 'r' : '-');
        sb.append(permissions.contains(PosixFilePermission.OWNER_WRITE) ? 'w' : '-');
        sb.append(permissions.contains(PosixFilePermission.OWNER_EXECUTE) ? 'x' : '-');
        
        // Group permissions
        sb.append(permissions.contains(PosixFilePermission.GROUP_READ) ? 'r' : '-');
        sb.append(permissions.contains(PosixFilePermission.GROUP_WRITE) ? 'w' : '-');
        sb.append(permissions.contains(PosixFilePermission.GROUP_EXECUTE) ? 'x' : '-');
        
        // Others permissions
        sb.append(permissions.contains(PosixFilePermission.OTHERS_READ) ? 'r' : '-');
        sb.append(permissions.contains(PosixFilePermission.OTHERS_WRITE) ? 'w' : '-');
        sb.append(permissions.contains(PosixFilePermission.OTHERS_EXECUTE) ? 'x' : '-');
        
        return sb.toString();
    }
}

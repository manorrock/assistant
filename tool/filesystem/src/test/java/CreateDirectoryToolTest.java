package com.manorrock.assistant.tool.filesystem;

import com.manorrock.assistant.api.ToolResult;
import org.junit.jupiter.api.Test;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class CreateDirectoryToolTest {
    @Test
    void testCreateDirectory() {
        CreateDirectoryTool tool = new CreateDirectoryTool();
        String testDir = "testDir";
        File dir = new File(testDir);
        if (dir.exists()) {
            dir.delete();
        }
        ToolResult result = tool.execute(java.util.Map.of("path", testDir));
        assertTrue(result.isSuccess(), "Directory should be created");
        assertTrue(dir.exists() && dir.isDirectory(), "Directory should exist");
        // Cleanup
        dir.delete();
    }

    @Test
    void testCreateDirectoryAlreadyExists() {
        CreateDirectoryTool tool = new CreateDirectoryTool();
        String testDir = "testDirExists";
        File dir = new File(testDir);
        dir.mkdirs();
        ToolResult result = tool.execute(java.util.Map.of("path", testDir));
        assertFalse(result.isSuccess(), "Should return failure if directory already exists");
        // Cleanup
        dir.delete();
    }
}

package com.manorrock.assistant;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolExecutionException;
import com.manorrock.assistant.api.ToolLifecycle;
import com.manorrock.assistant.api.ToolManager;
import com.manorrock.assistant.api.ToolResult;
import com.manorrock.assistant.tool.DefaultToolManager;
import com.manorrock.assistant.tool.FileReadTool;
import com.manorrock.assistant.tool.ProcessExecutionTool;
import com.manorrock.assistant.tool.MockTool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cross-Platform Tool Integration Tests")
class CrossPlatformToolTest {

    private ToolManager toolManager;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        toolManager = new DefaultToolManager();
        toolManager.registerTool(new ProcessExecutionTool());
        toolManager.registerTool(new FileReadTool());
    }

    @Test
    @DisplayName("File operations should work on all platforms")
    void testFileOperationsCrossPlatform() throws Exception, ToolExecutionException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        String testContent = "Test content\nLine 2";
        Files.writeString(testFile, testContent);

        Map<String, Object> params = new HashMap<>();
        params.put("path", testFile.toString());

        ToolResult result = toolManager.executeTool("file_read", params);
        assertTrue(result.isSuccess());
        assertEquals(testContent, result.getData().get("content"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @DisplayName("Windows-specific process execution")
    void testWindowsProcessExecution() throws ToolExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("command", "cmd.exe");
        params.put("args", "/c echo Hello Windows");

        ToolResult result = toolManager.executeTool("process_execution", params);
        assertTrue(result.isSuccess());
        assertTrue(result.getData().get("output").toString().contains("Hello Windows"));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("Unix-like process execution")
    void testUnixProcessExecution() throws ToolExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("command", "echo");
        params.put("args", "Hello Unix");

        ToolResult result = toolManager.executeTool("process_execution", params);
        assertTrue(result.isSuccess());
        assertTrue(result.getData().get("output").toString().contains("Hello Unix"));
    }

    @Test
    @DisplayName("Path handling should be platform-aware")
    void testPlatformSpecificPathHandling() throws ToolExecutionException {
        String platformPath = new File(tempDir.toString(), "test.txt").getPath();
        
        // MockTool just to test path handling
        MockTool mockTool = new MockTool("pathTest", "Test path handling");
        toolManager.registerTool(mockTool);

        Map<String, Object> params = new HashMap<>();
        params.put("requiredParam", platformPath);

        ToolResult result = toolManager.executeTool("pathTest", params);
        assertTrue(result.isSuccess());
        assertEquals(platformPath, mockTool.getLastExecutionParameters().get("requiredParam"));
    }

    @Test
    @DisplayName("Tool lifecycle should be consistent across platforms")
    void testToolLifecycleCrossPlatform() {
        MockTool mockTool = new MockTool("lifecycleTest", "Test lifecycle consistency");
        
        // Test registration
        toolManager.registerTool(mockTool);
        assertEquals(ToolLifecycle.READY, mockTool.getLifecycle());

        // Test disable/enable
        toolManager.disableTool("lifecycleTest");
        assertEquals(ToolLifecycle.DISABLED, mockTool.getLifecycle());
        
        toolManager.enableTool("lifecycleTest");
        assertEquals(ToolLifecycle.READY, mockTool.getLifecycle());

        // Test unregistration
        toolManager.unregisterTool("lifecycleTest");
        assertEquals(ToolLifecycle.UNREGISTERED, mockTool.getLifecycle());
    }

    @Test
    @DisplayName("Tool parameters should handle Unicode across platforms")
    void testUnicodeParameterHandling() throws ToolExecutionException {
        MockTool mockTool = new MockTool("unicodeTest", "Test Unicode handling");
        toolManager.registerTool(mockTool);

        Map<String, Object> params = new HashMap<>();
        params.put("requiredParam", "Hello, 世界! Здравствуйте! مرحبا");

        ToolResult result = toolManager.executeTool("unicodeTest", params);
        assertTrue(result.isSuccess());
        assertEquals("Hello, 世界! Здравствуйте! مرحبا", 
                    mockTool.getLastExecutionParameters().get("requiredParam"));
    }

    @Disabled
    @Test
    @DisplayName("Environment variables should be accessible across platforms")
    void testEnvironmentVariableAccess() throws ToolExecutionException {
        Map<String, Object> params = new HashMap<>();
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            params.put("command", "cmd.exe");
            params.put("args", "/c echo %JAVA_HOME%");
        } else {
            params.put("command", "sh");
            params.put("args", "-c 'echo $JAVA_HOME'");
        }

        ToolResult result = toolManager.executeTool("process_execution", params);
        assertTrue(result.isSuccess());
        String output = result.getData().get("output").toString();
        assertNotNull(output);
        assertFalse(output.trim().isEmpty());
        assertTrue(output.toLowerCase().contains("java") || output.toLowerCase().contains("jdk"),
                "Output '" + output.trim() + "' should contain Java-related path");
    }
}

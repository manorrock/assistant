package com.manorrock.assistant.core;

import com.manorrock.assistant.api.ToolManager;
import com.manorrock.assistant.api.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoreToolManagerToolTest {

    private ToolManager mockToolManager;
    private CoreToolManagerTool coreToolManagerTool;

    @BeforeEach
    void setUp() {
        mockToolManager = mock(ToolManager.class);
        coreToolManagerTool = new CoreToolManagerTool(mockToolManager);
    }

    @Test
    void testGetName() {
        assertEquals("tool_manager", coreToolManagerTool.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Provides access to the tool manager, allowing operations such as listing, enabling, and disabling tools", coreToolManagerTool.getDescription());
    }

    @Test
    void testGetParameters() {
        List<?> parameters = coreToolManagerTool.getParameters();
        assertNotNull(parameters);
        assertEquals(2, parameters.size());
    }

    @Test
    void testExecuteListOperation() {
        when(mockToolManager.getAvailableTools()).thenReturn(List.of());

        ToolResult result = coreToolManagerTool.execute(Map.of("operation", "list"));

        assertTrue(result.isSuccess());
        assertEquals(0, ((List<?>) result.getData().get("tools")).size());
        assertEquals("Retrieved 0 available tools", result.getMessage());
    }

    @Test
    void testExecuteEnableOperation() {
        when(mockToolManager.findTool("testTool")).thenReturn(Optional.of(mock(CoreToolManagerTool.class)));

        ToolResult result = coreToolManagerTool.execute(Map.of("operation", "enable", "toolName", "testTool"));

        assertTrue(result.isSuccess());
        assertEquals("testTool", result.getData().get("toolName"));
        assertEquals("enabled", result.getData().get("status"));
        assertEquals("Tool 'testTool' has been enabled", result.getMessage());
        verify(mockToolManager).enableTool("testTool");
    }

    @Test
    void testExecuteDisableOperation() {
        when(mockToolManager.findTool("testTool")).thenReturn(Optional.of(mock(CoreToolManagerTool.class)));

        ToolResult result = coreToolManagerTool.execute(Map.of("operation", "disable", "toolName", "testTool"));

        assertTrue(result.isSuccess());
        assertEquals("testTool", result.getData().get("toolName"));
        assertEquals("disabled", result.getData().get("status"));
        assertEquals("Tool 'testTool' has been disabled", result.getMessage());
        verify(mockToolManager).disableTool("testTool");
    }

    @Test
    void testExecuteDisableSelf() {
        // Mock the toolManager to return a tool when findTool("tool_manager") is called
        when(mockToolManager.findTool("tool_manager")).thenReturn(Optional.of(coreToolManagerTool));
        
        ToolResult result = coreToolManagerTool.execute(Map.of("operation", "disable", "toolName", "tool_manager"));

        assertFalse(result.isSuccess());
        assertEquals("Cannot disable the tool_manager tool", result.getMessage());
    }

    @Test
    void testExecuteInfoOperation() {
        CoreToolManagerTool mockTool = mock(CoreToolManagerTool.class);
        when(mockTool.getName()).thenReturn("testTool");
        when(mockTool.getDescription()).thenReturn("Test tool description");
        when(mockTool.getParameters()).thenReturn(List.of());
        when(mockToolManager.findTool("testTool")).thenReturn(Optional.of(mockTool));

        ToolResult result = coreToolManagerTool.execute(Map.of("operation", "info", "toolName", "testTool"));

        assertTrue(result.isSuccess());
        assertEquals("testTool", result.getData().get("name"));
        assertEquals("Test tool description", result.getData().get("description"));
        assertEquals("Tool information retrieved successfully", result.getMessage());
    }

    @Test
    void testExecuteUnknownOperation() {
        ToolResult result = coreToolManagerTool.execute(Map.of("operation", "unknown"));

        assertFalse(result.isSuccess());
        assertEquals("Unknown operation: unknown. Supported operations are: list, enable, disable, info", result.getMessage());
    }

    @Test
    void testExecuteMissingOperationParameter() {
        ToolResult result = coreToolManagerTool.execute(Map.of());

        assertFalse(result.isSuccess());
        assertEquals("Missing required parameter: operation", result.getMessage());
    }
}
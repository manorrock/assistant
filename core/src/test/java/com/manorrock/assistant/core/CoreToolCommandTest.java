package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolManager;
import com.manorrock.assistant.api.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CoreToolCommandTest {

    private CoreAssistant assistant;
    private ToolManager toolManager;
    private CoreToolCommand command;

    @BeforeEach
    void setUp() {
        assistant = mock(CoreAssistant.class);
        toolManager = mock(ToolManager.class);
        when(assistant.getToolManager()).thenReturn(toolManager);
        command = new CoreToolCommand(assistant);
    }

    @Test
    void testListTools_noToolsAvailable() {
        when(toolManager.getAvailableTools()).thenReturn(Collections.emptyList());

        String result = command.execute("list");

        assertEquals("No tools available", result);
    }

    @Test
    void testListTools_withToolsAvailable() {
        Tool tool = mock(Tool.class);
        when(tool.getName()).thenReturn("exampleTool");
        when(tool.getDescription()).thenReturn("An example tool");
        when(toolManager.getAvailableTools()).thenReturn(List.of(tool));

        String result = command.execute("list");

        assertEquals("Available tools:\n  - exampleTool: An example tool\n\nUse '/tool info <tool-name>' for details about a specific tool.\nUse '/tool execute <tool-name> [params]' to execute a tool.", result);
    }

    @Test
    void testGetToolInfo_toolNotFound() {
        when(toolManager.findTool("nonexistentTool")).thenReturn(Optional.empty());

        String result = command.execute("info nonexistentTool");

        assertEquals("Tool not found: nonexistentTool", result);
    }

    @Test
    void testGetToolInfo_toolFound() {
        Tool tool = mock(Tool.class);
        when(tool.getName()).thenReturn("exampleTool");
        when(tool.getDescription()).thenReturn("An example tool");
        when(tool.getParameters()).thenReturn(Collections.emptyList());
        when(toolManager.findTool("exampleTool")).thenReturn(Optional.of(tool));

        String result = command.execute("info exampleTool");

        assertEquals("Tool: exampleTool\nDescription: An example tool\nParameters:\n  None\n\nUsage: /tool execute exampleTool [param1=value1 param2=value2 ...]", result);
    }

    @Test
    void testExecuteTool_toolExecutionSuccess() {
        ToolResult toolResult = mock(ToolResult.class);
        when(toolResult.isSuccess()).thenReturn(true);
        when(toolResult.getMessage()).thenReturn("Execution successful");
        when(toolResult.getData()).thenReturn(Map.of("key", "value"));
        when(toolManager.executeTool(eq("exampleTool"), anyMap())).thenReturn(toolResult);

        String result = command.execute("execute exampleTool");

        assertEquals("Tool execution succeeded\nMessage: Execution successful\nResult data:\n  key: value\n", result);
    }

    @Test
    void testExecuteTool_toolExecutionFailure() {
        ToolResult toolResult = mock(ToolResult.class);
        when(toolResult.isSuccess()).thenReturn(false);
        when(toolResult.getMessage()).thenReturn("Execution failed");
        when(toolResult.getData()).thenReturn(Collections.emptyMap());
        when(toolManager.executeTool(eq("exampleTool"), anyMap())).thenReturn(toolResult);

        String result = command.execute("execute exampleTool");

        assertEquals("Tool execution failed\nMessage: Execution failed\n", result);
    }

    @Test
    void testExecuteTool_missingToolName() {
        String result = command.execute("execute");

        assertEquals("Error: Tool name required\nUsage: tool execute <tool-name> [param1=value1 param2=value2 ...]", result);
    }

    @Test
    void testUnknownSubcommand() {
        String result = command.execute("unknown");

        assertEquals("Unknown subcommand: unknown\nAvailable subcommands: list, execute, info, status", result);
    }

    @Test
    void testGetToolStatus() {
        when(toolManager.getAvailableTools()).thenReturn(List.of(mock(Tool.class), mock(Tool.class)));

        String result = command.execute("status");

        assertEquals("Tool system status: 2 available tools", result);
    }
}
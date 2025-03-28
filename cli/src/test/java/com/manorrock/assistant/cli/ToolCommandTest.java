package com.manorrock.assistant.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolResult;
import com.manorrock.assistant.command.ToolCommand;
import com.manorrock.assistant.core.Assistant;

/**
 * Tests for the ToolCommand class.
 */
public class ToolCommandTest {
    
    /**
     * Mock tool implementation for testing.
     */
    private static class MockTool implements Tool {
        private final String name;
        private final String description;
        
        public MockTool(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getDescription() {
            return description;
        }
        
        @Override
        public List<com.manorrock.assistant.api.ToolParameter> getParameters() {
            return Collections.emptyList();
        }
        
        @Override
        public ToolResult execute(Map<String, Object> parameters) {
            return ToolResult.success(Collections.singletonMap("status", "success"), 
                    "Tool " + name + " executed successfully");
        }
    }
    
    /**
     * Test that the tool command works correctly.
     */
    @Test
    public void testToolCommand() {
        // Create an Assistant instance for the test
        Assistant assistant = new Assistant();
        
        // Create a list of mock tools
        List<Tool> mockTools = new ArrayList<>();
        mockTools.add(new MockTool("test_tool", "A test tool"));
        
        // Create a supplier for tools
        Supplier<List<Tool>> toolSupplier = () -> mockTools;
        
        // Create a simple tool executor
        BiFunction<String, Map<String, Object>, ToolResult> toolExecutor = 
            (toolName, params) -> {
                for (Tool tool : mockTools) {
                    if (tool.getName().equals(toolName)) {
                        return tool.execute(params);
                    }
                }
                return ToolResult.failure("Tool not found: " + toolName);
            };
        
        // Register the tool command
        ToolCommand toolCommand = new ToolCommand(toolSupplier, toolExecutor);
        assistant.getCommandRegistry().registerCommand("tool", toolCommand);
        
        // Verify registration
        Command cmd = assistant.getCommandRegistry().getCommand("tool");
        assertNotNull(cmd);
        assertTrue(cmd instanceof ToolCommand);
        
        // Execute the command without parameters (should list tools)
        String result = cmd.executeToString("");
        assertTrue(result.contains("Available tools:"));
        assertTrue(result.contains("test_tool"));
        assertTrue(result.contains("A test tool"));
        
        // Execute with a specific tool
        result = cmd.executeToString("execute test_tool");
        assertTrue(result.contains("Tool execution succeeded"));
        assertTrue(result.contains("Tool test_tool executed successfully"));
        
        // Test info subcommand
        result = cmd.executeToString("info test_tool");
        assertTrue(result.contains("Tool: test_tool"));
        assertTrue(result.contains("Description: A test tool"));
    }
    
    /**
     * Test that the tool command correctly handles tool integration toggle.
     */
    @Test
    public void testToolIntegrationToggle() {
        // Create an Assistant instance for the test
        Assistant assistant = new Assistant();
        
        // Tracking variable for integration status
        boolean[] integrationEnabled = {true};
        
        // Create a list of mock tools
        List<Tool> mockTools = new ArrayList<>();
        mockTools.add(new MockTool("test_tool", "A test tool"));
        
        // Create a supplier for tools
        Supplier<List<Tool>> toolSupplier = () -> mockTools;
        
        // Create a simple tool executor
        BiFunction<String, Map<String, Object>, ToolResult> toolExecutor = 
            (toolName, params) -> {
                for (Tool tool : mockTools) {
                    if (tool.getName().equals(toolName)) {
                        return tool.execute(params);
                    }
                }
                return ToolResult.failure("Tool not found: " + toolName);
            };
        
        // Register the tool command with integration support
        ToolCommand toolCommand = new ToolCommand(
                toolSupplier, 
                toolExecutor,
                enabled -> integrationEnabled[0] = enabled,
                () -> integrationEnabled[0]
        );
        
        assistant.getCommandRegistry().registerCommand("tool", toolCommand);
        
        // Get the registered command
        Command cmd = assistant.getCommandRegistry().getCommand("tool");
        assertNotNull(cmd);
        
        // Test status subcommand
        String result = cmd.executeToString("status");
        assertTrue(result.contains("LLM tool integration is currently enabled"));
        
        // Test disable integration
        result = cmd.executeToString("integration off");
        assertTrue(result.contains("LLM tool integration disabled"));
        assertFalse(integrationEnabled[0]);
        
        // Test enable integration
        result = cmd.executeToString("integration on");
        assertTrue(result.contains("LLM tool integration enabled"));
        assertTrue(integrationEnabled[0]);
    }
}
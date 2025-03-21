package com.manorrock.assistant.cli;

import com.manorrock.assistant.shared.Tool;
import com.manorrock.assistant.shared.ToolParameter;
import com.manorrock.assistant.shared.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the CliToolRegistry class.
 */
public class CliToolRegistryTest {
    
    private CliToolRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = new CliToolRegistry();
    }
    
    @Test
    public void testDefaultToolRegistration() {
        // Verify that default tools are registered
        List<Tool> tools = registry.getRegisteredTools();
        assertFalse(tools.isEmpty(), "Default tools should be registered");
        
        // Verify that we can find specific default tools
        boolean foundFileReadTool = false;
        boolean foundProcessExecutionTool = false;
        
        for (Tool tool : tools) {
            if ("file_read".equals(tool.getName())) {
                foundFileReadTool = true;
            }
            if ("process_execution".equals(tool.getName())) {
                foundProcessExecutionTool = true;
            }
        }
        
        assertTrue(foundFileReadTool, "FileReadTool should be registered by default");
        assertTrue(foundProcessExecutionTool, "ProcessExecutionTool should be registered by default");
    }
    
    @Test
    public void testRegisterAndUnregisterTool() {
        // Create a mock tool
        Tool mockTool = new MockTool("mock_tool", "A mock tool for testing");
        
        // Register the tool
        boolean registered = registry.registerTool(mockTool);
        assertTrue(registered, "Tool should be registered successfully");
        
        // Verify that the tool is in the list of registered tools
        List<Tool> tools = registry.getRegisteredTools();
        boolean foundMockTool = false;
        
        for (Tool tool : tools) {
            if ("mock_tool".equals(tool.getName())) {
                foundMockTool = true;
            }
        }
        
        assertTrue(foundMockTool, "Mock tool should be in the list of registered tools");
        
        // Unregister the tool
        boolean unregistered = registry.unregisterTool("mock_tool");
        assertTrue(unregistered, "Tool should be unregistered successfully");
        
        // Verify that the tool is no longer in the list of registered tools
        tools = registry.getRegisteredTools();
        foundMockTool = false;
        
        for (Tool tool : tools) {
            if ("mock_tool".equals(tool.getName())) {
                foundMockTool = true;
            }
        }
        
        assertFalse(foundMockTool, "Mock tool should not be in the list of registered tools");
    }
    
    @Test
    public void testValidateTool() {
        // Try to register a null tool
        boolean registered = registry.registerTool(null);
        assertFalse(registered, "Null tool should not be registered");
        
        // Try to register a tool with a null name
        Tool nullNameTool = new MockTool(null, "Description");
        registered = registry.registerTool(nullNameTool);
        assertFalse(registered, "Tool with null name should not be registered");
        
        // Try to register a tool with an empty name
        Tool emptyNameTool = new MockTool("", "Description");
        registered = registry.registerTool(emptyNameTool);
        assertFalse(registered, "Tool with empty name should not be registered");
        
        // Try to register a tool with a null description
        Tool nullDescTool = new MockTool("name", null);
        registered = registry.registerTool(nullDescTool);
        assertFalse(registered, "Tool with null description should not be registered");
        
        // Try to register a tool with an empty description
        Tool emptyDescTool = new MockTool("name", "");
        registered = registry.registerTool(emptyDescTool);
        assertFalse(registered, "Tool with empty description should not be registered");
    }
    
    /**
     * Mock implementation of Tool for testing.
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
        public List<ToolParameter> getParameters() {
            return new ArrayList<>();
        }
        
        @Override
        public ToolResult execute(Map<String, Object> parameters) {
            ToolResult result = ToolResult.success(
                new HashMap<>(),  // empty data map
                "Test execution successful"  // message
            );
            return result;
        }
    }
}

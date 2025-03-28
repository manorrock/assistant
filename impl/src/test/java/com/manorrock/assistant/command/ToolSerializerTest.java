package com.manorrock.assistant.command;

import static org.junit.Assert.*;
import org.junit.Test;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;
import com.manorrock.assistant.api.ToolExecutionException;
import com.manorrock.assistant.api.ToolLifecycle;

import java.util.*;

public class ToolSerializerTest {

    @Test
    public void testSerializeTool() {
        List<ToolParameter> parameters = Arrays.asList(
            new ToolParameter("param1", "string", "A string parameter", true),
            new ToolParameter("param2", "number", "A numeric parameter", false)
        );
        
        Tool tool = new MockTool("testTool", "A test tool", parameters);
        
        String json = ToolSerializer.serializeTool(tool);
        
        // Verify the JSON contains expected values
        assertTrue(json.contains("\"name\":\"testTool\""));
        assertTrue(json.contains("\"description\":\"A test tool\""));
        assertTrue(json.contains("\"name\":\"param1\""));
        assertTrue(json.contains("\"type\":\"string\""));
        assertTrue(json.contains("\"description\":\"A string parameter\""));
        assertTrue(json.contains("\"required\":true"));
        assertTrue(json.contains("\"name\":\"param2\""));
        assertTrue(json.contains("\"required\":false"));
    }

    @Test
    public void testSerializeToolWithEmptyParameters() {
        Tool tool = new MockTool("emptyTool", "A tool with no parameters", Collections.emptyList());
        
        String json = ToolSerializer.serializeTool(tool);
        
        assertTrue(json.contains("\"name\":\"emptyTool\""));
        assertTrue(json.contains("\"parameters\":[]"));
    }

    @Test
    public void testCreateToolRequestJson() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        parameters.put("param2", 42);
        parameters.put("param3", true);
        
        Map<String, Object> context = new HashMap<>();
        context.put("contextKey", "contextValue");
        
        String json = ToolSerializer.createToolRequestJson("testTool", parameters, context);
        
        // Verify JSON structure
        assertTrue(json.contains("\"toolName\":\"testTool\""));
        assertTrue(json.contains("\"param1\":\"value1\""));
        assertTrue(json.contains("\"param2\":42"));
        assertTrue(json.contains("\"param3\":true"));
        assertTrue(json.contains("\"contextKey\":\"contextValue\""));
    }

    @Test
    public void testCreateToolRequestJsonWithNullValues() {
        String json = ToolSerializer.createToolRequestJson("testTool", null, null);
        
        assertEquals("{\"toolName\":\"testTool\"}", json);
    }

    @Test
    public void testCreateToolRequestJsonWithEmptyMaps() {
        String json = ToolSerializer.createToolRequestJson("testTool", 
                Collections.emptyMap(), Collections.emptyMap());
        
        assertEquals("{\"toolName\":\"testTool\"}", json);
    }

    @Test
    public void testEscapeJsonSpecialChars() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("text", "Line 1\nLine \"2\"\tTabbed");
        
        String json = ToolSerializer.createToolRequestJson("testTool", parameters, null);
        
        assertTrue(json.contains("\"text\":\"Line 1\\nLine \\\"2\\\"\\tTabbed\""));
    }

    // Mock implementation of the Tool interface for testing
    private static class MockTool implements Tool {
        private final String name;
        private final String description;
        private final List<ToolParameter> parameters;
        private ToolLifecycle lifecycle = ToolLifecycle.READY;

        public MockTool(String name, String description, List<ToolParameter> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
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
            return parameters;
        }

        @Override
        public ToolResult execute(Map<String, Object> parameters) throws ToolExecutionException {
            return ToolResult.success(Collections.emptyMap(), "Test execution");
        }

        @Override
        public boolean initialize() {
            return true;
        }

        @Override
        public void cleanup() {
            // No-op for test
        }

        @Override
        public ToolLifecycle getLifecycle() {
            return lifecycle;
        }

        @Override
        public void setLifecycle(ToolLifecycle lifecycle) {
            this.lifecycle = lifecycle;
        }
    }
}
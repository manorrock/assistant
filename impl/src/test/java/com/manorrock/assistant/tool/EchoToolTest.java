package com.manorrock.assistant.tool;

import org.junit.jupiter.api.Test;

import com.manorrock.assistant.api.ToolResult;
import com.manorrock.assistant.tool.EchoTool;

import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EchoToolTest {

    private final EchoTool tool = new EchoTool();

    @Test
    void testMetadata() {
        assertEquals("echo", tool.getName());
        assertNotNull(tool.getDescription());
        assertEquals(1, tool.getParameters().size());
        assertEquals("message", tool.getParameters().get(0).getName());
    }

    @Test
    void testSuccessfulExecution() {
        String testMessage = "Hello, World!";
        ToolResult result = tool.execute(Collections.singletonMap("message", testMessage));
        
        assertTrue(result.success());
        assertEquals("Echo successful", result.message());
        assertEquals(testMessage, result.data().get("echo"));
    }

    @Test
    void testMissingParameter() {
        ToolResult result = tool.execute(Collections.emptyMap());
        
        assertFalse(result.success());
        assertEquals("Message parameter is required", result.message());
        assertTrue(result.data().isEmpty());
    }
}

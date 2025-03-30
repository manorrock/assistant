package com.manorrock.assistant.core;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.manorrock.assistant.api.Tool;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoreToolManagerTest {

    @Test
    void testRegisterTool() {
        CoreToolManager manager = new CoreToolManager(null);
        
        // null tool should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> manager.registerTool(null)
        );
        assertEquals("Tool cannot be null", exception.getMessage());
        
        // Create a valid mock tool
        Tool mockTool = createValidMockTool("testTool");
        
        // Should not throw exception with valid tool
        assertDoesNotThrow(() -> manager.registerTool(mockTool));
        
        // Registering same tool name should throw exception
        Tool duplicateTool = createValidMockTool("testTool");
        exception = assertThrows(
            IllegalArgumentException.class,
            () -> manager.registerTool(duplicateTool)
        );
        assertTrue(exception.getMessage().contains("is already registered"));
    }

    @Test
    void testUnregisterTool() {
        CoreToolManager manager = new CoreToolManager(null);
        
        // Unregistering non-existent tool should return false
        assertFalse(manager.unregisterTool("nonExistentTool"));
        
        // Register a tool and then unregister it
        Tool mockTool = createValidMockTool("testTool");
        manager.registerTool(mockTool);
        
        // Should return true when unregistering existing tool
        assertTrue(manager.unregisterTool("testTool"));
        
        // Verify tool is actually unregistered
        assertFalse(manager.getAvailableTools().stream()
            .anyMatch(tool -> "testTool".equals(tool.getName())));
    }

    @Test
    void testGetAvailableTools() {
        CoreToolManager manager = new CoreToolManager(null);
        assertNotNull(manager.getAvailableTools());
        
        // At least one tool should be present (CoreToolManagerTool)
        assertTrue(manager.getAvailableTools().size() >= 1);
        
        // Add another tool and verify it's in the list
        Tool mockTool = createValidMockTool("testTool");
        manager.registerTool(mockTool);
        
        List<Tool> tools = manager.getAvailableTools();
        assertTrue(tools.stream().anyMatch(tool -> "testTool".equals(tool.getName())));
    }

    @Test
    void testFindTool() {
        CoreToolManager manager = new CoreToolManager(null);
        
        // Finding non-existent tool should return empty Optional
        Optional<Tool> result = manager.findTool("nonExistentTool");
        assertTrue(result.isEmpty());
        
        // Register a tool
        Tool mockTool = createValidMockTool("testTool");
        manager.registerTool(mockTool);
        
        // Finding existing tool should return the tool
        result = manager.findTool("testTool");
        assertTrue(result.isPresent());
        assertEquals("testTool", result.get().getName());
    }

    @Test
    void testExecuteTool() {
        CoreToolManager manager = new CoreToolManager(null);
        
        // Executing non-existent tool should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> manager.executeTool("nonExistentTool", Map.of())
        );
        assertEquals("Tool not found: nonExistentTool", exception.getMessage());
        
        // Register a valid tool
        Tool mockTool = createValidMockTool("testTool");
        manager.registerTool(mockTool);
        
        // Execute the tool
        assertDoesNotThrow(() -> manager.executeTool("testTool", Map.of()));
    }

    @Test
    void testGenerateToolDescriptionsForLlm() {
        CoreToolManager manager = new CoreToolManager(null);
        
        // Should not throw exception
        String result = manager.generateToolDescriptionsForLlm();
        
        // Result should not be null or empty
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // Result should contain information about available tools
        assertTrue(result.contains("Available tools:"));
    }

    @Test
    void testEnableTool() {
        CoreToolManager manager = new CoreToolManager(null);
        
        // Enabling non-existent tool should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> manager.enableTool("nonExistentTool")
        );
        assertEquals("Tool not found: nonExistentTool", exception.getMessage());
        
        // Register a tool
        Tool mockTool = createValidMockTool("testTool");
        manager.registerTool(mockTool);
        
        // Should not throw exception for existing tool
        assertDoesNotThrow(() -> manager.enableTool("testTool"));
    }

    @Test
    void testDisableTool() {
        CoreToolManager manager = new CoreToolManager(null);
        
        // Disabling non-existent tool should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> manager.disableTool("nonExistentTool")
        );
        assertEquals("Tool not found: nonExistentTool", exception.getMessage());
        
        // Register a tool
        Tool mockTool = createValidMockTool("testTool");
        manager.registerTool(mockTool);
        
        // Should not throw exception for existing tool
        assertDoesNotThrow(() -> manager.disableTool("testTool"));
        
        // After disabling, the tool should not be in available tools
        assertFalse(manager.getAvailableTools().stream()
            .anyMatch(tool -> "testTool".equals(tool.getName())));
    }
    
    /**
     * Helper method to create a valid mock Tool for testing
     */
    private Tool createValidMockTool(String name) {
        Tool mockTool = Mockito.mock(Tool.class);
        when(mockTool.getName()).thenReturn(name);
        when(mockTool.getDescription()).thenReturn("Test Description");
        when(mockTool.getParameters()).thenReturn(Collections.emptyList());
        when(mockTool.initialize()).thenReturn(true);
        return mockTool;
    }
}
package com.manorrock.assistant.core;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CoreToolManagerTest {

    @Test
    void testRegisterTool() {
        CoreToolManager manager = new CoreToolManager();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> manager.registerTool(null)
        );
        assertEquals("Unimplemented method 'registerTool'", exception.getMessage());
    }

    @Test
    void testUnregisterTool() {
        CoreToolManager manager = new CoreToolManager();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> manager.unregisterTool("testTool")
        );
        assertEquals("Unimplemented method 'unregisterTool'", exception.getMessage());
    }

    @Test
    void testGetAvailableTools() {
        CoreToolManager manager = new CoreToolManager();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            manager::getAvailableTools
        );
        assertEquals("Unimplemented method 'getAvailableTools'", exception.getMessage());
    }

    @Test
    void testFindTool() {
        CoreToolManager manager = new CoreToolManager();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> manager.findTool("testTool")
        );
        assertEquals("Unimplemented method 'findTool'", exception.getMessage());
    }

    @Test
    void testExecuteTool() {
        CoreToolManager manager = new CoreToolManager();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> manager.executeTool("testTool", Map.of())
        );
        assertEquals("Unimplemented method 'executeTool'", exception.getMessage());
    }

    @Test
    void testGenerateToolDescriptionsForLlm() {
        CoreToolManager manager = new CoreToolManager();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            manager::generateToolDescriptionsForLlm
        );
        assertEquals("Unimplemented method 'generateToolDescriptionsForLlm'", exception.getMessage());
    }

    @Test
    void testEnableTool() {
        CoreToolManager manager = new CoreToolManager();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> manager.enableTool("testTool")
        );
        assertEquals("Unimplemented method 'enableTool'", exception.getMessage());
    }

    @Test
    void testDisableTool() {
        CoreToolManager manager = new CoreToolManager();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> manager.disableTool("testTool")
        );
        assertEquals("Unimplemented method 'disableTool'", exception.getMessage());
    }
}
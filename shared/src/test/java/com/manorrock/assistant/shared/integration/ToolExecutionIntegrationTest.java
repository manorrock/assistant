package com.manorrock.assistant.shared.integration;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolExecutionException;
import com.manorrock.assistant.api.ToolLifecycle;
import com.manorrock.assistant.api.ToolManager;
import com.manorrock.assistant.api.ToolResult;
import com.manorrock.assistant.shared.DefaultToolManager;
import com.manorrock.assistant.shared.tools.test.MockTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tool Execution Integration Tests")
class ToolExecutionIntegrationTest {
    
    private ToolManager toolManager;
    private MockTool mockTool;
    
    @BeforeEach
    void setUp() {
        toolManager = new DefaultToolManager();
        mockTool = new MockTool("test_tool", "Test tool for integration testing");
    }
    
    @Nested
    @DisplayName("Tool Registration")
    class ToolRegistrationTests {
        
        @Test
        @DisplayName("Successfully register valid tool")
        void testRegisterValidTool() {
            toolManager.registerTool(mockTool);
            assertTrue(toolManager.findTool("test_tool").isPresent());
            assertEquals(ToolLifecycle.READY, mockTool.getLifecycle());
        }
        
        @Test
        @DisplayName("Fail to register null tool")
        void testRegisterNullTool() {
            assertThrows(IllegalArgumentException.class, () -> toolManager.registerTool(null));
        }
        
        @Test
        @DisplayName("Fail to register tool with initialization failure")
        void testRegisterToolWithInitFailure() {
            mockTool.setShouldFailInitialization(true);
            assertThrows(IllegalStateException.class, () -> toolManager.registerTool(mockTool));
            assertEquals(ToolLifecycle.ERROR, mockTool.getLifecycle());
        }
    }
    
    @Nested
    @DisplayName("Tool Execution")
    class ToolExecutionTests {
        
        @BeforeEach
        void setUp() {
            toolManager.registerTool(mockTool);
        }
        
        @Test
        @DisplayName("Successfully execute tool with valid parameters")
        void testExecuteToolWithValidParams() throws ToolExecutionException {
            Map<String, Object> params = new HashMap<>();
            params.put("requiredParam", "test");
            params.put("optionalParam", 42);
            
            ToolResult result = toolManager.executeTool("test_tool", params);
            
            assertTrue(result.isSuccess());
            assertNotNull(mockTool.getLastExecutionParameters());
            assertEquals("test", mockTool.getLastExecutionParameters().get("requiredParam"));
            assertEquals(42, mockTool.getLastExecutionParameters().get("optionalParam"));
        }
        
        @Test
        @DisplayName("Fail execution when missing required parameter")
        void testExecuteToolMissingRequiredParam() throws ToolExecutionException {
            Map<String, Object> params = new HashMap<>();
            params.put("optionalParam", 42);
            
            assertThrows(Exception.class, () -> toolManager.executeTool("test_tool", params));
        }
        
        @Test
        @DisplayName("Handle tool execution failure")
        void testExecuteToolFailure() throws ToolExecutionException {
            mockTool.setShouldFailExecution(true);
            Map<String, Object> params = new HashMap<>();
            params.put("requiredParam", "test");
            
            assertThrows(Exception.class, () -> toolManager.executeTool("test_tool", params));
            assertEquals(ToolLifecycle.ERROR, mockTool.getLifecycle());
        }
    }
    
    @Nested
    @DisplayName("Tool Lifecycle")
    class ToolLifecycleTests {
        
        @BeforeEach
        void setUp() {
            toolManager.registerTool(mockTool);
        }
        
        @Test
        @DisplayName("Successfully disable and enable tool")
        void testDisableAndEnableTool() {
            toolManager.disableTool("test_tool");
            assertEquals(ToolLifecycle.DISABLED, mockTool.getLifecycle());
            
            toolManager.enableTool("test_tool");
            assertEquals(ToolLifecycle.READY, mockTool.getLifecycle());
        }
        
        @Test
        @DisplayName("Successfully unregister tool")
        void testUnregisterTool() {
            assertTrue(toolManager.unregisterTool("test_tool"));
            assertEquals(ToolLifecycle.UNREGISTERED, mockTool.getLifecycle());
            assertTrue(toolManager.findTool("test_tool").isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Tool Discovery")
    class ToolDiscoveryTests {
        
        @Test
        @DisplayName("List available tools")
        void testListAvailableTools() {
            toolManager.registerTool(mockTool);
            MockTool anotherTool = new MockTool("another_tool", "Another test tool");
            toolManager.registerTool(anotherTool);
            
            assertEquals(2, toolManager.getAvailableTools().size());
            assertTrue(toolManager.getAvailableTools().stream()
                    .anyMatch(t -> t.getName().equals("test_tool")));
            assertTrue(toolManager.getAvailableTools().stream()
                    .anyMatch(t -> t.getName().equals("another_tool")));
        }
        
        @Test
        @DisplayName("Generate tool descriptions for LLM")
        void testGenerateToolDescriptions() {
            toolManager.registerTool(mockTool);
            String descriptions = toolManager.generateToolDescriptionsForLlm();
            
            assertTrue(descriptions.contains("\"name\": \"test_tool\""));
            assertTrue(descriptions.contains("\"requiredParam\""));
            assertTrue(descriptions.contains("\"optionalParam\""));
        }
    }
}

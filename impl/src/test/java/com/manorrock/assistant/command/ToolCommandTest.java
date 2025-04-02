package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;
import com.manorrock.assistant.api.ToolLifecycle;
import org.junit.Before;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class ToolCommandTest {

    private ToolCommand command;
    private List<Tool> toolList;
    private boolean integrationStatus;
    private final Map<String, ToolResult> executionResults = new HashMap<>();

    @Before
    public void setUp() {
        integrationStatus = false;
        toolList = new ArrayList<>();
        
        // Sample tool for testing
        toolList.add(createSampleTool("testTool", "A test tool", 
                Arrays.asList(
                    new ToolParameter("param1", "string", "First parameter", true),
                    new ToolParameter("param2", "number", "Second parameter", false)
                )));
        
        // Set up execution results for mocking
        executionResults.put("testTool", ToolResult.success(Map.of("result", "test value"), "Success"));
        
        command = new ToolCommand(
            () -> toolList,
            this::mockToolExecutor,
            (value) -> integrationStatus = value,
            () -> integrationStatus
        );
    }

    @Test
    public void testGetDescription() {
        assertEquals("Manage and execute tools", command.getDescription());
    }

    @Test
    public void testEmptyCommandListsTools() {
        String result = command.execute("");
        assertThat(result, containsString("Available tools:"));
        assertThat(result, containsString("testTool"));
    }

    @Test
    public void testListCommand() {
        String result = command.execute("list");
        assertThat(result, containsString("Available tools:"));
        assertThat(result, containsString("testTool"));
    }

    @Test
    public void testInfoCommand() {
        String result = command.execute("info testTool");
        assertThat(result, containsString("Tool: testTool"));
        assertThat(result, containsString("param1 (string) [Required]"));
        assertThat(result, containsString("param2 (number)"));
    }

    @Test
    public void testInfoCommandWithNonExistentTool() {
        String result = command.execute("info nonExistentTool");
        assertThat(result, containsString("Tool not found"));
    }

    @Test
    public void testExecuteCommand() {
        String result = command.execute("execute testTool param1=value1 param2=42");
        assertThat(result, containsString("Tool execution succeeded"));
        assertThat(result, containsString("Success"));
    }

    @Test
    public void testExecuteCommandWithoutParameters() {
        String result = command.execute("execute");
        assertThat(result, containsString("Error: Tool name required"));
    }

    @Test
    public void testToggleIntegration() {
        // Integration starts as off
        assertFalse(integrationStatus);
        
        // Turn on
        String resultOn = command.execute("integration on");
        assertTrue(integrationStatus);
        assertThat(resultOn, containsString("enabled"));
        
        // Turn off
        String resultOff = command.execute("integration off");
        assertFalse(integrationStatus);
        assertThat(resultOff, containsString("disabled"));
        
        // Toggle (should turn on)
        String resultToggle = command.execute("integration");
        assertTrue(integrationStatus);
        assertThat(resultToggle, containsString("enabled"));
    }

    @Test
    public void testInvalidIntegrationParameter() {
        String result = command.execute("integration invalid");
        assertThat(result, containsString("Invalid parameter"));
    }

    @Test
    public void testStatusCommand() {
        String result = command.execute("status");
        assertThat(result, containsString("LLM tool integration is currently disabled"));
    }

    @Test
    public void testUnknownSubcommand() {
        String result = command.execute("unknown");
        assertThat(result, containsString("Unknown subcommand"));
    }

    @Test
    public void testConstructorWithoutIntegration() {
        ToolCommand simpleCommand = new ToolCommand(
            () -> toolList,
            this::mockToolExecutor
        );
        
        // Integration commands should indicate not supported
        String result = simpleCommand.execute("integration on");
        assertThat(result, containsString("not supported"));
        
        String statusResult = simpleCommand.execute("status");
        assertThat(statusResult, containsString("not available"));
    }

    // Helper methods for mocking
    private Tool createSampleTool(String name, String description, List<ToolParameter> parameters) {
        return new Tool() {
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
            public ToolResult execute(Map<String, Object> parameters) {
                return executionResults.getOrDefault(name, 
                    ToolResult.failure("Tool execution failed"));
            }
            
            @Override
            public boolean initialize() {
                return true;
            }
            
            @Override
            public void cleanup() {
            }
            
            @Override
            public ToolLifecycle getLifecycle() {
                return ToolLifecycle.READY;
            }
            
            @Override
            public void setLifecycle(ToolLifecycle lifecycle) {
            }
        };
    }

    private ToolResult mockToolExecutor(String toolName, Map<String, Object> parameters) {
        return executionResults.getOrDefault(toolName, 
            ToolResult.failure("Tool not found"));
    }
}

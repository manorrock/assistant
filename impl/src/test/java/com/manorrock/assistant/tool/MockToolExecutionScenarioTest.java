package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MockToolExecutionScenarioTest {

    @Test
    void testMultiStepScenario() {
        MockToolExecutionScenario scenario = new MockToolExecutionScenario(
            "test_scenario", 
            "Test multi-step scenario"
        )
        .addParameter(new ToolParameter("input", "string", "Test input", true))
        .addStep(params -> {
            String input = (String) params.get("input");
            return input != null ? 
                ToolResult.success(Map.of("step", 1, "input", input), "Step 1 completed") :
                ToolResult.failure("Input required");
        })
        .addStep(params -> ToolResult.success(Map.of("step", 2), "Step 2 completed"));

        // Test first step
        Map<String, Object> params = new HashMap<>();
        params.put("input", "test");
        ToolResult result1 = scenario.execute(params);
        assertTrue(result1.success());
        assertEquals(1, result1.data().get("step"));
        
        // Test second step
        ToolResult result2 = scenario.execute(params);
        assertTrue(result2.success());
        assertEquals(2, result2.data().get("step"));
        
        // Test completion
        ToolResult result3 = scenario.execute(params);
        assertFalse(result3.success());
        assertTrue(result3.message().contains("completed"));
    }

    @Test
    void testScenarioReset() {
        MockToolExecutionScenario scenario = new MockToolExecutionScenario(
            "reset_test", 
            "Test scenario reset"
        )
        .addStep(params -> ToolResult.success(Map.of("step", 1), "Step 1"));

        // Execute once
        scenario.execute(new HashMap<>());
        
        // Should fail - no more steps
        ToolResult result = scenario.execute(new HashMap<>());
        assertFalse(result.success());
        
        // Reset and try again
        scenario.reset();
        result = scenario.execute(new HashMap<>());
        assertTrue(result.success());
        assertEquals(1, result.data().get("step"));
    }
}

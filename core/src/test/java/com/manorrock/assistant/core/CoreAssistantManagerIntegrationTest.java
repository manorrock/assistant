package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.api.AssistantResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoreAssistantManagerIntegrationTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "OLLAMA_HOST", matches = ".+")
    void testRegisterAndExecuteCoreAssistant() {
        CoreAssistantManager manager = new CoreAssistantManager();
        CoreAssistant coreAssistant = new CoreAssistant();
        manager.registerAssistant(coreAssistant);
        assertTrue(manager.findAssistant(coreAssistant.getClass().getSimpleName()).isPresent(), "CoreAssistant should be registered");

        Map<String, Object> params = new HashMap<>();
        params.put("message", new CoreAssistantMessage("Hello Manorrock!"));
        AssistantResult result = manager.executeAssistant(coreAssistant.getClass().getSimpleName(), params);
        assertTrue(result.isSuccess(), "Execution should succeed");
        assertNotNull(result.getData().get("response"), "Response should not be null");
        AssistantMessage response = (AssistantMessage) result.getData().get("response");
        assertNotNull(response.getContent(), "Response content should not be null");
        assertFalse(response.getContent().isEmpty(), "Response content should not be empty");
    }

    @Test
    void testDisableEnableCoreAssistant() {
        CoreAssistantManager manager = new CoreAssistantManager();
        CoreAssistant coreAssistant = new CoreAssistant();
        manager.registerAssistant(coreAssistant);
        String assistantName = coreAssistant.getClass().getSimpleName();
        assertTrue(manager.disableAssistant(assistantName), "Should be able to disable");
        Map<String, Object> params = new HashMap<>();
        params.put("message", new CoreAssistantMessage("Test"));
        AssistantResult result = manager.executeAssistant(assistantName, params);
        assertFalse(result.isSuccess(), "Execution should fail when disabled");
        assertTrue(result.getMessage().contains("disabled"), "Error message should mention disabled");
        assertTrue(manager.enableAssistant(assistantName), "Should be able to enable");
        result = manager.executeAssistant(assistantName, params);
        assertTrue(result.isSuccess(), "Execution should succeed when enabled");
    }
}

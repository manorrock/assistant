package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.api.AssistantResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CoreAssistantManagerTest {
    static class DummyAssistant implements Assistant {
        private final String name;
        private final String description;
        private String activeLlm;
        public DummyAssistant(String name, String description) {
            this.name = name;
            this.description = description;
        }
        public String getName() { return name; }
        public String getDescription() { return description; }
        @Override
        public java.util.concurrent.CompletableFuture<com.manorrock.assistant.api.AssistantMessage> sendMessage(com.manorrock.assistant.api.AssistantMessage message) {
            return java.util.concurrent.CompletableFuture.completedFuture(new CoreAssistantMessage("Echo: " + message.getContent()));
        }
        @Override
        public com.manorrock.assistant.api.AssistantMessage processMessage(com.manorrock.assistant.api.AssistantMessage message) {
            return new CoreAssistantMessage("Echo: " + message.getContent());
        }
        @Override
        public com.manorrock.assistant.api.CommandRegistry getCommandRegistry() { return null; }
        @Override
        public com.manorrock.assistant.api.LlmManager getLlmManager() { return null; }
        @Override
        public com.manorrock.assistant.api.ToolManager getToolManager() { return null; }
        @Override
        public String getActiveLlm() { return activeLlm; }
        @Override
        public void setActiveLlm(String activeLlm) { this.activeLlm = activeLlm; }
    }

    @Test
    void testRegisterAndFindAssistant() {
        CoreAssistantManager manager = new CoreAssistantManager();
        DummyAssistant assistant = new DummyAssistant("test", "desc");
        manager.registerAssistant(assistant);
        Optional<Assistant> found = manager.findAssistant("test");
        assertTrue(found.isPresent());
        assertEquals("test", ((DummyAssistant) found.get()).getName());
    }

    @Test
    void testUnregisterAssistant() {
        CoreAssistantManager manager = new CoreAssistantManager();
        DummyAssistant assistant = new DummyAssistant("test", "desc");
        manager.registerAssistant(assistant);
        assertTrue(manager.unregisterAssistant("test"));
        assertFalse(manager.findAssistant("test").isPresent());
    }

    @Test
    void testEnableDisableAssistant() {
        CoreAssistantManager manager = new CoreAssistantManager();
        DummyAssistant assistant = new DummyAssistant("test", "desc");
        manager.registerAssistant(assistant);
        assertTrue(manager.disableAssistant("test"));
        assertTrue(manager.getActiveAssistants().isEmpty());
        assertTrue(manager.enableAssistant("test"));
        assertFalse(manager.getActiveAssistants().isEmpty());
    }

    @Test
    void testExecuteAssistant() {
        CoreAssistantManager manager = new CoreAssistantManager();
        DummyAssistant assistant = new DummyAssistant("test", "desc");
        manager.registerAssistant(assistant);
        Map<String, Object> params = new HashMap<>();
    params.put("message", new CoreAssistantMessage("Hello"));
        AssistantResult result = manager.executeAssistant("test", params);
        assertTrue(result.isSuccess());
        assertTrue(result.getData().get("response") instanceof AssistantMessage);
        assertEquals("Echo: Hello", ((AssistantMessage) result.getData().get("response")).getContent());
    }

    @Test
    void testExecuteDisabledAssistant() {
        CoreAssistantManager manager = new CoreAssistantManager();
        DummyAssistant assistant = new DummyAssistant("test", "desc");
        manager.registerAssistant(assistant);
        manager.disableAssistant("test");
        Map<String, Object> params = new HashMap<>();
    params.put("message", new CoreAssistantMessage("Hello"));
        AssistantResult result = manager.executeAssistant("test", params);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("disabled"));
    }
}

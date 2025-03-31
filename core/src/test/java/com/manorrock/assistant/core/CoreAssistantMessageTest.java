package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class CoreAssistantMessageTest {

    @Test
    public void testDefaultConstructor() {
        CoreAssistantMessage message = new CoreAssistantMessage();
        assertEquals("No message", message.getContent());
    }

    @Test
    public void testParameterizedConstructor() {
        CoreAssistantMessage message = new CoreAssistantMessage("Hello, World!");
        assertEquals("Hello, World!", message.getContent());
    }

    @Test
    public void testSetContent() {
        CoreAssistantMessage message = new CoreAssistantMessage();
        message.setContent("New Content");
        assertEquals("New Content", message.getContent());
    }
}
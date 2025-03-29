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

    @Test
    public void testGetTypeThrowsException() {
        CoreAssistantMessage message = new CoreAssistantMessage();
        assertThrows(UnsupportedOperationException.class, message::getType);
    }

    @Test
    public void testSetTypeThrowsException() {
        CoreAssistantMessage message = new CoreAssistantMessage();
        assertThrows(UnsupportedOperationException.class, () -> message.setType("type"));
    }

    @Test
    public void testGetTimestampThrowsException() {
        CoreAssistantMessage message = new CoreAssistantMessage();
        assertThrows(UnsupportedOperationException.class, message::getTimestamp);
    }

    @Test
    public void testSetTimestampThrowsException() {
        CoreAssistantMessage message = new CoreAssistantMessage();
        assertThrows(UnsupportedOperationException.class, () -> message.setTimestamp(null));
    }

    @Test
    public void testSetIdThrowsException() {
        CoreAssistantMessage message = new CoreAssistantMessage();
        assertThrows(UnsupportedOperationException.class, () -> message.setId("id"));
    }
}
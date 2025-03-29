package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CoreCommandRegistryTest {

    private CoreCommandRegistry registry;

    @BeforeEach
    void setUp() {
        CoreAssistant coreAssistant = new CoreAssistant();
        registry = new CoreCommandRegistry(coreAssistant);
    }

    @Test
    void testRegisterAndGetCommand() {
        Command mockCommand = mock(Command.class);
        registry.registerCommand("testCommand", mockCommand);

        Command retrievedCommand = registry.getCommand("testCommand");
        assertNotNull(retrievedCommand);
        assertEquals(mockCommand, retrievedCommand);
    }

    @Test
    void testGetCommandWithType() {
        Command mockCommand = mock(Command.class);
        registry.registerCommand("testCommand", mockCommand);

        Command retrievedCommand = registry.getCommand("testCommand", Command.class);
        assertNotNull(retrievedCommand);
        assertEquals(mockCommand, retrievedCommand);
    }

    @Test
    void testHasCommand() {
        Command mockCommand = mock(Command.class);
        registry.registerCommand("testCommand", mockCommand);

        assertTrue(registry.hasCommand("testCommand"));
        assertFalse(registry.hasCommand("nonExistentCommand"));
    }

    @Test
    void testGetCommandNames() {
        Command mockCommand1 = mock(Command.class);
        Command mockCommand2 = mock(Command.class);
        registry.registerCommand("command1", mockCommand1);
        registry.registerCommand("command2", mockCommand2);

        Set<String> commandNames = registry.getCommandNames();
        assertEquals(4, commandNames.size());
        assertTrue(commandNames.contains("command1"));
        assertTrue(commandNames.contains("command2"));
    }

    @Test
    void testUnregisterCommand() {
        Command mockCommand = mock(Command.class);
        registry.registerCommand("testCommand", mockCommand);

        Command unregisteredCommand = registry.unregisterCommand("testCommand");
        assertNotNull(unregisteredCommand);
        assertEquals(mockCommand, unregisteredCommand);
        assertFalse(registry.hasCommand("testCommand"));
    }

    @Test
    void testClearCommands() {
        Command mockCommand1 = mock(Command.class);
        Command mockCommand2 = mock(Command.class);
        registry.registerCommand("command1", mockCommand1);
        registry.registerCommand("command2", mockCommand2);

        registry.clearCommands();
        assertTrue(registry.getCommandNames().isEmpty());
    }
}
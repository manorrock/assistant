package com.manorrock.assistant.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.CommandRegistry;

public class CoreHelpCommandTest {

    private CoreAssistant mockAssistant;
    private CoreHelpCommand helpCommand;
    private CommandRegistry mockRegistry;

    @BeforeEach
    public void setUp() {
        mockAssistant = mock(CoreAssistant.class);
        mockRegistry = mock(CommandRegistry.class);
        when(mockAssistant.getCommandRegistry()).thenReturn(mockRegistry);
        helpCommand = new CoreHelpCommand(mockAssistant);
    }

    @Test
    public void testExecuteWithNoCommands() {
        // Arrange
        when(mockRegistry.getCommandNames()).thenReturn(Collections.emptySet());

        // Act
        String result = helpCommand.execute("");

        // Assert
        assertEquals("Manorrock Assistant \n\nAvailable commands:\n", result);
    }

    @Test
    public void testExecuteWithCommands() {
        // Arrange
        Set<String> commandNames = new HashSet<>();
        commandNames.add("cmd1");
        commandNames.add("cmd2");
        when(mockRegistry.getCommandNames()).thenReturn(commandNames);

        Command mockCmd1 = mock(Command.class);
        when(mockRegistry.getCommand("cmd1")).thenReturn(mockCmd1);
        when(mockCmd1.getShortDescription()).thenReturn("Description for cmd1");

        Command mockCmd2 = mock(Command.class);
        when(mockRegistry.getCommand("cmd2")).thenReturn(mockCmd2);
        when(mockCmd2.getShortDescription()).thenReturn("Description for cmd2");

        // Act
        String result = helpCommand.execute("");

        // Assert
        // Since Set doesn't guarantee order, we need to check for both possible orderings
        String expected1 = "Manorrock Assistant \n\nAvailable commands:\n/cmd1 - Description for cmd1\n/cmd2 - Description for cmd2\n";
        String expected2 = "Manorrock Assistant \n\nAvailable commands:\n/cmd2 - Description for cmd2\n/cmd1 - Description for cmd1\n";
        assertTrue(result.equals(expected1) || result.equals(expected2),
                "Result should match one of the expected outputs");
    }

    @Test
    public void testGetDescription() {
        // Arrange & Act
        String result = helpCommand.getDescription();

        // Assert
        assertEquals("Show available commands", result);
    }

    @Test
    public void testGetShortDescription() {
        // Arrange & Act
        String result = helpCommand.getShortDescription();

        // Assert
        assertEquals("Show available commands", result);
    }
}
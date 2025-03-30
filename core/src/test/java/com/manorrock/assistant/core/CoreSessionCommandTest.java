package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the CoreSessionCommand class.
 */
public class CoreSessionCommandTest {

    private CoreAssistant mockAssistant;
    private CoreSessionCommand command;

    @BeforeEach
    public void setUp() {
        mockAssistant = mock(CoreAssistant.class);
        command = new CoreSessionCommand(mockAssistant);
    }

    @Test
    public void testExecuteToString_withNewSubcommand() {
        String result = command.executeToString("new");
        assertEquals("Started new chat session", result);
        verify(mockAssistant).reset();
    }

    @Test
    public void testExecuteToString_withHelpSubcommand() {
        String result = command.executeToString("help");
        assertEquals("""
               Session management commands.
               
               Usage:
                 /session             - Show session command help
                 /session new         - Start a new session (clear history)
               """, result);
    }

    @Test
    public void testExecuteToString_withEmptyInput() {
        String result = command.executeToString("");
        assertEquals("""
               Session management commands.
               
               Usage:
                 /session             - Show session command help
                 /session new         - Start a new session (clear history)
               """, result);
    }

    @Test
    public void testExecuteToString_withNullInput() {
        String result = command.executeToString(null);
        assertEquals("""
               Session management commands.
               
               Usage:
                 /session             - Show session command help
                 /session new         - Start a new session (clear history)
               """, result);
    }

    @Test
    public void testExecuteToStream() throws Exception {
        String input = "new";
        String expected = "Started new chat session";
        assertEquals(expected, new String(command.executeToStream(input).readAllBytes()));
    }

    @Test
    public void testGetDescription() {
        assertEquals("Manages chat sessions including creating new ones, viewing history, etc.", command.getDescription());
    }

    @Test
    public void testGetShortDescription() {
        assertEquals("Manages chat sessions", command.getShortDescription());
    }
}
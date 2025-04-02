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
    public void testExecute_withNewSubcommand() {
        String result = command.execute("new");
        assertEquals("Started new chat session", result);
        verify(mockAssistant).reset();
    }

    @Test
    public void testExecute_withHelpSubcommand() {
        String result = command.execute("help");
        assertEquals("""
               Session management commands.
               
               Usage:
                 /session             - Show session command help
                 /session new         - Start a new session (clear history)
               """, result);
    }

    @Test
    public void testExecute_withEmptyInput() {
        String result = command.execute("");
        assertEquals("""
               Session management commands.
               
               Usage:
                 /session             - Show session command help
                 /session new         - Start a new session (clear history)
               """, result);
    }

    @Test
    public void testExecute_withNullInput() {
        String result = command.execute(null);
        assertEquals("""
               Session management commands.
               
               Usage:
                 /session             - Show session command help
                 /session new         - Start a new session (clear history)
               """, result);
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
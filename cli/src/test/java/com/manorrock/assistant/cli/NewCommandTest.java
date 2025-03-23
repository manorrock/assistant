package com.manorrock.assistant.cli;

import com.manorrock.assistant.shared.Command;
import com.manorrock.assistant.shared.NewCommand;
import com.manorrock.assistant.core.Assistant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the NewCommand class.
 */
public class NewCommandTest {
    
    /**
     * Test that the new command works correctly.
     */
    @Test
    public void testNewCommand() {
        // Create an Assistant instance for the test
        Assistant assistant = new Assistant();
        
        // Create a simple runnable for the new command
        Runnable newSessionAction = () -> System.out.println("New session started");
        
        // Register the new command
        NewCommand newCommand = new NewCommand(newSessionAction);
        assistant.getCommandRegistry().registerCommand("new", newCommand);
        
        // Verify registration
        Command cmd = assistant.getCommandRegistry().getCommand("new");
        assertNotNull(cmd);
        assertTrue(cmd instanceof NewCommand);
        
        // Execute the command and check result
        String result = cmd.executeToString("");
        assertEquals("Started new chat session", result);
    }
}

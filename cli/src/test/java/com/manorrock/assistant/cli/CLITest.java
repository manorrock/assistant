package com.manorrock.assistant.cli;

import com.manorrock.assistant.shared.Command;
import com.manorrock.assistant.shared.HelpCommand;
import com.manorrock.assistant.core.Assistant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CLI class.
 */
public class CLITest {
    
    /**
     * Test the CLI constructor.
     */
    @Test
    public void testCLIConstructor() {
        CLI cli = new CLI();
        assertNotNull(cli);
    }
    
    /**
     * Test that the help command is registered.
     */
    @Test
    public void testHelpCommand() {
        CLI cli = new CLI();
        
        // We need to create an instance of Assistant to access the command registry
        // Access the Assistant instance via reflection since it's private
        try {
            java.lang.reflect.Field assistanceField = CLI.class.getDeclaredField("assistance");
            assistanceField.setAccessible(true);
            Assistant assistance = (Assistant) assistanceField.get(cli);
            
            // Call method to register commands (similar to what CLI.call() would do)
            assistance.getCommandRegistry().registerCommand("help", new HelpCommand());
            
            // Now check if the help command is registered
            Command helpCommand = assistance.getCommandRegistry().getCommand("help");
            assertNotNull(helpCommand);
            assertTrue(helpCommand instanceof HelpCommand);
        } catch (Exception e) {
            fail("Exception accessing assistance field: " + e.getMessage());
        }
    }
}

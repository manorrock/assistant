package com.manorrock.assistant.cli;



import dev.langchain4j.data.message.ChatMessage;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.command.HelpCommand;
import com.manorrock.assistant.core.Assistant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.net.URL;
import java.util.LinkedList;

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
    public void testHelpCommand() throws Exception {
        CLI cli = new CLI();
        // Initialize CLI first
        cli.call();
        
        java.lang.reflect.Field assistanceField = CLI.class.getDeclaredField("assistance");
        assistanceField.setAccessible(true);
        Assistant assistance = (Assistant) assistanceField.get(cli);
        
        Command helpCommand = assistance.getCommandRegistry().getCommand("help");
        assertNotNull(helpCommand);
        assertTrue(helpCommand instanceof HelpCommand);
    }

    /**
     * Test command registration and verification.
     */
    @Test
    public void testCommandRegistration() throws Exception {
        CLI cli = new CLI();
        // Initialize CLI first
        cli.call();
        
        java.lang.reflect.Field assistanceField = CLI.class.getDeclaredField("assistance");
        assistanceField.setAccessible(true);
        Assistant assistance = (Assistant) assistanceField.get(cli);
        
        // Verify core commands are registered
        assertTrue(assistance.getCommandRegistry().hasCommand("source"),
            "source command should be registered");
        assertTrue(assistance.getCommandRegistry().hasCommand("help"),
            "help command should be registered");
        assertTrue(assistance.getCommandRegistry().hasCommand("new"),
            "new command should be registered");
    }

    /**
     * Test source command with non-existent file.
     */
    @Test
    public void testSourceCommandWithNonExistentFile() throws Exception {
        CLI cli = new CLI();
        cli.call();
        
        java.lang.reflect.Field assistanceField = CLI.class.getDeclaredField("assistance");
        assistanceField.setAccessible(true);
        Assistant assistance = (Assistant) assistanceField.get(cli);
        
        Command sourceCommand = assistance.getCommandRegistry().getCommand("source");
        String response = sourceCommand.executeToString("/source nonexistent.file");
        
        assertTrue(response.contains("Error reading source file"),
            "Response should contain error message");
    }
}

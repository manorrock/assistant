package com.manorrock.assistant.cli;

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
}

/*
 * Copyright (c) 2002-2025, Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright 
 *        notice, this list of conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CommandRegistryImpl class.
 */
public class CommandRegistryImplTest {

    /**
     * The command registry to be tested.
     */
    private CommandRegistryImpl registry;
    
    /**
     * A mock command for testing.
     */
    private Command mockCommand;
    
    /**
     * A specialized command for type testing.
     */
    private SpecialCommand specialCommand;

    /**
     * Set up before each test.
     */
    @BeforeEach
    public void setUp() {
        registry = new CommandRegistryImpl();
        mockCommand = new MockCommand();
        specialCommand = new SpecialCommand();
    }

    /**
     * Test for registerCommand and hasCommand methods.
     */
    @Test
    public void testRegisterAndHasCommand() {
        // Test registering a valid command
        registry.registerCommand("test", mockCommand);
        assertTrue(registry.hasCommand("test"));
        
        // Test checking for non-existent command
        assertFalse(registry.hasCommand("nonexistent"));
        
        // Test checking with null name
        assertFalse(registry.hasCommand(null));
    }

    /**
     * Test for registerCommand with invalid parameters.
     */
    @Test
    public void testRegisterCommandWithInvalidParams() {
        // Test registering with null name
        assertThrows(IllegalArgumentException.class, () -> registry.registerCommand(null, mockCommand));
        
        // Test registering with empty name
        assertThrows(IllegalArgumentException.class, () -> registry.registerCommand("", mockCommand));
        assertThrows(IllegalArgumentException.class, () -> registry.registerCommand("   ", mockCommand));
        
        // Test registering with null command
        assertThrows(IllegalArgumentException.class, () -> registry.registerCommand("test", null));
    }

    /**
     * Test for getCommand method.
     */
    @Test
    public void testGetCommand() {
        // Register a command
        registry.registerCommand("test", mockCommand);
        
        // Test retrieving an existing command
        Command retrieved = registry.getCommand("test");
        assertSame(mockCommand, retrieved);
        
        // Test retrieving a non-existent command
        assertNull(registry.getCommand("nonexistent"));
        
        // Test retrieving with null name
        assertNull(registry.getCommand(null));
    }

    /**
     * Test for typed getCommand method.
     */
    @Test
    public void testGetCommandWithType() {
        // Register commands with different types
        registry.registerCommand("mock", mockCommand);
        registry.registerCommand("special", specialCommand);
        
        // Test retrieving with correct type
        SpecialCommand retrievedSpecial = registry.getCommand("special", SpecialCommand.class);
        assertSame(specialCommand, retrievedSpecial);
        
        // Test retrieving with incompatible type
        SpecialCommand shouldBeNull = registry.getCommand("mock", SpecialCommand.class);
        assertNull(shouldBeNull);
        
        // Test retrieving with correct supertype
        Command retrievedCommand = registry.getCommand("special", Command.class);
        assertSame(specialCommand, retrievedCommand);
        
        // Test retrieving non-existent command with type
        assertNull(registry.getCommand("nonexistent", Command.class));
    }

    /**
     * Test for getCommandNames method.
     */
    @Test
    public void testGetCommandNames() {
        // Test with empty registry
        Set<String> emptyNames = registry.getCommandNames();
        assertTrue(emptyNames.isEmpty());
        
        // Add some commands
        registry.registerCommand("test1", mockCommand);
        registry.registerCommand("test2", specialCommand);
        
        // Test with populated registry
        Set<String> names = registry.getCommandNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("test1"));
        assertTrue(names.contains("test2"));
        
        // Verify that the returned set is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> names.add("newCommand"));
    }

    /**
     * Test for unregisterCommand method.
     */
    @Test
    public void testUnregisterCommand() {
        // Register a command
        registry.registerCommand("test", mockCommand);
        assertTrue(registry.hasCommand("test"));
        
        // Unregister the command
        Command unregistered = registry.unregisterCommand("test");
        assertSame(mockCommand, unregistered);
        assertFalse(registry.hasCommand("test"));
        
        // Test unregistering a non-existent command
        assertNull(registry.unregisterCommand("nonexistent"));
        
        // Test unregistering with null name
        assertNull(registry.unregisterCommand(null));
    }

    /**
     * Test for clearCommands method.
     */
    @Test
    public void testClearCommands() {
        // Register multiple commands
        registry.registerCommand("test1", mockCommand);
        registry.registerCommand("test2", specialCommand);
        assertEquals(2, registry.getCommandNames().size());
        
        // Clear all commands
        registry.clearCommands();
        
        // Verify all commands are cleared
        assertTrue(registry.getCommandNames().isEmpty());
        assertFalse(registry.hasCommand("test1"));
        assertFalse(registry.hasCommand("test2"));
    }
    
    /**
     * A mock implementation of Command for testing.
     */
    private static class MockCommand implements Command {
        @Override
        public String execute(String input) {
            return executeToString(input);
        }
        
        @Override
        public String executeToString(String input) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'executeToString'");
        }
        
        @Override
        public InputStream executeToStream(String input) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'executeToStream'");
        }
        
        @Override
        public String getDescription() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getDescription'");
        }
        // Mock implementation for testing
    }
    
    /**
     * A specialized Command implementation for type testing.
     */
    private static class SpecialCommand implements Command {
        @Override
        public String execute(String input) {
            return executeToString(input);
        }
        
        @Override
        public String executeToString(String input) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'executeToString'");
        }

        @Override
        public InputStream executeToStream(String input) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'executeToStream'");
        }

        @Override
        public String getDescription() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getDescription'");
        }
        // Specialized implementation for testing
    }
}

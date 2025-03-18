package com.manorrock.assistant.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manorrock.assistant.shared.CommandRegistry;
import com.manorrock.assistant.shared.NewCommand;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewCommandTest {

  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private CLI cli;

  @BeforeEach
  void setUp() {
    System.setOut(new PrintStream(outputStream));
    cli = new CLI();
    // Clear any existing commands to start fresh
    CommandRegistry.getInstance().clearCommands();
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
    // Clear commands after test
    CommandRegistry.getInstance().clearCommands();
    outputStream.reset();
  }

  @Test
  void testNewCommand() throws Exception {
    // Ensure CLI is initialized
    cli.call();

    // Add some history
    cli.handleSendAction("Test message");
    assertTrue(outputStream.toString().contains("Test message"));
    outputStream.reset();

    // Execute new command
    cli.handleSendAction("/new");

    // Verify the response
    String output = outputStream.toString();
    assertTrue(output.contains("Started new chat session"));

    // Add another message and verify history was cleared
    outputStream.reset();
    cli.handleSendAction("Another test");
    output = outputStream.toString();
    assertFalse(output.contains("Test message"));
    assertTrue(output.contains("Another test"));
  }
}

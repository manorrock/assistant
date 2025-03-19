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

  @Test
  void testNewCommand() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(outputStream));
    CLI cli = new CLI();

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

    System.setOut(originalOut);
    outputStream.close();
  }

  // Removed testNewCommandWithExport test as export functionality is no longer needed
}

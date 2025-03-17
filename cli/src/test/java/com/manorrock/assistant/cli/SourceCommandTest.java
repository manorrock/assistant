package com.manorrock.assistant.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class SourceCommandTest {

  private final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  @BeforeEach
  void setUp() {
    System.setOut(new PrintStream(outputCapture));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void testSourceCommand() throws Exception {
    // Use existing help.prompt file
    Path promptFile = Path.of("src/test/prompts/help.prompt");

    // Initialize CLI and execute source command
    CLI cli = new CLI();
    cli.call(); // Initialize the CLI first

    cli.handleCommand("/source " + promptFile);
    String output = outputCapture.toString();

    // Verify the help output contains expected sections
    assertTrue(output.contains("CLI-specific commands:"));
    assertTrue(output.contains("/llmEndpoint"));
    assertTrue(output.contains("/llmModel"));
    assertTrue(output.contains("Successfully executed commands from"));
  }

  @Test
  void testSourceCommandWithNonExistentFile() throws Exception {
    CLI cli = new CLI();
    cli.call(); // Initialize the CLI first

    cli.handleCommand("/source nonexistent.file");
    String output = outputCapture.toString();

    assertTrue(output.contains("Error reading source file"));
  }
}

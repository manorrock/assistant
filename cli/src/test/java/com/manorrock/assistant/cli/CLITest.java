package com.manorrock.assistant.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

class CLITest {
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  @BeforeEach
  void setUp() {
    System.setOut(new PrintStream(outputStream));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void testHelpCommand() {
    CLI cli = new CLI();
    cli.handleCommand("/help");

    String output = outputStream.toString();

    // Verify CLI-specific commands
    assertTrue(output.contains("CLI-specific commands:"));
    assertTrue(output.contains("/llmEndpoint"));
    assertTrue(output.contains("/llmModel"));
    assertTrue(output.contains("/llmVendor"));
    assertTrue(output.contains("/llmApiKey"));
    assertTrue(output.contains("/llmTemperature"));
    assertTrue(output.contains("/source"));
    assertTrue(output.contains("/explain"));
    assertTrue(output.contains("/exit"));

    // Verify command descriptions
    assertTrue(output.contains("Change the endpoint"));
    assertTrue(output.contains("Change the model"));
    assertTrue(output.contains("Change the vendor"));
  }

  @Test
  void testModelCommand() throws Exception {
    CLI cli = new CLI();
    // Initialize CLI - call() sets up the config
    cli.call();

    // Test changing model
    cli.handleCommand("/llmModel gpt4");
    String output = outputStream.toString();
    assertTrue(output.contains("Model changed to gpt4"));
    outputStream.reset();

    // Verify model was changed by getting current model again
    cli.handleCommand("/llmModel");
    output = outputStream.toString();
    assertTrue(output.contains("Current model: gpt4"));
  }
}

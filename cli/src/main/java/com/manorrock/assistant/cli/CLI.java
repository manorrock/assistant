package com.manorrock.assistant.cli;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.core.CoreAssistant;
import com.manorrock.assistant.core.CoreAssistantMessage;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * The CLI for the Manorrock Assistant.
 * 
 * <p>
 * This class provides a command-line interface for interacting with the
 * Manorrock Assistant. It allows users to send messages, receive responses,
 * and manage configurations.
 * </p>
 * 
 * @author Manfred Riem (mriem@manorrock.com)
 */
@Command(name = "assistant-cli", mixinStandardHelpOptions = true, versionProvider = CLI.PropertiesVersionProvider.class, description = "CLI version of the Manorrock Assistant")
public class CLI implements Callable<Integer> {

  /**
   * Stores the logger.
   */
  private static final Logger LOGGER = Logger.getLogger(CLI.class.getName());

  /**
   * Stores the assistant.
   */
  private CoreAssistant coreAssistant;

  /**
   * Stores the command line flag that specifieds to read the message from stdin.
   */
  @Option(names = { "--stdin" }, description = "Read message from standard input")
  private boolean readFromStdin = false;

  /**
   * Stores the command line flag to start in interactive mode.
   */
  @Option(names = { "-i", "--interactive" }, description = "Start in interactive mode")
  private boolean interactive = false;

  /**
   * Stores the command line flag to not show the banner.
   */
  @Option(names = { "--no-banner" }, description = "Do not show the banner")
  private boolean noBanner = false;

  /**
   * Stores the command line flag to disable the You/Assistant: prefix.
   */
  @Option(names = { "--no-prefix" }, description = "Do not show You/Assistant: prefix")
  private boolean noPrefix = false;

  /**
   * Stores the command line flag to enable debug logging.
   */
  @Option(names = { "--debug" }, description = "Enable debug logging")
  private boolean debug = false;

  /**
   * Stores the message to send.
   */
  @Parameters(paramLabel = "MESSAGE", description = "Message to send", arity = "0..1")
  private String message;

  /**
   * Constructor.
   */
  public CLI() {
    coreAssistant = new CoreAssistant();
    coreAssistant.setActiveLlm("llama3.2");
    coreAssistant.getCommandRegistry()
        .registerCommand("explain", new CLIExplainCommand(coreAssistant));
    coreAssistant.getCommandRegistry()
        .registerCommand("template", new CLISystemMessageCommand(coreAssistant));
    coreAssistant.getCommandRegistry()
        .registerCommand("issue-template", new CLIIssueTemplateCommand(coreAssistant));
    coreAssistant.getCommandRegistry()
        .registerCommand("implement-issue", new CLIImplementIssueCommand(coreAssistant));
  }

  /**
   * Protected accessor method to allow subclasses to access the CoreAssistant instance.
   * This supports extension by enterprise or specialized CLI implementations.
   * 
   * @return The CoreAssistant instance
   */
  protected CoreAssistant getCoreAssistant() {
    return coreAssistant;
  }

  /**
   * Main method to run the CLI.
   * 
   * @param args the command line arguments.
   */
  public static void main(String[] args) {
    // Configure default logging to only show warnings and errors
    configureLogging(Level.WARNING);

    int exitCode = new CommandLine(new CLI()).execute(args);
    System.exit(exitCode);
  }

  /**
   * Configure logging with the specified level.
   *
   * @param level The logging level to set
   */
  private static void configureLogging(Level level) {
    // Set root logger level
    Logger rootLogger = LogManager.getLogManager().getLogger("");
    rootLogger.setLevel(level);

    // Set console handler level
    for (Handler handler : rootLogger.getHandlers()) {
      if (handler instanceof ConsoleHandler) {
        handler.setLevel(level);
      }
    }
  }

  @Override
  public Integer call() throws Exception {
    // Set debug mode if requested
    if (debug) {
      configureLogging(Level.INFO);
      LOGGER.info("Debug logging enabled");
    }

    if (interactive) {
      // start interactive mode
      startInteractiveMode();
    } else {
      if (readFromStdin) {
        // read the entire message from stdin
        message = new String(System.in.readAllBytes()).trim();
      } else if (!interactive && message == null) {
        // no message provided, show help
        message = "/help";
      }
      // process the message
      handleSendAction(message);
    }

    return 0;
  }

  protected void handleSendAction(String userMessage) {
    if (!userMessage.isEmpty()) {
      // Create the message for CoreAssistant
      AssistantMessage message = new CoreAssistantMessage(userMessage);

      // Process using the CoreAssistant
      AssistantMessage response = coreAssistant.processMessage(message);

      // Display the response
      if (!noPrefix) {
        System.out.print("Assistant: ");
      }
      System.out.println(response.getContent());
    }
  }

  private void startInteractiveMode() {
    if (!noBanner) {
      System.out.println("Entering interactive mode. Type /exit to quit, or /help for commands.");
      System.out.println("Use \\ at end of line for multi-line input.");
    }
    try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
      StringBuilder messageBuilder = new StringBuilder();
      while (true) {
        if (!noPrefix) {
          System.out.print(messageBuilder.length() == 0 ? "\nYou: " : "... ");
        }
        String line = scanner.nextLine();
        String trimmedLine = line.trim();

        // Check for exit command
        if (trimmedLine.equals("/exit")) {
          System.out.println("Exiting interactive mode.");
          break;
        }

        // Handle line continuation
        if (line.endsWith("\\") || trimmedLine.endsWith("\\")) {
          // Remove the backslash and add the line with a newline
          messageBuilder.append(line.substring(0, line.lastIndexOf('\\')).stripTrailing()).append("\n");
          continue;
        }

        // Add the line to the message
        messageBuilder.append(line);

        // Process the complete message
        String fullMessage = messageBuilder.toString().trim();
        if (!fullMessage.isEmpty()) {
          handleSendAction(fullMessage);
        }
        messageBuilder.setLength(0);
      }
    }
  }

  static class PropertiesVersionProvider implements CommandLine.IVersionProvider {
    public String[] getVersion() throws Exception {
      Properties props = new Properties();
      try (InputStream is = getClass().getClassLoader().getResourceAsStream("version.properties")) {
        if (is != null) {
          props.load(is);
          String version = props.getProperty("version", "unknown");
          // Strip -SNAPSHOT if present
          return new String[] { version.replace("-SNAPSHOT", "") };
        }
      }
      return new String[] { "unknown" };
    }
  }
}

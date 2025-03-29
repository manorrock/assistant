package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Command implementation that provides help by listing all available commands.
 */
public class HelpCommand implements Command {

  @Override
  public String execute(String input) {
    return executeToString(input);
  }
  
  @Override
  public String executeToString(String input) {
    StringBuilder result = new StringBuilder();
    result.append("Available commands:\n");
    result.append("/clear - Clears the response window\n");
    result.append("/endpoint - Changes the LLM API endpoint (legacy, use /llmEndpoint instead)\n");
    result.append("/explain - Explains text from clipboard, selection, or file\n");
    result.append("/help - Displays available commands\n");
    result.append("/llm - Displays or configures LLM settings\n");
    result.append("/model - Changes the LLM model used (legacy, use /llmModel instead)\n");
    result.append("/new - Starts a new chat session\n");
    result.append("/source - Executes commands from a file\n");
    result.append("/tool - Manages tool integrations\n");
    return result.toString();
  }

  @Override
  public InputStream executeToStream(String input) {
    return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String getDescription() {
    return "Show available commands";
  }
}

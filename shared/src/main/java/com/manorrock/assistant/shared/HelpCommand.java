package com.manorrock.assistant.shared;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command implementation that provides help by listing all available commands.
 */
public class HelpCommand implements Command {

  @Override
  public String executeToString(String input) {
    StringBuilder result = new StringBuilder();
    result.append("Available commands:\n");
    result.append("/clear - Clear the response window\n");
    result.append("/explain - Explain text from clipboard or selection\n");
    result.append("/help - Show this help message\n");
    result.append("/llmEndpoint <host:port> - Change LLM API endpoint\n");
    result.append("/llmModel <name> - Change LLM model\n");
    result.append("/llmVendor <name> - Change LLM vendor\n");
    result.append("/model <name> - Legacy: Change model\n");
    result.append("/new - Start a new chat session\n");
    result.append("/source <file_path> - Execute commands from a file\n");
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

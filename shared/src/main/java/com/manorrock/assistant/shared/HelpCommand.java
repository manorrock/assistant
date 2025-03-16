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
    CommandRegistry registry = CommandRegistry.getInstance();
    StringBuilder help = new StringBuilder("Available commands:\n");

    registry.getCommandNames().stream().sorted().forEach(name -> {
      Command cmd = registry.getCommand(name);
      help.append("/").append(name).append(" - ").append(cmd.getDescription()).append("\n");
    });

    return help.toString();
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

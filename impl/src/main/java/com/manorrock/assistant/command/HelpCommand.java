package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.CommandRegistry;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Command implementation that provides help by listing all available commands.
 */
public class HelpCommand implements Command {

  private final CommandRegistry commandRegistry;

  /**
   * Constructor.
   * 
   * @param commandRegistry The registry containing all available commands
   */
  public HelpCommand(CommandRegistry commandRegistry) {
    this.commandRegistry = commandRegistry;
  }

  /**
   * Default constructor - for backward compatibility.
   * Using this constructor results in static command listing.
   */
  public HelpCommand() {
    this.commandRegistry = null;
  }

  @Override
  public String execute(String input) {
    return executeToString(input);
  }
  
  @Override
  public String executeToString(String input) {
    StringBuilder result = new StringBuilder();
    result.append("Available commands:\n");
    
    if (commandRegistry != null) {
      // If a command registry is available, use it to get all registered commands
      Set<String> commandNames = commandRegistry.getCommandNames();
      List<String> sortedNames = new ArrayList<>(commandNames);
      sortedNames.sort(String::compareTo); // Sort alphabetically
      
      for (String name : sortedNames) {
        Command cmd = commandRegistry.getCommand(name);
        result.append("/").append(name);
        
        String description = cmd.getDescription();
        if (description != null && !description.isEmpty()) {
          result.append(" - ").append(description);
        }
        
        result.append("\n");
      }
    } else {
      // Fallback to static list if no registry is provided
      result.append("/clear - Clears the response window\n");
      result.append("/explain - Explains text from clipboard, selection, or file\n");
      result.append("/help - Displays available commands\n");
      result.append("/llm - Displays or configures LLM settings\n");
      result.append("/new - Starts a new chat session\n");
      result.append("/source - Executes commands from a file\n");
      result.append("/tool - Manages tool integrations\n");
    }
    
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

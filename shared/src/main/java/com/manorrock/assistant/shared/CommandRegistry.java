package com.manorrock.assistant.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for managing and accessing Command instances by name.
 */
public class CommandRegistry {

  private static final CommandRegistry INSTANCE = new CommandRegistry();
  private final Map<String, Command> commands;

  private CommandRegistry() {
    commands = new HashMap<>();
    registerCommand("help", new HelpCommand());
  }

  /**
   * Get the singleton instance of the CommandRegistry.
   *
   * @return The CommandRegistry instance
   */
  public static CommandRegistry getInstance() {
    return INSTANCE;
  }

  /**
   * Register a command with the given name.
   *
   * @param name
   *          The name of the command
   * @param command
   *          The command implementation
   * @throws IllegalArgumentException
   *           if name is null or empty
   */
  public void registerCommand(String name, Command command) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Command name cannot be null or empty");
    }
    if (command == null) {
      throw new IllegalArgumentException("Command cannot be null");
    }
    commands.put(name, command);
  }

  /**
   * Get a command by name.
   *
   * @param name
   *          The name of the command
   * @return The command implementation, or null if not found
   */
  public Command getCommand(String name) {
    return name != null ? commands.get(name) : null;
  }

  /**
   * Get a command by name and required type.
   *
   * @param name
   *          The name of the command
   * @param type
   *          The required command type
   * @return The command implementation if it exists and matches the type, or null
   */
  public <T extends Command> T getCommand(String name, Class<T> type) {
    Command cmd = getCommand(name);
    return type.isInstance(cmd) ? type.cast(cmd) : null;
  }

  /**
   * Check if a command exists.
   *
   * @param name
   *          The name of the command
   * @return true if the command exists, false otherwise
   */
  public boolean hasCommand(String name) {
    return name != null && commands.containsKey(name);
  }

  /**
   * Get all registered command names.
   *
   * @return An unmodifiable set of command names
   */
  public Set<String> getCommandNames() {
    return Collections.unmodifiableSet(commands.keySet());
  }

  /**
   * Remove a command from the registry.
   *
   * @param name
   *          The name of the command to remove
   * @return The removed command, or null if not found
   */
  public Command unregisterCommand(String name) {
    return name != null ? commands.remove(name) : null;
  }

  /**
   * Clear all registered commands except the built-in help command.
   */
  public void clearCommands() {
    commands.clear();
    registerCommand("help", new HelpCommand());
  }
}

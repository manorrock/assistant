/*
 * Copyright (c) 2002-2025, Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright 
 *        notice, this list of conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.manorrock.assistant.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for managing and accessing Command instances by name.
 */
public class CommandRegistry {

  /**
   * Stores the registered commands.
   */
  private final Map<String, Command> commands;

  /*
   * Constructor to initialize the CommandRegistry.
   */
  public CommandRegistry() {
    commands = new HashMap<>();
  }

  /**
   * Register a command with the given name.
   *
   * @param name The name of the command
   * @param command The command implementation
   * @throws IllegalArgumentException if name is null or empty
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
   * @param name The name of the command
   * @return The command implementation, or null if not found
   */
  public Command getCommand(String name) {
    return name != null ? commands.get(name) : null;
  }

  /**
   * Get a command by name and required type.
   *
   * @param name The name of the command
   * @param type The required command type
   * @return The command implementation if it exists and matches the type, or null
   */
  public <T extends Command> T getCommand(String name, Class<T> type) {
    Command cmd = getCommand(name);
    return type.isInstance(cmd) ? type.cast(cmd) : null;
  }

  /**
   * Check if a command exists.
   *
   * @param name The name of the command
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
   * @param name The name of the command to remove
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
  }
}

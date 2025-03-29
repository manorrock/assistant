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
package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.CommandRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the CommandRegistry interface.
 */
public class CommandRegistryImpl implements CommandRegistry {

  /**
   * Stores the registered commands.
   */
  private final Map<String, Command> commands;

  /**
   * Constructor to initialize the CommandRegistry.
   */
  public CommandRegistryImpl() {
    commands = new HashMap<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
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
   * {@inheritDoc}
   */
  @Override
  public Command getCommand(String name) {
    return name != null ? commands.get(name) : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Command> T getCommand(String name, Class<T> type) {
    Command cmd = getCommand(name);
    return type.isInstance(cmd) ? type.cast(cmd) : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasCommand(String name) {
    return name != null && commands.containsKey(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getCommandNames() {
    return Collections.unmodifiableSet(commands.keySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Command unregisterCommand(String name) {
    return name != null ? commands.remove(name) : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearCommands() {
    commands.clear();
  }
}
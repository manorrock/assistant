package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.CommandRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

/**
 * The Core Command Registry.
 * 
 * <p>
 * This class implements the command registry functionality needed by all our
 * Assistant implementations.
 * </p>
 */
public class CoreCommandRegistry implements CommandRegistry {

    /**
     * Stores the command registry.
     */
    private Map<String, Command> commands = new HashMap<>();

    /**
     * Constructor.
     * 
     * @param assistant the core assistant instance.
     */
    public CoreCommandRegistry(CoreAssistant assistant) {

        // Register the default commands.
        registerCommand("llm", new CoreLlmCommand(assistant));
        registerCommand("help", new CoreHelpCommand(assistant));
    }

    /**
     * Get a command from the registry.
     * 
     * @param name the name of the command
     * @return the command
     */
    @Override
    public Command getCommand(String name) {
        return commands.get(name);
    }

    @Override
    public void registerCommand(String name, Command command) {
        commands.put(name, command);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Command> T getCommand(String name, Class<T> type) {
        Command command = commands.get(name);
        if (command != null && type.isAssignableFrom(command.getClass())) {
            return (T) command;
        }
        return null;
    }

    @Override
    public boolean hasCommand(String name) {
        return commands.containsKey(name);
    }

    @Override
    public Set<String> getCommandNames() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    @Override
    public Command unregisterCommand(String name) {
        return commands.remove(name);
    }

    @Override
    public void clearCommands() {
        commands.clear();
    }
}

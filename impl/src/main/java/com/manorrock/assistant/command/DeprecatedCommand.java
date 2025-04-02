package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;

/**
 * Command implementation for deprecated commands that provides a message indicating
 * the command is deprecated and suggesting the new command to use instead.
 */
public class DeprecatedCommand implements Command {

    private final String deprecatedCommandName;
    private final String newCommandName;
    private final Command delegateCommand;

    /**
     * Constructor for DeprecatedCommand with delegate command.
     *
     * @param deprecatedCommandName The name of the deprecated command
     * @param newCommandName The name of the new command to use instead
     * @param delegateCommand The command to delegate execution to (optional)
     */
    public DeprecatedCommand(String deprecatedCommandName, String newCommandName, Command delegateCommand) {
        this.deprecatedCommandName = deprecatedCommandName;
        this.newCommandName = newCommandName;
        this.delegateCommand = delegateCommand;
    }

    /**
     * Constructor for DeprecatedCommand without delegate command.
     *
     * @param deprecatedCommandName The name of the deprecated command
     * @param newCommandName The name of the new command to use instead
     */
    public DeprecatedCommand(String deprecatedCommandName, String newCommandName) {
        this(deprecatedCommandName, newCommandName, null);
    }

    @Override
    public String execute(String input) {
        StringBuilder result = new StringBuilder();
        result.append("WARNING: The command '/")
              .append(deprecatedCommandName)
              .append("' is deprecated. Please use '/")
              .append(newCommandName)
              .append("' instead.\n\n");
        
        if (delegateCommand != null) {
            // If a delegate command is provided, execute it and append its result
            result.append(delegateCommand.execute(input));
        }
        
        return result.toString();
    }

    @Override
    public String getDescription() {
        return "Deprecated: Use /" + newCommandName + " instead";
    }
    
    /**
     * Get the name of the new command that should be used instead.
     * 
     * @return The name of the new command
     */
    public String getNewCommandName() {
        return newCommandName;
    }
    
    /**
     * Get the name of the deprecated command.
     * 
     * @return The name of the deprecated command
     */
    public String getDeprecatedCommandName() {
        return deprecatedCommandName;
    }

    @Override
    public String getShortDescription() {
        return "Deprecated: Use /" + newCommandName + " instead";
    }
}

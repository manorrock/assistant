package com.manorrock.assistant.api;

/**
 * The Command interface is used to define a command that can be executed
 * with a given input string. 
 */
public interface Command {

  /**
   * Executes the command with the given input string and returns the result as a
   * string.
   * 
   * @param command the command to execute
   * @return the result of the command execution
   */
  String execute(String command);

  /**
   * Get a description of what this command does
   *
   * @return the command description
   */
  String getDescription();

  /**
   * Get a short description of what this command does.
   * 
   * @return the command short description.
   */
  String getShortDescription();
}

package com.manorrock.assistant.api;

import java.io.InputStream;

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
   * Executes the command with the given input string and returns the result as a
   * string.
   *
   * @deprecated Use {@link #execute(String)} instead. This method will be removed
   *             in the next monthly base version.
   * @param input The input string to process
   * @return The result of the command execution
   */
  @Deprecated
  String executeToString(String input);

  /**
   * Executes the command with the given input string and returns the result as an
   * InputStream. This
   * is the preferred method for handling potentially large data sets as it allows
   * for streaming
   * processing.
   *
   * @deprecated Use {@link #execute(String)} instead. This method will be removed
   *             in the next monthly base version.
   * @param input
   *              The input string to process
   * @return An InputStream containing the result of the command execution
   */
  @Deprecated
  InputStream executeToStream(String input);

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

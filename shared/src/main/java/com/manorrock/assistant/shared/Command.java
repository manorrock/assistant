package com.manorrock.assistant.shared;

import java.io.InputStream;

/**
 * Interface for executing commands that process input and produce output. This interface provides
 * two methods for command execution: a simple String-based approach and a streaming approach.
 */
public interface Command {

  /**
   * Executes the command with the given input string and returns the result as a string.
   *
   * @param input
   *          The input string to process
   * @return The result of the command execution
   */
  String executeToString(String input);

  /**
   * Executes the command with the given input string and returns the result as an InputStream. This
   * is the preferred method for handling potentially large data sets as it allows for streaming
   * processing.
   *
   * @param input
   *          The input string to process
   * @return An InputStream containing the result of the command execution
   */
  InputStream executeToStream(String input);

  /**
   * Get a description of what this command does
   *
   * @return the command description
   */
  String getDescription();
}

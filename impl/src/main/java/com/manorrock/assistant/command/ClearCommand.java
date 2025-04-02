package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;

/**
 * Command implementation that clears the response area.
 */
public class ClearCommand implements Command {

  @Override
  public String execute(String input) {
    return "Response area cleared.";
  }

  @Override
  public String getDescription() {
    return "Clear the response window";
  }

  @Override
  public String getShortDescription() {
    return "Clears the response window";
  }
}

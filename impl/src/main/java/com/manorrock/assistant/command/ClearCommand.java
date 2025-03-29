package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Command implementation that clears the response area.
 */
public class ClearCommand implements Command {

  @Override
  public String execute(String input) {
    return executeToString(input);
  }
  
  @Override
  public String executeToString(String input) {
    return "Response area cleared.";
  }

  @Override
  public InputStream executeToStream(String input) {
    return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
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

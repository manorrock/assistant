package com.manorrock.assistant.shared;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class NewCommand implements Command {

  private final Runnable newSessionHandler;

  public NewCommand(Runnable newSessionHandler) {
    this.newSessionHandler = newSessionHandler;
  }

  @Override
  public String executeToString(String input) {
    newSessionHandler.run();
    return "Started new chat session";
  }

  @Override
  public InputStream executeToStream(String input) {
    return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String getDescription() {
    return "Start a new chat session";
  }
}

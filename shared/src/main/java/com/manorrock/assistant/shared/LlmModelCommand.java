package com.manorrock.assistant.shared;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LlmModelCommand implements Command {
  private final LlmConfiguration config;

  public LlmModelCommand(LlmConfiguration config) {
    this.config = config;
  }

  @Override
  public String executeToString(String input) {
    if (input == null || input.trim().isEmpty()) {
      return "Current model: " + config.model();
    }
    String newModel = input.trim();
    config.setModel(newModel);
    return "Model changed to " + newModel;
  }

  @Override
  public InputStream executeToStream(String input) {
    return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String getDescription() {
    return "Change the LLM model being used (e.g. llama2, codellama, mistral)";
  }
}

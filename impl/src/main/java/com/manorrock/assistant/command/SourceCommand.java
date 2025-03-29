package com.manorrock.assistant.command;

import com.manorrock.assistant.api.Command;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class SourceCommand implements Command {
  private final Consumer<String> messageHandler;
  private final Consumer<String> commandHandler;

  public SourceCommand(Consumer<String> messageHandler, Consumer<String> commandHandler) {
    this.messageHandler = messageHandler;
    this.commandHandler = commandHandler;
  }

  @Override
  public String execute(String input) {
    return executeToString(input);
  }
  
  @Override
  public String executeToString(String input) {
    if (input == null || input.trim().isEmpty()) {
      return "Usage: /source <file_path>";
    }

    String filePath = input.trim();
    Path path = Paths.get(filePath);

    try {
      String content = Files.readString(path);
      StringBuilder messageBuilder = new StringBuilder();

      for (String line : content.split("\n")) {
        String trimmedLine = line.trim();

        if (line.endsWith("\\") || trimmedLine.endsWith("\\")) {
          messageBuilder.append(line, 0, line.lastIndexOf('\\')).append("\n");
          continue;
        }

        messageBuilder.append(line);

        String fullMessage = messageBuilder.toString().trim();
        if (!fullMessage.isEmpty()) {
          if (fullMessage.startsWith("/") && !fullMessage.startsWith("/exit")) {
            commandHandler.accept(fullMessage);
          } else {
            messageHandler.accept(fullMessage);
          }
        }
        messageBuilder.setLength(0);
      }

      String remaining = messageBuilder.toString().trim();
      if (!remaining.isEmpty()) {
        if (remaining.startsWith("/") && !remaining.startsWith("/exit")) {
          commandHandler.accept(remaining);
        } else {
          messageHandler.accept(remaining);
        }
      }

      return "Successfully executed commands from " + filePath;
    } catch (IOException e) {
      return "Error reading source file: " + e.getMessage();
    }
  }

  @Override
  public InputStream executeToStream(String input) {
    return new ByteArrayInputStream(executeToString(input).getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String getDescription() {
    return "Execute commands from a file";
  }
}

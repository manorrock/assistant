package com.manorrock.assistant.cli;

import com.manorrock.assistant.shared.Command;
import com.manorrock.assistant.shared.CommandRegistry;
import com.manorrock.assistant.shared.LlmConfiguration;
import com.manorrock.assistant.shared.LlmModelCommand;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;

@picocli.CommandLine.Command(name = "assistant-cli", mixinStandardHelpOptions = true, versionProvider = CLI.PropertiesVersionProvider.class, description = "CLI version of the Manorrock Assistant")
public class CLI implements Callable<Integer> {

  private LlmConfiguration config;

  @Option(names = {"--stdin"}, description = "Read message from standard input")
  private boolean readFromStdin = false;

  @Option(names = {"-i", "--interactive"}, description = "Start in interactive mode")
  private boolean interactive = false;

  @Parameters(paramLabel = "MESSAGE", description = "Message to send", arity = "0..1")
  private String message;

  private String sessionId = UUID.randomUUID().toString();
  private LinkedList<ChatMessage> history = new LinkedList<>();
  private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
  private Path stateDir = Paths.get(System.getProperty("user.home"), ".manorrock", "assistant", "cli-state");
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  public CLI() {
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new CLI()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    loadState();
    if (config == null) {
      config = LlmConfiguration.defaultConfig();
    }
    CommandRegistry.getInstance().registerCommand("llmModel", new LlmModelCommand(config));

    if (interactive) {
      startInteractiveMode();
    } else if (readFromStdin) {
      message = new String(System.in.readAllBytes()).trim();
    }
    if (message != null) {
      handleSendAction(message);
    } else if (!interactive) {
      showHelp();
    }
    saveState();
    return 0;
  }

  private void handleSendAction(String userMessage) {
    if (!userMessage.isEmpty()) {
      if (userMessage.startsWith("/")) {
        handleCommand(userMessage);
        return;
      }

      System.out.println("You: " + userMessage);
      processMessage(userMessage);
    }
  }

  protected void handleCommand(String command) {
    if (command.startsWith("/llmEndpoint ")) {
      changeEndpoint(command);
    } else if (command.startsWith("/llmModel")) {
      String cmdLine = command.substring(9); // remove '/llmModel'
      LlmModelCommand modelCommand = CommandRegistry.getInstance().getCommand("llmModel", LlmModelCommand.class);
      if (modelCommand != null) {
        String result = modelCommand.executeToString(cmdLine);
        System.out.println("System: " + result);
        saveState();
      } else {
        System.out.println("System: LLM model command not available");
      }
    } else if (command.startsWith("/llmVendor ")) {
      changeVendor(command);
    } else if (command.startsWith("/llmApiKey ")) {
      changeApiKey(command);
    } else if (command.startsWith("/llmTemperature ")) {
      changeModelTemperature(command);
    } else if (command.equals("/help")) {
      showHelp();
    } else if (command.startsWith("/explain")) {
      explainFromClipboardOrFile(command);
    } else if (command.startsWith("/source ")) {
      handleSourceCommand(command);
    } else {
      String cmdLine = command.substring(1); // remove the leading '/'
      int spaceIndex = cmdLine.indexOf(' ');

      String cmdName;
      String cmdArgs;
      if (spaceIndex > 0) {
        cmdName = cmdLine.substring(0, spaceIndex);
        cmdArgs = cmdLine.substring(spaceIndex + 1).trim();
      } else {
        cmdName = cmdLine;
        cmdArgs = "";
      }

      Command cmd = CommandRegistry.getInstance().getCommand(cmdName);
      if (cmd != null) {
        System.out.println("System: " + cmd.executeToString(cmdArgs));
      } else {
        System.out.println("System: Unknown command. Type /help for a list of commands.");
      }
    }
  }

  private void handleSourceCommand(String command) {
    String filePath = command.substring(8).trim();
    Path path = Paths.get(filePath);

    try {
      String content = Files.readString(path);
      StringBuilder messageBuilder = new StringBuilder();

      // Process the file line by line
      for (String line : content.split("\n")) {
        String trimmedLine = line.trim();

        // Handle line continuation
        if (line.endsWith("\\") || trimmedLine.endsWith("\\")) {
          messageBuilder.append(line, 0, line.lastIndexOf('\\')).append("\n");
          continue;
        }

        // Add the line to the current message
        messageBuilder.append(line);

        // Process the complete message
        String fullMessage = messageBuilder.toString().trim();
        if (!fullMessage.isEmpty()) {
          if (fullMessage.startsWith("/") && !fullMessage.startsWith("/exit")) {
            handleCommand(fullMessage);
          } else {
            handleSendAction(fullMessage);
          }
        }
        messageBuilder.setLength(0);
      }

      // Handle any remaining content
      String remaining = messageBuilder.toString().trim();
      if (!remaining.isEmpty()) {
        if (remaining.startsWith("/") && !remaining.startsWith("/exit")) {
          handleCommand(remaining);
        } else {
          handleSendAction(remaining);
        }
      }

    } catch (IOException e) {
      System.out.println("System: Error reading source file: " + e.getMessage());
    }
  }

  private void explainFromClipboardOrFile(String command) {
    String textToExplain = null;

    // Parse optional file path if provided
    String filePath = null;
    if (command.length() > 9) { // "/explain " + something
      filePath = command.substring(9).trim();
    }

    if (filePath != null && !filePath.isEmpty()) {
      // Read from file
      try {
        textToExplain = Files.readString(Paths.get(filePath));
        System.out.println("System: Explaining content from file: " + filePath);
      } catch (IOException e) {
        System.out.println("System: Error reading file: " + e.getMessage());
        return;
      }
    } else {
      // Read from clipboard
      try {
        textToExplain = getClipboardContent();
        System.out.println("System: Explaining content from clipboard");
      } catch (Exception e) {
        System.out.println("System: Failed to access clipboard: " + e.getMessage());
        System.out.println("System: Usage: /explain [file_path] - Explains text from clipboard or specified file");
        return;
      }
    }

    if (textToExplain != null && !textToExplain.trim().isEmpty()) {
      String promptPrefix = "Please explain the following text in a clear and concise manner:\n\n";
      processMessage(promptPrefix + textToExplain);
    } else {
      System.out.println("System: No content found to explain.");
    }
  }

  private String getClipboardContent() throws Exception {
    // For Mac/Linux, we can use the 'pbpaste' or 'xclip' commands
    String os = System.getProperty("os.name").toLowerCase();
    ProcessBuilder pb;

    if (os.contains("mac")) {
      pb = new ProcessBuilder("pbpaste");
    } else if (os.contains("nix") || os.contains("nux")) {
      pb = new ProcessBuilder("xclip", "-selection", "clipboard", "-o");
    } else if (os.contains("win")) {
      pb = new ProcessBuilder("powershell.exe", "-command", "Get-Clipboard");
    } else {
      throw new UnsupportedOperationException("Clipboard access not supported on this OS");
    }

    Process process = pb.start();
    String content = new String(process.getInputStream().readAllBytes());
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      throw new IOException("Failed to get clipboard content, exit code: " + exitCode);
    }

    return content;
  }

  private void changeEndpoint(String command) {
    String newEndpoint = command.substring(12).trim();
    if (!newEndpoint.startsWith("http://") && !newEndpoint.startsWith("https://")) {
      newEndpoint = "http://" + newEndpoint;
    }
    newEndpoint = newEndpoint + "/api/chat";
    config = new LlmConfiguration(newEndpoint, config.model(), config.vendor(), config.apiKey(), config.temperature());
    System.out.println("System: Endpoint changed to " + newEndpoint);
    saveState();
  }

  private void changeModel(String command) {
    String newModel = command.substring(10).trim();
    config = new LlmConfiguration(config.endpoint(), newModel, config.vendor(), config.apiKey(), config.temperature());
    System.out.println("System: Model changed to " + newModel);
    saveState();
  }

  private void changeVendor(String command) {
    String newVendor = command.substring(11).trim().toUpperCase();
    config = new LlmConfiguration(config.endpoint(), config.model(), newVendor, config.apiKey(), config.temperature());
    System.out.println("System: Vendor changed to " + newVendor);
    saveState();
  }

  private void changeApiKey(String command) {
    String newApiKey = command.substring(11).trim();
    config = new LlmConfiguration(config.endpoint(), config.model(), config.vendor(), newApiKey, config.temperature());
    System.out.println("System: API key updated");
    saveState();
  }

  private void changeModelTemperature(String command) {
    try {
      double newTemperature = Double.parseDouble(command.substring(15).trim());
      if (newTemperature < 0.0 || newTemperature > 1.0) {
        System.out.println("System: Temperature must be between 0.0 and 1.0");
        return;
      }
      config = new LlmConfiguration(config.endpoint(), config.model(), config.vendor(), config.apiKey(),
          newTemperature);
      System.out.println("System: Temperature set to " + newTemperature);
      saveState();
    } catch (NumberFormatException e) {
      System.out.println("System: Invalid temperature format. Use /llmTemperature <number>");
    }
  }

  private void showHelp() {
    String cliHelp = "\n\nCLI-specific commands:\n" + "/llmEndpoint myhostname:myport - Change the endpoint\n"
        + "/llmModel <name> - Change the model used\n"
        + "/llmVendor <name> - Change the vendor (OLLAMA, OPENAI, AZURE_OPENAI)\n"
        + "/llmApiKey <key> - Set API key for OpenAI or Azure\n"
        + "/llmTemperature <value> - Set temperature (0.0-1.0)\n"
        + "/source <file_path> - Execute commands from a file\n"
        + "/explain [file_path] - Explain text from clipboard or specified file\n" + "/exit - Exit interactive mode";
    System.out.println(cliHelp);

    Command helpCommand = CommandRegistry.getInstance().getCommand("help");
    if (helpCommand != null) {
      System.out.println("\n" + helpCommand.executeToString(""));
    }
  }

  /**
   * Creates a streaming chat language model based on current configuration. Configures model-specific
   * settings for: - OLLAMA: Uses baseUrl, model name, timeout, temperature - OPENAI: Uses API key,
   * model name, timeout, temperature - AZURE_OPENAI: Uses endpoint, API key, deployment name,
   * timeout, temperature
   *
   * @return Configured StreamingChatLanguageModel instance
   * @throws IllegalArgumentException
   *           if vendor is unknown
   */
  private StreamingChatLanguageModel createLanguageModel() {
    String vendor = config.vendor();
    return switch (vendor.toUpperCase()) {
      case "OLLAMA" -> OllamaStreamingChatModel.builder()
          .baseUrl(config.endpoint().substring(0, config.endpoint().lastIndexOf("/api/chat"))).modelName(config.model())
          .timeout(TIMEOUT).temperature(config.temperature()).build();
      case "OPENAI" -> OpenAiStreamingChatModel.builder().apiKey(config.apiKey()).modelName(config.model())
          .timeout(TIMEOUT).temperature(config.temperature()).build();
      case "AZURE_OPENAI" -> AzureOpenAiStreamingChatModel.builder().endpoint(config.endpoint()).apiKey(config.apiKey())
          .deploymentName(config.model()).timeout(TIMEOUT).temperature(config.temperature()).build();
      default -> throw new IllegalArgumentException("Unknown vendor: " + vendor);
    };
  }

  private void processMessage(String message) {
    String timestamp = LocalDateTime.now().format(formatter);

    try {
      UserMessage userMessage = UserMessage.from(message);
      history.add(userMessage);
      if (history.size() > 50) {
        history.removeFirst();
      }

      StreamingChatLanguageModel langChainModel = createLanguageModel();

      StringBuilder responseBuilder = new StringBuilder();
      final boolean[] isFirstLine = {true};

      // Create a latch to wait for response completion
      final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

      ArrayList<ChatMessage> messages = new ArrayList<>(history);

      langChainModel.chat(messages, new StreamingChatResponseHandler() {
        @Override
        public void onPartialResponse(String token) {
          responseBuilder.append(token);
          if (isFirstLine[0]) {
            System.out.print("Assistant: " + token);
            isFirstLine[0] = false;
          } else {
            System.out.print(token);
          }
        }

        @Override
        public void onCompleteResponse(ChatResponse response) {
          String fullResponse = responseBuilder.toString().trim();
          if (fullResponse.isEmpty()) {
            System.out.println(); // Just print a newline if response was empty
          } else if (!isFirstLine[0]) {
            System.out.println(); // Add a final newline after streaming
          }

          // Add the assistant's response to the history
          history.add(AiMessage.from(fullResponse));
          if (history.size() > 50) {
            history.removeFirst();
          }

          // Signal that processing is complete
          latch.countDown();
        }

        @Override
        public void onError(Throwable error) {
          System.out.println("Assistant: Error: " + error.getMessage());
          // Signal that processing is complete even if there was an error
          latch.countDown();
        }
      });

      // Wait for the streaming response to complete
      try {
        latch.await();
      } catch (InterruptedException e) {
        System.out.println("Assistant: Processing was interrupted");
        Thread.currentThread().interrupt();
      }

    } catch (Exception e) {
      String errorMessage;
      if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
        errorMessage = "Request timed out after " + TIMEOUT.getSeconds() + " seconds";
      } else {
        errorMessage = "Error: " + e.getMessage();
      }
      System.out.println("Assistant: " + errorMessage);
      System.out.println("[" + timestamp + " - Error]\n" + e.getMessage());
    }
  }

  private void startInteractiveMode() {
    System.out.println("Entering interactive mode. Type /exit to quit, or /help for commands.");
    System.out.println("Use \\ at end of line for multi-line input.");

    StringBuilder messageBuilder = new StringBuilder();
    java.util.Scanner scanner = new java.util.Scanner(System.in);

    while (true) {
      System.out.print(messageBuilder.length() == 0 ? "\nYou: " : "... ");

      String line = scanner.nextLine();
      String trimmedLine = line.trim();

      // Check for exit command
      if (trimmedLine.equals("/exit")) {
        System.out.println("Exiting interactive mode.");
        break;
      }

      // Handle commands when not in the middle of a message
      if (trimmedLine.startsWith("/") && messageBuilder.length() == 0) {
        handleCommand(trimmedLine);
        continue;
      }

      // Handle line continuation
      if (line.endsWith("\\") || trimmedLine.endsWith("\\")) {
        // Remove the backslash and add the line with a newline
        messageBuilder.append(line.substring(0, line.lastIndexOf('\\')).stripTrailing()).append("\n");
        continue;
      }

      // Add the line to the message
      messageBuilder.append(line);

      // Process the complete message
      String fullMessage = messageBuilder.toString().trim();
      if (!fullMessage.isEmpty()) {
        handleSendAction(fullMessage);
      }
      messageBuilder.setLength(0);
    }
  }

  private void loadState() {
    try {
      if (Files.exists(stateDir)) {
        Path configFile = stateDir.resolve("config.json");
        if (Files.exists(configFile)) {
          String content = Files.readString(configFile);
          JSONObject configJson = new JSONObject(content);
          config = new LlmConfiguration(configJson.getString("endpoint"), configJson.getString("model"),
              configJson.getString("vendor"), configJson.getString("apiKey"), configJson.getDouble("temperature"));
        }

        // History saved in JSON format needs to be converted to ChatMessage objects
        Path historyFile = stateDir.resolve("history.json");
        if (Files.exists(historyFile)) {
          String content = Files.readString(historyFile);
          JSONArray jsonArray = new JSONArray(content);
          for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject msgObj = jsonArray.getJSONObject(i);
            String role = msgObj.getString("role");
            String msgContent = msgObj.getString("content");

            if ("user".equals(role)) {
              history.add(UserMessage.from(msgContent));
            } else if ("assistant".equals(role)) {
              history.add(AiMessage.from(msgContent));
            } // Ignore system messages for simplicity
          }
        }

        Path sessionIdFile = stateDir.resolve("session_id.txt");
        if (Files.exists(sessionIdFile)) {
          sessionId = Files.readString(sessionIdFile).trim();
        }
      } else {
        Files.createDirectories(stateDir);
      }
    } catch (IOException e) {
      System.out.println("Error loading state: " + e.getMessage());
    }
  }

  private void saveState() {
    try {
      Path configFile = stateDir.resolve("config.json");
      JSONObject configJson = new JSONObject();
      configJson.put("endpoint", config.endpoint());
      configJson.put("model", config.model());
      configJson.put("vendor", config.vendor());
      configJson.put("apiKey", config.apiKey());
      configJson.put("temperature", config.temperature());
      Files.writeString(configFile, configJson.toString());

      // Convert ChatMessage objects to JSON format for saving
      Path historyFile = stateDir.resolve("history.json");
      JSONArray historyArray = new JSONArray();
      for (ChatMessage msg : history) {
        JSONObject msgObj = new JSONObject();
        if (msg instanceof UserMessage) {
          msgObj.put("role", "user");
          msgObj.put("content", ((UserMessage) msg).text());
        } else if (msg instanceof AiMessage) {
          msgObj.put("role", "assistant");
          msgObj.put("content", ((AiMessage) msg).text());
        } else if (msg instanceof SystemMessage) {
          msgObj.put("role", "system");
          msgObj.put("content", ((SystemMessage) msg).text());
        }
        historyArray.put(msgObj);
      }
      Files.writeString(historyFile, historyArray.toString());

      Path sessionIdFile = stateDir.resolve("session_id.txt");
      Files.writeString(sessionIdFile, sessionId);
    } catch (IOException e) {
      System.out.println("Error saving state: " + e.getMessage());
    }
  }

  static class PropertiesVersionProvider implements CommandLine.IVersionProvider {
    public String[] getVersion() throws Exception {
      Properties props = new Properties();
      try (InputStream is = getClass().getClassLoader().getResourceAsStream("version.properties")) {
        if (is != null) {
          props.load(is);
          String version = props.getProperty("version", "unknown");
          // Strip -SNAPSHOT if present
          return new String[]{version.replace("-SNAPSHOT", "")};
        }
      }
      return new String[]{"unknown"};
    }
  }
}

package com.manorrock.assistant.cli;

import com.manorrock.assistant.shared.Command;
import com.manorrock.assistant.shared.CommandRegistry;
import com.manorrock.assistant.shared.DefaultToolManager;
import com.manorrock.assistant.shared.LlmConfiguration;
import com.manorrock.assistant.shared.LlmModelCommand;
import com.manorrock.assistant.shared.NewCommand;
import com.manorrock.assistant.shared.SourceCommand;
import com.manorrock.assistant.shared.ToolManager;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatResponseMetadata;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.TokenUsage;

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
import java.util.concurrent.Callable;
import com.manorrock.assistant.shared.Tool;
import com.manorrock.assistant.shared.ToolResult;
import com.manorrock.assistant.shared.tools.FileReadTool;
import com.manorrock.assistant.shared.tools.DirectoryListTool;
import com.manorrock.assistant.shared.tools.ProcessExecutionTool;
import com.manorrock.assistant.shared.tools.ProjectStructureAnalysisTool;
import com.manorrock.assistant.shared.tools.DependencyAnalysisTool;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import com.manorrock.assistant.shared.tools.generic.JsonBasedTool;
import com.manorrock.assistant.shared.tools.generic.ScriptBasedTool;
import com.manorrock.assistant.shared.ToolCommand;
import com.manorrock.assistant.shared.ToolExecutionException;
import java.util.stream.Collectors;

@picocli.CommandLine.Command(name = "assistant-cli", mixinStandardHelpOptions = true, versionProvider = CLI.PropertiesVersionProvider.class, description = "CLI version of the Manorrock Assistant")
public class CLI implements Callable<Integer> {

  private LlmConfiguration config;
  private ToolManager toolManager;
  // Remove the separate registeredTools list, we'll use toolManager.getAvailableTools() instead
  private boolean useToolIntegration = false;

  @Option(names = {"--stdin"}, description = "Read message from standard input")
  private boolean readFromStdin = false;

  @Option(names = {"-i", "--interactive"}, description = "Start in interactive mode")
  private boolean interactive = false;

  @Parameters(paramLabel = "MESSAGE", description = "Message to send", arity = "0..1")
  private String message;

  private LinkedList<ChatMessage> history = new LinkedList<>();
  private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
  private Path stateDir = Paths.get(System.getProperty("user.home"), ".manorrock", "assistant", "cli-state");
  private static final Duration TIMEOUT = Duration.ofMinutes(5);

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
    
    // Initialize tool manager and register default tools
    initializeToolManager();
    
    CommandRegistry.getInstance().registerCommand("llmModel", new LlmModelCommand(config));
    // Register the LlmCommand
    CommandRegistry.getInstance().registerCommand("llm", 
        new com.manorrock.assistant.shared.LlmCommand(
            () -> config, 
            newConfig -> { 
                config = newConfig;
                saveState();
            }));
    CommandRegistry.getInstance().registerCommand("source",
        new SourceCommand(this::handleSendAction, this::handleCommand));
    CommandRegistry.getInstance().registerCommand("new", new NewCommand(this::startNewSession));
    
    // Register the tool command with integration support - use toolManager directly
    CommandRegistry.getInstance().registerCommand("tool", new ToolCommand(
        () -> new ArrayList<>(toolManager.getAvailableTools()),
        this::executeToolWithParams,
        (enabled) -> useToolIntegration = enabled,
        () -> useToolIntegration
    ));

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

  /**
   * Initialize the tool manager and register default tools.
   */
  private void initializeToolManager() {
    toolManager = new DefaultToolManager();
    
    // Register default tools
    registerTool(new FileReadTool());
    registerTool(new DirectoryListTool());
    registerTool(new ProcessExecutionTool());
    registerTool(new ProjectStructureAnalysisTool());
    registerTool(new DependencyAnalysisTool());
    
    // Discover and register custom tools
    discoverAndRegisterCustomTools();
  }
  
  /**
   * Register a tool with the tool manager.
   * 
   * @param tool The tool to register
   * @return true if the tool was registered successfully, false otherwise
   */
  private boolean registerTool(Tool tool) {
    try {
      // Use the toolManager for validation before registration
      toolManager.registerTool(tool);
      return true;
    } catch (Exception e) {
      System.err.println("Failed to register tool: " + 
              (tool != null ? tool.getName() : "null") + " - " + e.getMessage());
      return false;
    }
  }
  
  /**
   * Execute a tool with the given name and parameters.
   * This method is used by the ToolCommand.
   *
   * @param toolName Name of the tool to execute
   * @param parameters Parameters to pass to the tool
   * @return Result of the tool execution
   */
  private ToolResult executeToolWithParams(String toolName, Map<String, Object> parameters) {
    try {
      return toolManager.executeTool(toolName, parameters);
    } catch (ToolExecutionException e) {
      return ToolResult.failure(e.getMessage());
    }
  }

  // Remove the validateTool method - validation should be handled by the ToolManager

  /**
   * Discover and register tools from custom directories.
   * 
   * @return The number of custom tools registered
   */
  private int discoverAndRegisterCustomTools() {
    int count = 0;
    
    // Use ServiceLoader to discover tools on the classpath
    ServiceLoader<Tool> serviceLoader = ServiceLoader.load(Tool.class);
    for (Tool tool : serviceLoader) {
      if (registerTool(tool)) {
        count++;
        System.out.println("Discovered and registered tool via ServiceLoader: " + tool.getName());
      }
    }
    
    // Look for tools in custom directories
    String defaultToolDir = System.getProperty("user.home") + "/.manorrock/assistant/tools";
    
    // Check for additional tool directories from system property
    String toolDirsProp = System.getProperty("manorrock.assistant.tool.dirs");
    List<String> toolDirs = new ArrayList<>();
    toolDirs.add(defaultToolDir);
    
    if (toolDirsProp != null && !toolDirsProp.isEmpty()) {
      String[] dirs = toolDirsProp.split(File.pathSeparator);
      for (String dir : dirs) {
        if (!dir.trim().isEmpty()) {
          toolDirs.add(dir.trim());
        }
      }
    }
    
    // Scan each directory for tools
    for (String toolDir : toolDirs) {
      File dir = new File(toolDir);
      if (dir.exists() && dir.isDirectory()) {
        int dirCount = loadToolsFromDirectory(dir);
        count += dirCount;
        if (dirCount > 0) {
          System.out.println("Discovered and registered " + dirCount + " tools from " + toolDir);
        }
      } else {
        // Create the directory if it doesn't exist
        if (!dir.exists() && toolDir.equals(defaultToolDir)) {
          try {
            Files.createDirectories(dir.toPath());
            System.out.println("Created tool directory: " + toolDir);
          } catch (IOException e) {
            System.err.println("Failed to create tool directory: " + toolDir + " - " + e.getMessage());
          }
        }
      }
    }
    
    return count;
  }
  
  /**
   * Load tools from a directory. The directory can contain:
   * 1. JAR files with tools that implement the Tool interface
   * 2. JSON descriptor files for script-based tools
   * 
   * @param directory The directory to scan for tools
   * @return The number of tools registered from this directory
   */
  private int loadToolsFromDirectory(File directory) {
    int count = 0;
    
    // Look for JAR files
    File[] jarFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
    if (jarFiles != null) {
      for (File jarFile : jarFiles) {
        try {
          count += loadToolsFromJar(jarFile);
        } catch (Exception e) {
          System.err.println("Error loading tools from JAR: " + jarFile.getName() + " - " + e.getMessage());
        }
      }
    }
    
    // Look for JSON descriptor files
    File[] jsonFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".tool.json"));
    if (jsonFiles != null) {
      for (File jsonFile : jsonFiles) {
        try {
          if (loadToolFromJson(jsonFile)) {
            count++;
          }
        } catch (Exception e) {
          System.err.println("Error loading tool from JSON: " + jsonFile.getName() + " - " + e.getMessage());
        }
      }
    }
    
    // Look for script files with associated metadata
    File[] scriptFiles = directory.listFiles((dir, name) -> 
        name.toLowerCase().endsWith(".sh") || 
        name.toLowerCase().endsWith(".bat") || 
        name.toLowerCase().endsWith(".ps1"));
    if (scriptFiles != null) {
      for (File scriptFile : scriptFiles) {
        String baseName = scriptFile.getName().substring(0, scriptFile.getName().lastIndexOf('.'));
        File metadataFile = new File(directory, baseName + ".metadata.json");
        if (metadataFile.exists() && metadataFile.isFile()) {
          try {
            if (loadScriptTool(scriptFile, metadataFile)) {
              count++;
            }
          } catch (Exception e) {
            System.err.println("Error loading script tool: " + scriptFile.getName() + " - " + e.getMessage());
          }
        }
      }
    }
    
    return count;
  }
  
  /**
   * Load tools from a JAR file using a URLClassLoader.
   * 
   * @param jarFile The JAR file to load tools from
   * @return The number of tools registered from this JAR
   * @throws Exception If an error occurs while loading tools
   */
  private int loadToolsFromJar(File jarFile) throws Exception {
    int count = 0;
    try (URLClassLoader classLoader = new URLClassLoader(
        new URL[] { jarFile.toURI().toURL() },
        getClass().getClassLoader())) {
      // Use ServiceLoader with the custom class loader
      ServiceLoader<Tool> serviceLoader = ServiceLoader.load(Tool.class, classLoader);
      for (Tool tool : serviceLoader) {
        if (registerTool(tool)) {
          count++;
          System.out.println("Registered tool from JAR " + jarFile.getName() + ": " + tool.getName());
        }
      }
    }
    return count;
  }
  
  /**
   * Load a tool from a JSON descriptor file.
   * The JSON format should match the Tool interface.
   * 
   * @param jsonFile The JSON file describing the tool
   * @return true if the tool was registered, false otherwise
   * @throws Exception If an error occurs while loading the tool
   */
  private boolean loadToolFromJson(File jsonFile) throws Exception {
    String json = Files.readString(jsonFile.toPath());
    JSONObject toolJson = new JSONObject(json);
    
    // Create a JSON-based tool implementation
    Tool tool = new JsonBasedTool(toolJson);
    
    return registerTool(tool);
  }
  
  /**
   * Load a script-based tool using a script file and its metadata.
   * 
   * @param scriptFile The script file (.sh, .bat, .ps1)
   * @param metadataFile The JSON metadata file
   * @return true if the tool was registered, false otherwise
   * @throws Exception If an error occurs while loading the tool
   */
  private boolean loadScriptTool(File scriptFile, File metadataFile) throws Exception {
    String metadata = Files.readString(metadataFile.toPath());
    JSONObject metadataJson = new JSONObject(metadata);
    
    // Create a script-based tool implementation
    Tool tool = new ScriptBasedTool(scriptFile, metadataJson);
    
    return registerTool(tool);
  }

  protected void handleSendAction(String userMessage) {
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
    } else {
      // Handle all other commands through the command registry
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
    String cliHelp = "CLI-specific commands:\n" + 
        "/llmEndpoint <hostname:port> - Change the endpoint\n" +
        "/llmModel <name> - Change the model used\n" +
        "/llmVendor <name> - Change the vendor (OLLAMA, OPENAI, AZURE_OPENAI)\n" +
        "/llmApiKey <key> - Set the API key\n" +
        "/llmTemperature <value> - Set temperature (0.0-1.0)\n" +
        "/explain [file_path] - Explain text from clipboard or specified file\n" +
        "/new - Start a new chat session\n" +
        "/help - Show this help message\n" +
        "/exit - Exit interactive mode";
    System.out.println(cliHelp);

    Command helpCommand = CommandRegistry.getInstance().getCommand("help");
    if (helpCommand != null) {
      System.out.println("\n" + helpCommand.executeToString(""));
    }
    
    // Show tool help through the tool command
    Command toolCommand = CommandRegistry.getInstance().getCommand("tool");
    if (toolCommand != null) {
      System.out.println("\nTool commands:\n" + toolCommand.executeToString(""));
    }
    
    // Show llm command help
    Command llmCommand = CommandRegistry.getInstance().getCommand("llm");
    if (llmCommand != null) {
      System.out.println("\nLLM configuration commands:\n" + llmCommand.executeToString(""));
    }
  }

  /**
   * Creates a streaming chat language model based on current configuration.
   * Configures model-specific settings for:
   * - OLLAMA: Uses baseUrl, model name, timeout, temperature
   * - OPENAI: Uses API key, model name, timeout, temperature
   * - AZURE_OPENAI: Uses endpoint, API key, deployment name, timeout, temperature
   * 
   * @return Configured StreamingChatLanguageModel instance
   * @throws IllegalArgumentException if vendor is unknown
   */
  private StreamingChatLanguageModel createLanguageModel() {
    ChatModelListener listener = new ChatModelListener() {
      @Override
      public void onRequest(ChatModelRequestContext requestContext) {
          ChatRequest chatRequest = requestContext.chatRequest();
          List<ChatMessage> messages = chatRequest.messages();
          System.out.println(messages);

          ChatRequestParameters parameters = chatRequest.parameters();
          System.out.println(parameters.modelName());
          System.out.println(parameters.temperature());
          System.out.println(parameters.topP());
          System.out.println(parameters.topK());
          System.out.println(parameters.frequencyPenalty());
          System.out.println(parameters.presencePenalty());
          System.out.println(parameters.maxOutputTokens());
          System.out.println(parameters.stopSequences());
          System.out.println(parameters.toolSpecifications());
          System.out.println(parameters.toolChoice());
          System.out.println(parameters.responseFormat());

          if (parameters instanceof OpenAiChatRequestParameters openAiParameters) {
              System.out.println(openAiParameters.maxCompletionTokens());
              System.out.println(openAiParameters.logitBias());
              System.out.println(openAiParameters.parallelToolCalls());
              System.out.println(openAiParameters.seed());
              System.out.println(openAiParameters.user());
              System.out.println(openAiParameters.store());
              System.out.println(openAiParameters.metadata());
              System.out.println(openAiParameters.serviceTier());
              System.out.println(openAiParameters.reasoningEffort());
          }

          System.out.println(requestContext.modelProvider());

          Map<Object, Object> attributes = requestContext.attributes();
          attributes.put("my-attribute", "my-value");
      }

      @Override
      public void onResponse(ChatModelResponseContext responseContext) {
          ChatResponse chatResponse = responseContext.chatResponse();
          AiMessage aiMessage = chatResponse.aiMessage();
          System.out.println(aiMessage);

          ChatResponseMetadata metadata = chatResponse.metadata();
          System.out.println(metadata.id());
          System.out.println(metadata.modelName());
          System.out.println(metadata.finishReason());

          if (metadata instanceof OpenAiChatResponseMetadata openAiMetadata) {
              System.out.println(openAiMetadata.created());
              System.out.println(openAiMetadata.serviceTier());
              System.out.println(openAiMetadata.systemFingerprint());
          }

          TokenUsage tokenUsage = metadata.tokenUsage();
          System.out.println(tokenUsage.inputTokenCount());
          System.out.println(tokenUsage.outputTokenCount());
          System.out.println(tokenUsage.totalTokenCount());
          if (tokenUsage instanceof OpenAiTokenUsage openAiTokenUsage) {
              System.out.println(openAiTokenUsage.inputTokensDetails().cachedTokens());
              System.out.println(openAiTokenUsage.outputTokensDetails().reasoningTokens());
          }

          ChatRequest chatRequest = responseContext.chatRequest();
          System.out.println(chatRequest);
          System.out.println(responseContext.modelProvider());

          Map<Object, Object> attributes = responseContext.attributes();
          System.out.println(attributes.get("my-attribute"));
      }

      @Override
      public void onError(ChatModelErrorContext errorContext) {
          Throwable error = errorContext.error();
          error.printStackTrace();

          ChatRequest chatRequest = errorContext.chatRequest();
          System.out.println(chatRequest);
          System.out.println(errorContext.modelProvider());

          Map<Object, Object> attributes = errorContext.attributes();
          System.out.println(attributes.get("my-attribute"));
      }
    };

    String vendor = config.vendor();
    return switch (vendor.toUpperCase()) {
      case "OLLAMA" -> OllamaStreamingChatModel.builder()
          .baseUrl(config.endpoint().substring(0, config.endpoint().lastIndexOf("/api/chat")))
          .modelName(config.model())
          .timeout(TIMEOUT)
          .temperature(config.temperature())
          // .listeners(List.of(listener))
          // .logRequests(true)
          // .logResponses(true)
          .build();
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
      final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

      ArrayList<ChatMessage> messages = new ArrayList<>(history);
      
      // Add system message for tool-enabled interactions
      if (useToolIntegration) {
        messages.add(0, dev.langchain4j.data.message.SystemMessage.from(
            "You are a helpful assistant with access to tools. When appropriate, use tools to accomplish tasks. " +
            "Always think step by step and explain your reasoning clearly."));
      }
      List<ToolSpecification> toolSpecifications = new ArrayList<>();
      toolSpecifications.addAll(toolManager.getAvailableTools().stream()
      .map(tool -> {
        ToolSpecification.Builder toolSpecBuilder = ToolSpecification.builder()
            .name(tool.getName())
            .description(tool.getDescription());

        JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
        
        // Add each parameter to the schema
        tool.getParameters().forEach(param -> {
            switch (param.getType().toLowerCase()) {
                case "string":
                    schemaBuilder.addStringProperty(param.getName(), param.getDescription());
                    break;
                case "integer":
                case "int":
                    schemaBuilder.addIntegerProperty(param.getName(), param.getDescription());
                    break;
                case "number":
                case "float":
                case "double":
                    schemaBuilder.addNumberProperty(param.getName(), param.getDescription());
                    break;
                case "boolean":
                case "bool":
                    schemaBuilder.addBooleanProperty(param.getName(), param.getDescription());
                    break;
                default:
                    // Default to string for unknown types
                    schemaBuilder.addStringProperty(param.getName(), param.getDescription());
            }
            // Mark required parameters
            if (param.isRequired()) {
                schemaBuilder.required(param.getName());
            }
        });
        // Add the parameter schema to the tool specification
        return toolSpecBuilder
            .parameters(schemaBuilder.build())
            .build();
    })
    .collect(Collectors.toList()));
      
      ChatRequest request = ChatRequest.builder()
          .messages(messages)
          .toolSpecifications(toolSpecifications)
          .build();
      
      
      langChainModel.chat(request, new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {

        public void onPartialResponse(String partialResponse) {
          String content = partialResponse;
              responseBuilder.append(content);
              if (isFirstLine[0]) {
                  System.out.print("Assistant: " + content);
                  isFirstLine[0] = false;
              } else {
                  System.out.print(content);
              }
        }

        public void onCompleteResponse(ChatResponse completeResponse) {
            System.out.println(); // Print newline after completion
            AiMessage aiMessage = completeResponse.aiMessage();
            
            if (aiMessage.hasToolExecutionRequests()) {
                // Process all tool requests and collect their results
                List<ChatMessage> updatedMessages = new ArrayList<>(history);
                updatedMessages.add(aiMessage);
                aiMessage.toolExecutionRequests().forEach(request -> {
                    try {
                        // Execute the tool
                        ToolResult result = executeToolWithParams(
                            request.name(),
                            new JSONObject(request.arguments()).toMap()
                        );
                        // Create a JSON object containing both status and result data
                        JSONObject resultJson = new JSONObject();
                        resultJson.put("status", result.success() ? "success" : "error");
                        resultJson.put("message", result.getMessage());
                        
                        // If there's result data, include it
                        if (result.getData() != null) {
                            resultJson.put("data", result.getData());
                        }
                        
                        // Create tool execution result message with complete result information
                        ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(
                            request,
                            resultJson.toString()
                        );
                        
                        // Add the tool result to the message history
                        updatedMessages.add(resultMessage);
                    } catch (Exception e) {
                        // Handle any errors during tool execution
                        JSONObject errorJson = new JSONObject();
                        errorJson.put("status", "error");
                        errorJson.put("message", e.getMessage());
                        
                        ToolExecutionResultMessage errorMessage = ToolExecutionResultMessage.from(
                            request,
                            errorJson.toString()
                        );
                        updatedMessages.add(errorMessage);
                    }
                });
                // Send the updated conversation back to the LLM for a final response
                try {
                    ChatRequest followUpRequest = ChatRequest.builder()
                        .messages(updatedMessages)
                        .toolSpecifications(toolSpecifications)  // Add tool specifications to follow-up request
                        .build();
                    // Clear the existing response builder for the new response
                    responseBuilder.setLength(0);
                    isFirstLine[0] = true;
                    // Process the follow-up request
                    langChainModel.chat(followUpRequest, this);
                    return; // The new response chain will handle the latch countdown
                } catch (Exception e) {
                    System.out.println("\nError processing tool results: " + e.getMessage());
                    // Make sure to countdown the latch in case of error
                    latch.countDown();
                }
            } else {
                // For responses without tool requests or after tool processing
                history.add(aiMessage);
                if (history.size() > 50) {
                    history.removeFirst();
                }
                
                latch.countDown();
            }
        }

        public void onError(Throwable error) {
          System.out.println("\nError generating response: " + error.getMessage());
          error.printStackTrace(System.out);
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
    java.util.Scanner scanner = new java.util.Scanner(System.in);
    StringBuilder messageBuilder = new StringBuilder();
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
    } catch (IOException e) {
      System.out.println("Error saving state: " + e.getMessage());
    }
  }

  protected void startNewSession() {
    history.clear();
    saveState();
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
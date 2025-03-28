package com.manorrock.assistant.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolManager;
import com.manorrock.assistant.api.ToolResult;
import com.manorrock.assistant.command.HelpCommand;
import com.manorrock.assistant.command.NewCommand;
import com.manorrock.assistant.command.OllamaCommand;
import com.manorrock.assistant.command.SourceCommand;
import com.manorrock.assistant.command.ToolCommand;
import com.manorrock.assistant.core.Assistant;
import com.manorrock.assistant.llm.LlmConfiguration;
import com.manorrock.assistant.tool.DefaultToolManager;
import com.manorrock.assistant.tool.DependencyAnalysisTool;
import com.manorrock.assistant.tool.DirectoryListTool;
import com.manorrock.assistant.tool.FileReadTool;
import com.manorrock.assistant.tool.FileWriteTool;
import com.manorrock.assistant.tool.JsonBasedTool;
import com.manorrock.assistant.tool.MavenArchetypeTool;
import com.manorrock.assistant.tool.ProcessExecutionTool;
import com.manorrock.assistant.tool.ProjectStructureAnalysisTool;
import com.manorrock.assistant.tool.ScriptBasedTool;
import com.manorrock.assistant.tool.ShellExecutionTool;
import com.manorrock.assistant.tool.WebScraperTool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@picocli.CommandLine.Command(name = "assistant-cli", mixinStandardHelpOptions = true, versionProvider = CLI.PropertiesVersionProvider.class, description = "CLI version of the Manorrock Assistant")
public class CLI implements Callable<Integer> {

  private static final Logger LOGGER = Logger.getLogger(CLI.class.getName());
  private static final Duration TIMEOUT = Duration.ofMinutes(5);
  private static final int MEMORY_WINDOW_SIZE = 50; // Max number of messages to keep in chat memory
  
  private LlmConfiguration config;
  private ToolManager toolManager;
  private boolean useToolIntegration = true;
  private Assistant assistance;
  private ChatMemory chatMemory; // Added chat memory for conversation management

  @Option(names = {"--stdin"}, description = "Read message from standard input")
  private boolean readFromStdin = false;

  @Option(names = {"-i", "--interactive"}, description = "Start in interactive mode")
  private boolean interactive = false;
  
  @Option(names = {"--debug"}, description = "Enable debug logging")
  private boolean debug = false;

  @Parameters(paramLabel = "MESSAGE", description = "Message to send", arity = "0..1")
  private String message;

  private LinkedList<ChatMessage> history = new LinkedList<>();
  private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
  private Path stateDir = Paths.get(System.getProperty("user.home"), ".manorrock", "assistant", "cli-state");

  public CLI() {
    assistance = new Assistant();
    chatMemory = MessageWindowChatMemory.builder()
        .maxMessages(MEMORY_WINDOW_SIZE)
        .build();
  }

  public static void main(String[] args) {
    // Configure default logging to only show warnings and errors
    configureLogging(Level.WARNING);
    
    int exitCode = new CommandLine(new CLI()).execute(args);
    System.exit(exitCode);
  }
  
  /**
   * Configure logging with the specified level.
   *
   * @param level The logging level to set
   */
  private static void configureLogging(Level level) {
    // Set root logger level
    Logger rootLogger = LogManager.getLogManager().getLogger("");
    rootLogger.setLevel(level);
    
    // Set console handler level
    for (Handler handler : rootLogger.getHandlers()) {
      if (handler instanceof ConsoleHandler) {
        handler.setLevel(level);
      }
    }
  }

  @Override
  public Integer call() throws Exception {
    // Set debug mode if requested
    if (debug) {
      configureLogging(Level.INFO);
      LOGGER.info("Debug logging enabled");
    }
    
    loadState();
    if (config == null) {
      config = LlmConfiguration.defaultConfig();
    }
    
    // Initialize tool manager and register default tools
    initializeToolManager();
    
    assistance.getCommandRegistry().registerCommand("help", new HelpCommand());
    
    // Register ONLY the consolidated LLM command - no individual LLM subcommands
    assistance.getCommandRegistry().registerCommand("llm", 
        new com.manorrock.assistant.command.LlmCommand(
            () -> config, 
            newConfig -> { 
                config = newConfig;
                saveState();
            }));
        
    assistance.getCommandRegistry().registerCommand("source",
        new SourceCommand(this::handleSendAction, this::handleCommand));
    assistance.getCommandRegistry().registerCommand("new", new NewCommand(this::startNewSession));
    
    // Register the Ollama command with config supplier
    assistance.getCommandRegistry().registerCommand("ollama", new OllamaCommand(() -> config));
    
    // Register the explain command with the message processor
    assistance.getCommandRegistry().registerCommand("explain", new CLIExplainCommand(this::processMessage));
    
    // Register the tool command with integration support - use toolManager directly
    assistance.getCommandRegistry().registerCommand("tool", new ToolCommand(
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
    
    // Add debug logging - this will only show if debug flag is enabled
    LOGGER.info("Initializing tool manager...");
    
    // Register default tools
    registerTool(new FileReadTool());
    registerTool(new FileWriteTool());
    registerTool(new DirectoryListTool());
    registerTool(new ShellExecutionTool());
    registerTool(new ProcessExecutionTool());
    registerTool(new ProjectStructureAnalysisTool());
    registerTool(new DependencyAnalysisTool());
    registerTool(new WebScraperTool());
    registerTool(new MavenArchetypeTool());
    
    // Discover and register custom tools
    discoverAndRegisterCustomTools();
    
    // Log registered tools - only visible in debug mode
    LOGGER.info("Registered tools: " + 
        toolManager.getAvailableTools().stream()
            .map(Tool::getName)
            .collect(Collectors.joining(", ")));
  }
  
  /**
   * Register a tool with the tool manager.
   * 
   * @param tool The tool to register
   * @return true if the tool was registered successfully, false otherwise
   */
  private boolean registerTool(Tool tool) {
    try {
      // Validate tool before registration
      validateTool(tool);
      
      // Use the toolManager for registration
      toolManager.registerTool(tool);
      LOGGER.info("Registered tool: " + tool.getName());
      return true;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to register tool: " + 
              (tool != null ? tool.getName() : "null") + " - " + e.getMessage(), e);
      return false;
    }
  }
  
  /**
   * Validate a tool before registration.
   * 
   * @param tool The tool to validate
   * @throws IllegalArgumentException If the tool is invalid
   */
  private void validateTool(Tool tool) {
    if (tool == null) {
        throw new IllegalArgumentException("Tool cannot be null");
    }
    
    if (tool.getName() == null || tool.getName().trim().isEmpty()) {
        throw new IllegalArgumentException("Tool name cannot be null or empty");
    }
    
    if (tool.getDescription() == null || tool.getDescription().trim().isEmpty()) {
        throw new IllegalArgumentException("Tool description cannot be null or empty");
    }
    
    if (tool.getParameters() == null) {
        throw new IllegalArgumentException("Tool parameters cannot be null");
    }
    
    // Check for duplicate tool names - use the tool manager's available tools
    if (toolManager.findTool(tool.getName()).isPresent()) {
        throw new IllegalArgumentException("Tool with name '" + tool.getName() + "' is already registered");
    }
  }
  
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
        LOGGER.info("Discovered and registered tool via ServiceLoader: " + tool.getName());
      }
    }
    
    // Look for tools in custom directories
    String defaultToolDir = System.getProperty("user.home") + "/.manorrock/assistant/tools";
    
    // Check for additional tool directories from system property
    String toolDirsProp = System.getProperty("manorrock.assistant.tool.dirs");
    List<String> toolDirs = new ArrayList<>();
    toolDirs.add(defaultToolDir);
    
    if (toolDirsProp != null && !toolDirs.isEmpty()) {
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
          LOGGER.info("Discovered and registered " + dirCount + " tools from " + toolDir);
        }
      } else {
        // Create the directory if it doesn't exist
        if (!dir.exists() && toolDir.equals(defaultToolDir)) {
          try {
            Files.createDirectories(dir.toPath());
            LOGGER.info("Created tool directory: " + toolDir);
          } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create tool directory: " + toolDir, e);
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
          LOGGER.log(Level.FINE, "Error loading tools from JAR: " + jarFile.getName(), e);
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
          LOGGER.log(Level.WARNING, "Error loading tool from JSON: " + jsonFile.getName(), e);
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
            LOGGER.log(Level.WARNING, "Error loading script tool: " + scriptFile.getName(), e);
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
          LOGGER.info("Registered tool from JAR " + jarFile.getName() + ": " + tool.getName());
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
    // Route all commands through the command registry
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

    Command cmd = assistance.getCommandRegistry().getCommand(cmdName);
    if (cmd != null) {
      System.out.println("System: " + cmd.executeToString(cmdArgs));
    } else {
      System.out.println("System: Unknown command. Type /help for a list of commands.");
    }
    
    // Save state if we've potentially modified LLM configuration
    if (cmdName.equals("llm")) {
      saveState();
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

  private void showHelp() {
    Command helpCommand = assistance.getCommandRegistry().getCommand("help");
    if (helpCommand != null) {
      System.out.println(helpCommand.executeToString(""));
    }
    
    // Get the explain command description for help text
    Command explainCommand = assistance.getCommandRegistry().getCommand("explain");
    String explainDescription = explainCommand != null ? explainCommand.getDescription() : 
                             "Explain text from clipboard or specified file";
    
    String cliHelp = 
        "/explain [file_path] - " + explainDescription + "\n" +
        "/new - Start a new chat session\n" +
        "/exit - Exit interactive mode";
    System.out.println(cliHelp);
  }

  /**
   * Creates a chat language model based on current configuration.
   * Configures model-specific settings for:
   * - OLLAMA: Uses baseUrl, model name, timeout, temperature
   * - OPENAI: Uses API key, model name, timeout, temperature
   * - AZURE_OPENAI: Uses endpoint, API key, deployment name, timeout, temperature
   * 
   * @return Configured ChatLanguageModel instance
   * @throws IllegalArgumentException if vendor is unknown
   */
  private ChatLanguageModel createChatModel() {
    String vendor = config.vendor();
    return switch (vendor.toUpperCase()) {
      case "OLLAMA" -> OllamaChatModel.builder()
          .baseUrl(config.endpoint().substring(0, config.endpoint().lastIndexOf("/api/chat")))
          .modelName(config.model())
          .timeout(TIMEOUT)
          .temperature(config.temperature())
          .build();
      case "OPENAI" -> OpenAiChatModel.builder()
          .apiKey(config.apiKey())
          .modelName(config.model())
          .timeout(TIMEOUT)
          .temperature(config.temperature())
          .build();
      case "AZURE_OPENAI" -> AzureOpenAiChatModel.builder()
          .endpoint(config.endpoint())
          .apiKey(config.apiKey())
          .deploymentName(config.model())
          .timeout(TIMEOUT)
          .temperature(config.temperature())
          .build();
      default -> throw new IllegalArgumentException("Unknown vendor: " + vendor);
    };
  }

  private void processMessage(String message) {
    String timestamp = LocalDateTime.now().format(formatter);
    try {
      // Add user message to chat memory
      UserMessage userMessage = UserMessage.from(message);
      chatMemory.add(userMessage);
      
      // Create chat model
      ChatLanguageModel chatModel = createChatModel();
      
      // Build tool specifications if tool integration is enabled
      List<ToolSpecification> toolSpecifications = Collections.emptyList();
      if (useToolIntegration) {
        toolSpecifications = buildToolSpecifications();
        
        // Add system message for tool-enabled interactions
        chatMemory.add(SystemMessage.from(
            "You are a helpful assistant with access to tools. When appropriate, use tools to accomplish tasks. " +
            "Always think step by step and explain your reasoning clearly."));
      }
      
      // Create initial request with chat memory and tool specifications
      ChatRequest initialRequest = ChatRequest.builder()
          .messages(chatMemory.messages())
          .toolSpecifications(toolSpecifications)
          .build();
      
      // Get initial response from the model
      ChatResponse initialResponse = chatModel.chat(initialRequest);
      AiMessage aiMessage = initialResponse.aiMessage();
      
      // Display the initial response if it has content
      if (aiMessage.text() != null && !aiMessage.text().isEmpty()) {
          System.out.println("Assistant: " + aiMessage.text());
      }
      
      // Handle tool execution if needed
      if (aiMessage.hasToolExecutionRequests()) {
          System.out.println("\nExecuting tools to help answer your question...");
          
          // Add the AI message with tool requests to chat memory
          chatMemory.add(aiMessage);
          
          // Process each tool execution request
          for (var toolRequest : aiMessage.toolExecutionRequests()) {
              try {
                  // Display tool execution details
                  System.out.println("\nTool");
                  System.out.println("  Name: " + toolRequest.name());
                  
                  // Map and validate arguments
                  Map<String, Object> mappedArgs;
                  try {
                      mappedArgs = mapToolArguments(toolRequest.name(), toolRequest.arguments());
                  } catch (IllegalArgumentException e) {
                      System.out.println("  ✗ Invalid arguments: " + e.getMessage());
                      throw e;
                  }
                  
                  // Display mapped arguments
                  System.out.println("  Arguments: " + mappedArgs.entrySet().stream()
                      .map(e -> e.getKey() + "=" + e.getValue())
                      .collect(Collectors.joining(", ")));
                  
                  // Execute the tool with mapped arguments
                  ToolResult result = executeToolWithParams(toolRequest.name(), mappedArgs);
                  
                  // Display execution result
                  if (result.success()) {
                      System.out.println("  ✓ Tool execution successful");
                      if (result.getData() != null) {
                          System.out.println("  Output:");
                          System.out.println(result.getData().toString()
                              .lines()
                              .map(line -> "    " + line)
                              .collect(Collectors.joining("\n")));
                      }
                  } else {
                      System.out.println("  ✗ Tool execution failed: " + result.getMessage());
                  }
                  
                  // Create a JSON object containing both status and result data
                  JSONObject resultJson = new JSONObject();
                  resultJson.put("status", result.success() ? "success" : "error");
                  resultJson.put("status", result.success() ? "success" : "error");
                  resultJson.put("message", result.getMessage());
                  
                  if (result.getData() != null) {
                      resultJson.put("data", result.getData());
                  }
                  
                  // Create tool execution result message and add to chat memory
                  ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(
                      toolRequest,
                      resultJson.toString()
                  );
                  chatMemory.add(resultMessage);
              } catch (Exception e) {
                  System.out.println("  ✗ Tool execution error: " + e.getMessage());
                  
                  // Handle any errors during tool execution
                  JSONObject errorJson = new JSONObject();
                  errorJson.put("status", "error");
                  errorJson.put("message", e.getMessage());
                  
                  ToolExecutionResultMessage errorMessage = ToolExecutionResultMessage.from(
                      toolRequest,
                      errorJson.toString()
                  );
                  chatMemory.add(errorMessage);
              }
          }
          
          // Create follow-up request with updated chat memory
          ChatRequest followUpRequest = ChatRequest.builder()
              .messages(chatMemory.messages())
              .toolSpecifications(toolSpecifications)
              .build();
          
          try {
              // Get follow-up response with tool results included
              ChatResponse followUpResponse = chatModel.chat(followUpRequest);
              AiMessage followUpMessage = followUpResponse.aiMessage();
              
              // Display the follow-up response
              if (followUpMessage.text() != null && !followUpMessage.text().isEmpty()) {
                  System.out.println("\nAssistant: " + followUpMessage.text());
              }
              
              // Add the final response to chat memory
              chatMemory.add(followUpMessage);
          } catch (Exception e) {
              System.out.println("\nError processing tool results: " + e.getMessage());
              LOGGER.log(Level.WARNING, "Error in follow-up response", e);
          }
      } else {
          // For responses without tool requests, simply add to chat memory
          chatMemory.add(aiMessage);
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
  
  /**
   * Builds tool specifications from available tools.
   * 
   * @return List of tool specifications for LangChain4j
   */
  private List<ToolSpecification> buildToolSpecifications() {
    return toolManager.getAvailableTools().stream()
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
          
          // Build and return the tool specification
          return toolSpecBuilder
              .parameters(schemaBuilder.build())
              .build();
      })
      .collect(Collectors.toList());
  }

  /**
   * Maps and validates tool execution request arguments to tool parameters.
   * 
   * @param toolName Name of the tool
   * @param requestArgs Raw arguments from the ToolExecutionRequest
   * @return Mapped and validated arguments for the Manorrock Tool
   * @throws IllegalArgumentException if arguments are invalid
   */
  private Map<String, Object> mapToolArguments(String toolName, String requestArgs) {
    // Check if tool exists by trying to find it in available tools
    Tool tool = toolManager.getAvailableTools().stream()
        .filter(t -> t.getName().equals(toolName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));

    Map<String, Object> rawArgs = new JSONObject(requestArgs).toMap();
    Map<String, Object> mappedArgs = new HashMap<>();
    
    // Validate and map each parameter
    tool.getParameters().forEach(param -> {
        String paramName = param.getName();
        Object value = rawArgs.get(paramName);
        
        // Check required parameters
        if (param.isRequired() && value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + paramName);
        }
        
        // Skip if parameter is optional and not provided
        if (value == null) {
            return;
        }
        
        // Type validation and conversion
        try {
            switch (param.getType().toLowerCase()) {
                case "string":
                    mappedArgs.put(paramName, String.valueOf(value));
                    break;
                case "integer":
                case "int":
                    if (value instanceof Number) {
                        mappedArgs.put(paramName, ((Number) value).intValue());
                    } else {
                        mappedArgs.put(paramName, Integer.parseInt(value.toString()));
                    }
                    break;
                case "number":
                case "float":
                case "double":
                    if (value instanceof Number) {
                        mappedArgs.put(paramName, ((Number) value).doubleValue());
                    } else {
                        mappedArgs.put(paramName, Double.parseDouble(value.toString()));
                    }
                    break;
                case "boolean":
                case "bool":
                    if (value instanceof Boolean) {
                        mappedArgs.put(paramName, value);
                    } else {
                        mappedArgs.put(paramName, Boolean.parseBoolean(value.toString()));
                    }
                    break;
                case "map":
                    if (value instanceof Map) {
                        mappedArgs.put(paramName, value);
                    } else if (value instanceof String && ((String) value).trim().isEmpty()) {
                        mappedArgs.put(paramName, new HashMap<>());
                    } else {
                        try {
                            JSONObject jsonObj = new JSONObject(value.toString());
                            mappedArgs.put(paramName, jsonObj.toMap());
                        } catch (Exception e) {
                            throw new IllegalArgumentException(
                                "Invalid map value for parameter '" + paramName + "': " + value);
                        }
                    }
                    break;
                case "list":
                    if (value instanceof List) {
                        mappedArgs.put(paramName, value);
                    } else if (value instanceof String && ((String) value).trim().isEmpty()) {
                        mappedArgs.put(paramName, new ArrayList<>());
                    } else {
                        try {
                            JSONArray jsonArray = new JSONArray(value.toString());
                            List<Object> list = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                list.add(jsonArray.get(i));
                            }
                            mappedArgs.put(paramName, list);
                        } catch (Exception e) {
                            throw new IllegalArgumentException(
                                "Invalid list value for parameter '" + paramName + "': " + value);
                        }
                    }
                    break;
                default:
                    // For unknown types, pass through as string
                    mappedArgs.put(paramName, String.valueOf(value));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid value for parameter '" + paramName + "': " + value);
        }
    });
    
    return mappedArgs;
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
    } catch (IllegalArgumentException e) {
      LOGGER.log(Level.FINE, "Invalid tool or parameters: " + e.getMessage(), e);
      return ToolResult.failure(e.getMessage());
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
        
        // Load history and populate chat memory
        Path historyFile = stateDir.resolve("history.json");
        if (Files.exists(historyFile)) {
          String content = Files.readString(historyFile);
          JSONArray jsonArray = new JSONArray(content);
          for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject msgObj = jsonArray.getJSONObject(i);
            String role = msgObj.getString("role");
            String msgContent = msgObj.getString("content");
            if ("user".equals(role)) {
              chatMemory.add(UserMessage.from(msgContent));
            } else if ("assistant".equals(role)) {
              chatMemory.add(AiMessage.from(msgContent));
            } else if ("system".equals(role)) {
              chatMemory.add(SystemMessage.from(msgContent));
            }
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
      
      // Save chat memory to history file
      Path historyFile = stateDir.resolve("history.json");
      JSONArray historyArray = new JSONArray();
      for (ChatMessage msg : chatMemory.messages()) {
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
    // Clear the chat memory
    chatMemory = MessageWindowChatMemory.builder()
        .maxMessages(MEMORY_WINDOW_SIZE)
        .build();
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

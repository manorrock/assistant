package com.manorrock.assistant.cli;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.core.CoreAssistant;
import com.manorrock.assistant.core.CoreAssistantMessage;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * The CLI for the Manorrock Assistant.
 * 
 * <p>
 * This class provides a command-line interface for interacting with the
 * Manorrock Assistant. It allows users to send messages, receive responses,
 * and manage configurations.
 * </p>
 * 
 * @author Manfred Riem (mriem@manorrock.com)
 */
@Command(name = "assistant-cli", mixinStandardHelpOptions = true, versionProvider = CLI.PropertiesVersionProvider.class, description = "CLI version of the Manorrock Assistant")
public class CLI implements Callable<Integer> {

  /**
   * Stores the logger.
   */
  private static final Logger LOGGER = Logger.getLogger(CLI.class.getName());

  /**
   * Stores the assistant.
   */
  private CoreAssistant coreAssistant;

  /**
   * Stores the command line flag that specifieds to read the message from stdin.
   */
  @Option(names = { "--stdin" }, description = "Read message from standard input")
  private boolean readFromStdin = false;

  /**
   * Stores the command line flag to start in interactive mode.
   */
  @Option(names = { "-i", "--interactive" }, description = "Start in interactive mode")
  private boolean interactive = false;

  /**
   * Stores the command line flag to disable the You/Assistant: prefix.
   */
  @Option(names = { "--no-prefix" }, description = "Do not show You/Assistant: prefix")
  private boolean noPrefix = false;

  /**
   * Stores the command line flag to enable debug logging.
   */
  @Option(names = { "--debug" }, description = "Enable debug logging")
  private boolean debug = false;

  /**
   * Stores the message to send.
   */
  @Parameters(paramLabel = "MESSAGE", description = "Message to send", arity = "0..1")
  private String message;

  /**
   * Constructor.
   */
  public CLI() {
    coreAssistant = new CoreAssistant();
    coreAssistant.setActiveLlm("llama3.2");
    coreAssistant.getCommandRegistry()
        .registerCommand("explain", new CLIExplainCommand(coreAssistant));
  }

  /**
   * Main method to run the CLI.
   * 
   * @param args the command line arguments.
   */
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

    // loadState();
    // if (config == null) {
    // config = LlmConfiguration.defaultConfig();
    // }

    // Initialize tool manager and register default tools
    // initializeToolManager();

    // Register help command with access to the command registry to show dynamic
    // command listing
    // commandRegistry.registerCommand("help", new HelpCommand(commandRegistry));

    // Register ONLY the consolidated LLM command - no individual LLM subcommands
    // commandRegistry.registerCommand("llm",
    // new com.manorrock.assistant.command.LlmCommand(
    // () -> config,
    // newConfig -> {
    // config = newConfig;
    // saveState();
    // }));

    // commandRegistry.registerCommand("source",
    // new SourceCommand(this::handleSendAction, this::handleCommand));
    // commandRegistry.registerCommand("new", new
    // NewCommand(this::startNewSession));

    // // Register the Ollama command with config supplier
    // commandRegistry.registerCommand("ollama", new OllamaCommand(() -> config));

    // // Register the explain command with the message processor
    // commandRegistry.registerCommand("explain", new
    // CLIExplainCommand(this::processMessage));

    // // Register the tool command with integration support - use toolManager
    // directly
    // commandRegistry.registerCommand("tool", new ToolCommand(
    // () -> new ArrayList<>(toolManager.getAvailableTools()),
    // this::executeToolWithParams,
    // (enabled) -> useToolIntegration = enabled,
    // () -> useToolIntegration
    // ));

    if (interactive) {
      // start interactive mode
      startInteractiveMode();
    } else {
      if (readFromStdin) {
        // read the entire message from stdin
        message = new String(System.in.readAllBytes()).trim();
      } else if (!interactive) {
        // no message provided, show help
        message = "/help";
      }
      // process the message
      handleSendAction(message);
    }

    // saveState();
    return 0;
  }

  /**
   * Initialize the tool manager and register default tools.
   */
  // @Deprecated
  // private void initializeToolManager() {
  // toolManager = new DefaultToolManager();

  // // Add debug logging - this will only show if debug flag is enabled
  // LOGGER.info("Initializing tool manager...");

  // // Register default tools
  // registerTool(new FileReadTool());
  // registerTool(new FileWriteTool());
  // registerTool(new DirectoryListTool());
  // registerTool(new ShellExecutionTool());
  // registerTool(new ProcessExecutionTool());
  // registerTool(new ProjectStructureAnalysisTool());
  // registerTool(new DependencyAnalysisTool());
  // registerTool(new WebScraperTool());
  // registerTool(new MavenArchetypeTool());

  // // Discover and register custom tools
  // discoverAndRegisterCustomTools();

  // // Log registered tools - only visible in debug mode
  // LOGGER.info("Registered tools: " +
  // toolManager.getAvailableTools().stream()
  // .map(Tool::getName)
  // .collect(Collectors.joining(", ")));
  // }

  // /**
  // * Register a tool with the tool manager.
  // *
  // * @param tool The tool to register
  // * @return true if the tool was registered successfully, false otherwise
  // */
  // @Deprecated
  // private boolean registerTool(Tool tool) {
  // try {
  // // Validate tool before registration
  // validateTool(tool);

  // // Use the toolManager for registration
  // toolManager.registerTool(tool);
  // LOGGER.info("Registered tool: " + tool.getName());
  // return true;
  // } catch (Exception e) {
  // LOGGER.log(Level.WARNING, "Failed to register tool: " +
  // (tool != null ? tool.getName() : "null") + " - " + e.getMessage(), e);
  // return false;
  // }
  // }

  // /**
  // * Validate a tool before registration.
  // *
  // * @param tool The tool to validate
  // * @throws IllegalArgumentException If the tool is invalid
  // */
  // @Deprecated
  // private void validateTool(Tool tool) {
  // if (tool == null) {
  // throw new IllegalArgumentException("Tool cannot be null");
  // }

  // if (tool.getName() == null || tool.getName().trim().isEmpty()) {
  // throw new IllegalArgumentException("Tool name cannot be null or empty");
  // }

  // if (tool.getDescription() == null || tool.getDescription().trim().isEmpty())
  // {
  // throw new IllegalArgumentException("Tool description cannot be null or
  // empty");
  // }

  // if (tool.getParameters() == null) {
  // throw new IllegalArgumentException("Tool parameters cannot be null");
  // }

  // // Check for duplicate tool names - use the tool manager's available tools
  // if (toolManager.findTool(tool.getName()).isPresent()) {
  // throw new IllegalArgumentException("Tool with name '" + tool.getName() + "'
  // is already registered");
  // }
  // }

  // /**
  // * Discover and register tools from custom directories.
  // *
  // * @return The number of custom tools registered
  // */
  // @Deprecated
  // private int discoverAndRegisterCustomTools() {
  // int count = 0;

  // // Use ServiceLoader to discover tools on the classpath
  // ServiceLoader<Tool> serviceLoader = ServiceLoader.load(Tool.class);
  // for (Tool tool : serviceLoader) {
  // if (registerTool(tool)) {
  // count++;
  // LOGGER.info("Discovered and registered tool via ServiceLoader: " +
  // tool.getName());
  // }
  // }

  // // Look for tools in custom directories
  // String defaultToolDir = System.getProperty("user.home") +
  // "/.manorrock/assistant/tools";

  // // Check for additional tool directories from system property
  // String toolDirsProp = System.getProperty("manorrock.assistant.tool.dirs");
  // List<String> toolDirs = new ArrayList<>();
  // toolDirs.add(defaultToolDir);

  // if (toolDirsProp != null && !toolDirs.isEmpty()) {
  // String[] dirs = toolDirsProp.split(File.pathSeparator);
  // for (String dir : dirs) {
  // if (!dir.trim().isEmpty()) {
  // toolDirs.add(dir.trim());
  // }
  // }
  // }

  // // Scan each directory for tools
  // for (String toolDir : toolDirs) {
  // File dir = new File(toolDir);
  // if (dir.exists() && dir.isDirectory()) {
  // int dirCount = loadToolsFromDirectory(dir);
  // count += dirCount;
  // if (dirCount > 0) {
  // LOGGER.info("Discovered and registered " + dirCount + " tools from " +
  // toolDir);
  // }
  // } else {
  // // Create the directory if it doesn't exist
  // if (!dir.exists() && toolDir.equals(defaultToolDir)) {
  // try {
  // Files.createDirectories(dir.toPath());
  // LOGGER.info("Created tool directory: " + toolDir);
  // } catch (IOException e) {
  // LOGGER.log(Level.WARNING, "Failed to create tool directory: " + toolDir, e);
  // }
  // }
  // }
  // }

  // return count;
  // }

  // /**
  // * Load tools from a directory. The directory can contain:
  // * 1. JAR files with tools that implement the Tool interface
  // * 2. JSON descriptor files for script-based tools
  // *
  // * @param directory The directory to scan for tools
  // * @return The number of tools registered from this directory
  // */
  // @Deprecated
  // private int loadToolsFromDirectory(File directory) {
  // int count = 0;

  // // Look for JAR files
  // File[] jarFiles = directory.listFiles((dir, name) ->
  // name.toLowerCase().endsWith(".jar"));
  // if (jarFiles != null) {
  // for (File jarFile : jarFiles) {
  // try {
  // count += loadToolsFromJar(jarFile);
  // } catch (Exception e) {
  // LOGGER.log(Level.FINE, "Error loading tools from JAR: " + jarFile.getName(),
  // e);
  // }
  // }
  // }

  // // Look for JSON descriptor files
  // File[] jsonFiles = directory.listFiles((dir, name) ->
  // name.toLowerCase().endsWith(".tool.json"));
  // if (jsonFiles != null) {
  // for (File jsonFile : jsonFiles) {
  // try {
  // if (loadToolFromJson(jsonFile)) {
  // count++;
  // }
  // } catch (Exception e) {
  // LOGGER.log(Level.WARNING, "Error loading tool from JSON: " +
  // jsonFile.getName(), e);
  // }
  // }
  // }

  // // Look for script files with associated metadata
  // File[] scriptFiles = directory.listFiles((dir, name) ->
  // name.toLowerCase().endsWith(".sh") ||
  // name.toLowerCase().endsWith(".bat") ||
  // name.toLowerCase().endsWith(".ps1"));
  // if (scriptFiles != null) {
  // for (File scriptFile : scriptFiles) {
  // String baseName = scriptFile.getName().substring(0,
  // scriptFile.getName().lastIndexOf('.'));
  // File metadataFile = new File(directory, baseName + ".metadata.json");
  // if (metadataFile.exists() && metadataFile.isFile()) {
  // try {
  // if (loadScriptTool(scriptFile, metadataFile)) {
  // count++;
  // }
  // } catch (Exception e) {
  // LOGGER.log(Level.WARNING, "Error loading script tool: " +
  // scriptFile.getName(), e);
  // }
  // }
  // }
  // }

  // return count;
  // }

  // /**
  // * Load tools from a JAR file using a URLClassLoader.
  // *
  // * @param jarFile The JAR file to load tools from
  // * @return The number of tools registered from this JAR
  // * @throws Exception If an error occurs while loading tools
  // */
  // @Deprecated
  // private int loadToolsFromJar(File jarFile) throws Exception {
  // int count = 0;
  // try (URLClassLoader classLoader = new URLClassLoader(
  // new URL[] { jarFile.toURI().toURL() },
  // getClass().getClassLoader())) {
  // // Use ServiceLoader with the custom class loader
  // ServiceLoader<Tool> serviceLoader = ServiceLoader.load(Tool.class,
  // classLoader);
  // for (Tool tool : serviceLoader) {
  // if (registerTool(tool)) {
  // count++;
  // LOGGER.info("Registered tool from JAR " + jarFile.getName() + ": " +
  // tool.getName());
  // }
  // }
  // }
  // return count;
  // }

  // /**
  // * Load a tool from a JSON descriptor file.
  // * The JSON format should match the Tool interface.
  // *
  // * @param jsonFile The JSON file describing the tool
  // * @return true if the tool was registered, false otherwise
  // * @throws Exception If an error occurs while loading the tool
  // */
  // @Deprecated
  // private boolean loadToolFromJson(File jsonFile) throws Exception {
  // String json = Files.readString(jsonFile.toPath());
  // JsonNode toolJson = MAPPER.readTree(json);

  // // Create a JSON-based tool implementation
  // Tool tool = new JsonBasedTool(toolJson);

  // return registerTool(tool);
  // }

  // /**
  // * Load a script-based tool using a script file and its metadata.
  // *
  // * @param scriptFile The script file (.sh, .bat, .ps1)
  // * @param metadataFile The JSON metadata file
  // * @return true if the tool was registered, false otherwise
  // * @throws Exception If an error occurs while loading the tool
  // */
  // @Deprecated
  // private boolean loadScriptTool(File scriptFile, File metadataFile) throws
  // Exception {
  // String metadata = Files.readString(metadataFile.toPath());
  // JsonNode metadataJson = MAPPER.readTree(metadata);

  // // Create a script-based tool implementation
  // Tool tool = new ScriptBasedTool(scriptFile, metadataJson);

  // return registerTool(tool);
  // }

  protected void handleSendAction(String userMessage) {
    if (!userMessage.isEmpty()) {
      // Check for "--old " prefix and route it to the old deprecate CLI processing
      // if (userMessage.startsWith("--old ")) {
      // String actualMessage = userMessage.substring(6); // Length of "--old "

      // if (userMessage.startsWith("/")) {
      // handleCommand(actualMessage);
      // return;
      // }
      // processMessage(actualMessage);
      // return;
      // }

      // Create the message for CoreAssistant
      AssistantMessage message = new CoreAssistantMessage(userMessage);

      // Process using the CoreAssistant
      AssistantMessage response = coreAssistant.processMessage(message);

      // Display the response
      if (!noPrefix) {
        System.out.print("Assistant: ");
      }
      System.out.println(response.getContent());
    }
  }

  // @Deprecated
  // protected void handleCommand(String command) {
  // // Route all commands through the command registry
  // String cmdLine = command.substring(1); // remove the leading '/'
  // int spaceIndex = cmdLine.indexOf(' ');

  // String cmdName;
  // String cmdArgs;
  // if (spaceIndex > 0) {
  // cmdName = cmdLine.substring(0, spaceIndex);
  // cmdArgs = cmdLine.substring(spaceIndex + 1).trim();
  // } else {
  // cmdName = cmdLine;
  // cmdArgs = "";
  // }

  // Command cmd = commandRegistry.getCommand(cmdName);
  // if (cmd != null) {
  // String result;
  // result = cmd.execute(cmdArgs);
  // System.out.println("System: " + result);
  // } else {
  // System.out.println("System: Unknown command. Type /help for a list of
  // commands.");
  // }

  // // Save state if we've potentially modified LLM configuration
  // if (cmdName.equals("llm")) {
  // saveState();
  // }
  // }

  // @Deprecated
  // private void showHelp() {
  // Command helpCommand = commandRegistry.getCommand("help");
  // if (helpCommand != null) {
  // try {
  // // Use execute() to get help text directly
  // String result = helpCommand.execute("");
  // System.out.println(result);
  // } catch (Exception e) {
  // System.out.println("Error showing help: " + e.getMessage());
  // }
  // }

  // // Get the explain command description for help text
  // Command explainCommand = commandRegistry.getCommand("explain");
  // String explainDescription = explainCommand != null ?
  // explainCommand.getDescription() :
  // "Explain text from clipboard or specified file";

  // String cliHelp =
  // "/explain [file_path] - " + explainDescription + "\n" +
  // "/new - Start a new chat session\n" +
  // "/exit - Exit interactive mode";
  // System.out.println(cliHelp);
  // }

  // /**
  // * Creates a chat language model based on current configuration.
  // * Configures model-specific settings for:
  // * - OLLAMA: Uses baseUrl, model name, timeout, temperature
  // * - OPENAI: Uses API key, model name, timeout, temperature
  // * - AZURE_OPENAI: Uses endpoint, API key, deployment name, timeout,
  // temperature
  // *
  // * @return Configured ChatLanguageModel instance
  // * @throws IllegalArgumentException if vendor is unknown
  // */
  // @Deprecated
  // private ChatLanguageModel createChatModel() {
  // String vendor = config.vendor();
  // return switch (vendor.toUpperCase()) {
  // case "OLLAMA" -> OllamaChatModel.builder()
  // .baseUrl(config.endpoint().substring(0,
  // config.endpoint().lastIndexOf("/api/chat")))
  // .modelName(config.model())
  // .timeout(TIMEOUT)
  // .temperature(config.temperature())
  // .build();
  // case "OPENAI" -> OpenAiChatModel.builder()
  // .apiKey(config.apiKey())
  // .modelName(config.model())
  // .timeout(TIMEOUT)
  // .temperature(config.temperature())
  // .build();
  // case "AZURE_OPENAI" -> AzureOpenAiChatModel.builder()
  // .endpoint(config.endpoint())
  // .apiKey(config.apiKey())
  // .deploymentName(config.model())
  // .timeout(TIMEOUT)
  // .temperature(config.temperature())
  // .build();
  // default -> throw new IllegalArgumentException("Unknown vendor: " + vendor);
  // };
  // }

  // @Deprecated
  // private void processMessage(String message) {
  // String timestamp = LocalDateTime.now().format(formatter);
  // try {
  // // Add user message to chat memory
  // UserMessage userMessage = UserMessage.from(message);
  // chatMemory.add(userMessage);

  // // Create chat model
  // ChatLanguageModel chatModel = createChatModel();

  // // Build tool specifications if tool integration is enabled
  // List<ToolSpecification> toolSpecifications = Collections.emptyList();
  // if (useToolIntegration) {
  // toolSpecifications = buildToolSpecifications();

  // // Add system message for tool-enabled interactions
  // chatMemory.add(SystemMessage.from(
  // "You are a helpful assistant with access to tools. When appropriate, use
  // tools to accomplish tasks. " +
  // "Always think step by step and explain your reasoning clearly."));
  // }

  // // Create initial request with chat memory and tool specifications
  // ChatRequest initialRequest = ChatRequest.builder()
  // .messages(chatMemory.messages())
  // .toolSpecifications(toolSpecifications)
  // .build();

  // // Get initial response from the model
  // ChatResponse initialResponse = chatModel.chat(initialRequest);
  // AiMessage aiMessage = initialResponse.aiMessage();

  // // Display the initial response if it has content
  // if (aiMessage.text() != null && !aiMessage.text().isEmpty()) {
  // System.out.println("Assistant: " + aiMessage.text());
  // }

  // // Handle tool execution if needed
  // if (aiMessage.hasToolExecutionRequests()) {
  // System.out.println("\nExecuting tools to help answer your question...");

  // // Add the AI message with tool requests to chat memory
  // chatMemory.add(aiMessage);

  // // Process each tool execution request
  // for (var toolRequest : aiMessage.toolExecutionRequests()) {
  // try {
  // // Display tool execution details
  // System.out.println("\nTool");
  // System.out.println(" Name: " + toolRequest.name());

  // // Map and validate arguments
  // Map<String, Object> mappedArgs;
  // try {
  // mappedArgs = mapToolArguments(toolRequest.name(), toolRequest.arguments());
  // } catch (IllegalArgumentException e) {
  // System.out.println(" ✗ Invalid arguments: " + e.getMessage());
  // throw e;
  // }

  // // Display mapped arguments
  // System.out.println(" Arguments: " + mappedArgs.entrySet().stream()
  // .map(e -> e.getKey() + "=" + e.getValue())
  // .collect(Collectors.joining(", ")));

  // // Execute the tool with mapped arguments
  // ToolResult result = executeToolWithParams(toolRequest.name(), mappedArgs);

  // // Display execution result
  // if (result.success()) {
  // System.out.println(" ✓ Tool execution successful");
  // if (result.getData() != null) {
  // System.out.println(" Output:");
  // System.out.println(result.getData().toString()
  // .lines()
  // .map(line -> " " + line)
  // .collect(Collectors.joining("\n")));
  // }
  // } else {
  // System.out.println(" ✗ Tool execution failed: " + result.getMessage());
  // }

  // // Create a JSON object containing both status and result data
  // ObjectNode resultJson = MAPPER.createObjectNode();
  // resultJson.put("status", result.success() ? "success" : "error");
  // resultJson.put("message", result.getMessage());

  // if (result.getData() != null) {
  // // Convert result data to JsonNode
  // JsonNode dataNode = MAPPER.valueToTree(result.getData());
  // resultJson.set("data", dataNode);
  // }

  // // Create tool execution result message and add to chat memory
  // ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(
  // toolRequest,
  // resultJson.toString()
  // );
  // chatMemory.add(resultMessage);
  // } catch (Exception e) {
  // System.out.println(" ✗ Tool execution error: " + e.getMessage());

  // // Handle any errors during tool execution
  // ObjectNode errorJson = MAPPER.createObjectNode();
  // errorJson.put("status", "error");
  // errorJson.put("message", e.getMessage());

  // ToolExecutionResultMessage errorMessage = ToolExecutionResultMessage.from(
  // toolRequest,
  // errorJson.toString()
  // );
  // chatMemory.add(errorMessage);
  // }
  // }

  // // Create follow-up request with updated chat memory
  // ChatRequest followUpRequest = ChatRequest.builder()
  // .messages(chatMemory.messages())
  // .toolSpecifications(toolSpecifications)
  // .build();

  // try {
  // // Get follow-up response with tool results included
  // ChatResponse followUpResponse = chatModel.chat(followUpRequest);
  // AiMessage followUpMessage = followUpResponse.aiMessage();

  // // Display the follow-up response
  // if (followUpMessage.text() != null && !followUpMessage.text().isEmpty()) {
  // System.out.println("\nAssistant: " + followUpMessage.text());
  // }

  // // Add the final response to chat memory
  // chatMemory.add(followUpMessage);
  // } catch (Exception e) {
  // System.out.println("\nError processing tool results: " + e.getMessage());
  // LOGGER.log(Level.WARNING, "Error in follow-up response", e);
  // }
  // } else {
  // // For responses without tool requests, simply add to chat memory
  // chatMemory.add(aiMessage);
  // }
  // } catch (Exception e) {
  // String errorMessage;
  // if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
  // errorMessage = "Request timed out after " + TIMEOUT.getSeconds() + "
  // seconds";
  // } else {
  // errorMessage = "Error: " + e.getMessage();
  // }
  // System.out.println("Assistant: " + errorMessage);
  // System.out.println("[" + timestamp + " - Error]\n" + e.getMessage());
  // }
  // }

  // /**
  // * Maps and validates tool execution request arguments to tool parameters.
  // *
  // * @param toolName Name of the tool
  // * @param requestArgs Raw arguments from the ToolExecutionRequest
  // * @return Mapped and validated arguments for the Manorrock Tool
  // * @throws IllegalArgumentException if arguments are invalid
  // */
  // @Deprecated
  // private Map<String, Object> mapToolArguments(String toolName, String
  // requestArgs) {
  // try {
  // // Check if tool exists by trying to find it in available tools
  // Tool tool = toolManager.getAvailableTools().stream()
  // .filter(t -> t.getName().equals(toolName))
  // .findFirst()
  // .orElseThrow(() -> new IllegalArgumentException("Tool not found: " +
  // toolName));

  // Map<String, Object> rawArgs = MAPPER.readValue(requestArgs, new
  // TypeReference<Map<String, Object>>() {});
  // Map<String, Object> mappedArgs = new HashMap<>();

  // // Validate and map each parameter
  // tool.getParameters().forEach(param -> {
  // String paramName = param.getName();
  // Object value = rawArgs.get(paramName);

  // // Check required parameters
  // if (param.isRequired() && value == null) {
  // throw new IllegalArgumentException("Missing required parameter: " +
  // paramName);
  // }

  // // Skip if parameter is optional and not provided
  // if (value == null) {
  // return;
  // }

  // // Type validation and conversion
  // try {
  // switch (param.getType().toLowerCase()) {
  // case "string":
  // mappedArgs.put(paramName, String.valueOf(value));
  // break;
  // case "integer":
  // case "int":
  // if (value instanceof Number) {
  // mappedArgs.put(paramName, ((Number) value).intValue());
  // } else {
  // mappedArgs.put(paramName, Integer.parseInt(value.toString()));
  // }
  // break;
  // case "number":
  // case "float":
  // case "double":
  // if (value instanceof Number) {
  // mappedArgs.put(paramName, ((Number) value).doubleValue());
  // } else {
  // mappedArgs.put(paramName, Double.parseDouble(value.toString()));
  // }
  // break;
  // case "boolean":
  // case "bool":
  // if (value instanceof Boolean) {
  // mappedArgs.put(paramName, value);
  // } else {
  // mappedArgs.put(paramName, Boolean.parseBoolean(value.toString()));
  // }
  // break;
  // case "map":
  // if (value instanceof Map) {
  // mappedArgs.put(paramName, value);
  // } else if (value instanceof String && ((String) value).trim().isEmpty()) {
  // mappedArgs.put(paramName, new HashMap<>());
  // } else {
  // try {
  // JsonNode jsonObj = MAPPER.readTree(value.toString());
  // mappedArgs.put(paramName, MAPPER.convertValue(jsonObj, Map.class));
  // } catch (Exception e) {
  // throw new IllegalArgumentException(
  // "Invalid map value for parameter '" + paramName + "': " + value);
  // }
  // }
  // break;
  // case "list":
  // if (value instanceof List) {
  // mappedArgs.put(paramName, value);
  // } else if (value instanceof String && ((String) value).trim().isEmpty()) {
  // mappedArgs.put(paramName, new ArrayList<>());
  // } else {
  // try {
  // JsonNode jsonArray = MAPPER.readTree(value.toString());
  // List<Object> list = new ArrayList<>();
  // if (jsonArray.isArray()) {
  // jsonArray.forEach(item -> list.add(MAPPER.convertValue(item, Object.class)));
  // }
  // mappedArgs.put(paramName, list);
  // } catch (Exception e) {
  // throw new IllegalArgumentException(
  // "Invalid list value for parameter '" + paramName + "': " + value);
  // }
  // }
  // break;
  // default:
  // // For unknown types, pass through as string
  // mappedArgs.put(paramName, String.valueOf(value));
  // }
  // } catch (Exception e) {
  // throw new IllegalArgumentException(
  // "Invalid value for parameter '" + paramName + "': " + value);
  // }
  // });

  // return mappedArgs;
  // } catch (JsonProcessingException e) {
  // throw new IllegalArgumentException("Invalid JSON in tool arguments: " +
  // e.getMessage(), e);
  // }
  // }

  private void startInteractiveMode() {
    System.out.println("Entering interactive mode. Type /exit to quit, or /help for commands.");
    System.out.println("Use \\ at end of line for multi-line input.");
    try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
      StringBuilder messageBuilder = new StringBuilder();
      while (true) {
        if (!noPrefix) {
          System.out.print(messageBuilder.length() == 0 ? "\nYou: " : "... ");
        }
        String line = scanner.nextLine();
        String trimmedLine = line.trim();

        // Check for exit command
        if (trimmedLine.equals("/exit")) {
          System.out.println("Exiting interactive mode.");
          break;
        }

        // if (trimmedLine.startsWith("--old ")) {
        // String actualMessage = trimmedLine.substring(6); // Length of "--old "
        // processMessage(actualMessage);
        // continue;
        // }

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
  }

  // @Deprecated
  // private void loadState() {
  // try {
  // if (Files.exists(stateDir)) {
  // Path configFile = stateDir.resolve("config.json");
  // if (Files.exists(configFile)) {
  // String content = Files.readString(configFile);
  // JsonNode configJson = MAPPER.readTree(content);
  // config = new LlmConfiguration(
  // configJson.path("endpoint").asText(),
  // configJson.path("model").asText(),
  // configJson.path("vendor").asText(),
  // configJson.path("apiKey").asText(),
  // configJson.path("temperature").asDouble(0.7));
  // }

  // // Load history and populate chat memory
  // Path historyFile = stateDir.resolve("history.json");
  // if (Files.exists(historyFile)) {
  // try {
  // String content = Files.readString(historyFile);
  // JsonNode jsonArray = MAPPER.readTree(content);
  // if (jsonArray.isArray()) {
  // for (JsonNode msgObj : jsonArray) {
  // try {
  // String role = msgObj.path("role").asText("");
  // // Get content if it exists
  // String msgContent = msgObj.has("content") ? msgObj.path("content").asText("")
  // : "";

  // if ("user".equals(role)) {
  // chatMemory.add(UserMessage.from(msgContent));
  // } else if ("assistant".equals(role)) {
  // chatMemory.add(AiMessage.from(msgContent));
  // } else if ("system".equals(role)) {
  // chatMemory.add(SystemMessage.from(msgContent));
  // }
  // } catch (Exception e) {
  // // Skip this message entry if there's any error parsing it
  // LOGGER.log(Level.WARNING, "Error parsing message entry in history file: " +
  // e.getMessage());
  // }
  // }
  // }
  // } catch (Exception e) {
  // LOGGER.log(Level.WARNING, "Error loading chat history, starting with empty
  // history: " + e.getMessage());
  // // Continue with empty chat history rather than failing
  // }
  // }
  // } else {
  // Files.createDirectories(stateDir);
  // }
  // } catch (IOException e) {
  // System.out.println("Error loading state: " + e.getMessage());
  // }
  // }

  // @Deprecated
  // private void saveState() {
  // try {
  // Path configFile = stateDir.resolve("config.json");
  // ObjectNode configJson = MAPPER.createObjectNode();
  // configJson.put("endpoint", config.endpoint());
  // configJson.put("model", config.model());
  // configJson.put("vendor", config.vendor());
  // configJson.put("apiKey", config.apiKey());
  // configJson.put("temperature", config.temperature());
  // Files.writeString(configFile, MAPPER.writeValueAsString(configJson));

  // // Save chat memory to history file
  // Path historyFile = stateDir.resolve("history.json");
  // ArrayNode historyArray = MAPPER.createArrayNode();
  // for (ChatMessage msg : chatMemory.messages()) {
  // ObjectNode msgObj = MAPPER.createObjectNode();
  // if (msg instanceof UserMessage userMsg) {
  // msgObj.put("role", "user");
  // msgObj.put("content", userMsg.text());
  // } else if (msg instanceof AiMessage aiMsg) {
  // msgObj.put("role", "assistant");
  // msgObj.put("content", aiMsg.text());
  // } else if (msg instanceof SystemMessage sysMsg) {
  // msgObj.put("role", "system");
  // msgObj.put("content", sysMsg.text());
  // }
  // historyArray.add(msgObj);
  // }
  // Files.writeString(historyFile, MAPPER.writeValueAsString(historyArray));
  // } catch (IOException e) {
  // System.out.println("Error saving state: " + e.getMessage());
  // }
  // }

  // @Deprecated
  // protected void startNewSession() {
  // // Clear the chat memory
  // chatMemory = MessageWindowChatMemory.builder()
  // .maxMessages(MEMORY_WINDOW_SIZE)
  // .build();
  // saveState();
  // }

  // /**
  // * Execute a tool with the given name and parameters.
  // * This method is used by the ToolCommand.
  // *
  // * @param toolName Name of the tool to execute
  // * @param parameters Parameters to pass to the tool
  // * @return Result of the tool execution
  // */
  // @Deprecated
  // private ToolResult executeToolWithParams(String toolName, Map<String, Object>
  // parameters) {
  // try {
  // return toolManager.executeTool(toolName, parameters);
  // } catch (IllegalArgumentException e) {
  // LOGGER.log(Level.FINE, "Invalid tool or parameters: " + e.getMessage(), e);
  // return ToolResult.failure(e.getMessage());
  // }
  // }

  // /**
  // * Convert Manorrock tools to LangChain4j ToolSpecifications.
  // * This method creates tool specifications compatible with LLMs that support
  // tools/function calling.
  // *
  // * @return List of ToolSpecification objects for use with the LLM
  // */
  // @Deprecated
  // private List<ToolSpecification> buildToolSpecifications() {
  // List<ToolSpecification> specifications = new ArrayList<>();

  // // Convert each available tool to a ToolSpecification
  // for (Tool tool : toolManager.getAvailableTools()) {
  // try {
  // // Create tool specification builder with name and description
  // ToolSpecification.Builder toolSpecBuilder = ToolSpecification.builder()
  // .name(tool.getName())
  // .description(tool.getDescription());

  // // Create JsonObjectSchema builder for parameters
  // JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();

  // // Process each parameter
  // for (com.manorrock.assistant.api.ToolParameter param : tool.getParameters())
  // {
  // // Add appropriate property type based on parameter type
  // switch (param.getType().toLowerCase()) {
  // case "string":
  // case "text":
  // case "path":
  // case "file":
  // case "dir":
  // case "directory":
  // schemaBuilder.addStringProperty(param.getName(), param.getDescription());
  // break;
  // case "integer":
  // case "int":
  // schemaBuilder.addIntegerProperty(param.getName(), param.getDescription());
  // break;
  // case "number":
  // case "float":
  // case "double":
  // schemaBuilder.addNumberProperty(param.getName(), param.getDescription());
  // break;
  // case "boolean":
  // case "bool":
  // schemaBuilder.addBooleanProperty(param.getName(), param.getDescription());
  // break;
  // case "array":
  // case "list":
  // // For complex types we fall back to string
  // schemaBuilder.addStringProperty(param.getName(), param.getDescription());
  // break;
  // case "object":
  // case "map":
  // // For complex types we fall back to string
  // schemaBuilder.addStringProperty(param.getName(), param.getDescription());
  // break;
  // default:
  // // Default to string for unknown types
  // schemaBuilder.addStringProperty(param.getName(), param.getDescription());
  // }

  // // Mark required parameters
  // if (param.isRequired()) {
  // schemaBuilder.required(param.getName());
  // }
  // }

  // // Build the schema and add it to the tool specification
  // toolSpecBuilder.parameters(schemaBuilder.build());

  // // Build and add the tool specification
  // specifications.add(toolSpecBuilder.build());
  // LOGGER.fine("Created tool specification for: " + tool.getName());
  // } catch (Exception e) {
  // LOGGER.log(Level.WARNING, "Failed to create tool specification for " +
  // tool.getName(), e);
  // }
  // }

  // return specifications;
  // }

  // /**
  // * Convert Manorrock Assistant tool parameter types to JSON Schema types.
  // *
  // * @param paramType The tool parameter type
  // * @return The corresponding JSON Schema type
  // */
  // @Deprecated
  // private String convertParamType(String paramType) {
  // if (paramType == null) {
  // return "string";
  // }

  // return switch (paramType.toLowerCase()) {
  // case "string", "text", "path", "file", "dir", "directory" -> "string";
  // case "int", "integer" -> "integer";
  // case "float", "double", "number" -> "number";
  // case "boolean", "bool" -> "boolean";
  // case "array", "list" -> "array";
  // case "object", "map" -> "object";
  // default -> "string"; // Default to string for unknown types
  // };
  // }

  static class PropertiesVersionProvider implements CommandLine.IVersionProvider {
    public String[] getVersion() throws Exception {
      Properties props = new Properties();
      try (InputStream is = getClass().getClassLoader().getResourceAsStream("version.properties")) {
        if (is != null) {
          props.load(is);
          String version = props.getProperty("version", "unknown");
          // Strip -SNAPSHOT if present
          return new String[] { version.replace("-SNAPSHOT", "") };
        }
      }
      return new String[] { "unknown" };
    }
  }
}

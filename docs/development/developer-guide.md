# Developer's Guide

This guide provides information for developers who want to extend, customize, or contribute to Manorrock Assistant.

## Project Structure

Manorrock Assistant is organized as a multi-module Maven project:

```
manorrock-assistant/
  ├── api/          # Core interfaces defining the contracts
  ├── cli/          # Command-line interface
  ├── core/         # Core implementation of the assistant
  ├── desktop/      # Desktop application
  ├── vscode/       # VS Code extension
  ├── eclipse/      # Eclipse plugin
  ├── intellij/     # IntelliJ IDEA plugin
  ├── netbeans/     # NetBeans module
  └── mobile/       # Mobile application
```

## Building the Project

### Prerequisites

- JDK 17 or later
- Maven 3.8.0 or later
- Node.js and npm (for VS Code extension)
- Gradle (for IntelliJ plugin)

### Building from Source

To build the entire project:

```bash
# Clone the repository
git clone https://github.com/manorrock/assistant.git
cd assistant

# Build with Maven
mvn clean install
```

To build a specific module:

```bash
cd assistant/cli
mvn clean package
```

### Running Tests

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify
```

## Extension Points

Manorrock Assistant provides several extension points for developers:

### 1. LLM Integration

Implement the `Llm` interface to add support for a new LLM provider.

```java
public class CustomLlmImplementation implements Llm {
    
    private Properties properties = new Properties();
    
    @Override
    public void init() {
        // Initialize your LLM connection
    }
    
    @Override
    public void destroy() {
        // Clean up resources
    }
    
    @Override
    public String process(String prompt) {
        // Process the prompt with your custom LLM
        return "Response from custom LLM";
    }
    
    @Override
    public Properties getProperties() {
        return properties;
    }
    
    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
```

Register your custom LLM with the manager:

```java
LlmManager manager = assistant.getLlmManager();
manager.registerLlm("custom-llm", new CustomLlmImplementation());
```

### 2. Command System

Implement the `Command` interface to add a new command:

```java
public class WeatherCommand implements Command {
    
    @Override
    public String execute(String input) {
        // Parse the input and execute the command
        String location = parseLocation(input);
        String weather = fetchWeatherData(location);
        return formatWeatherInfo(weather);
    }
    
    @Override
    public String getDescription() {
        return """
               Gets weather information for a location.
               
               Usage:
                 /weather <location>   - Get current weather for the specified location
               """;
    }
    
    @Override
    public String getShortDescription() {
        return "Gets weather information";
    }
    
    // Helper methods
    private String parseLocation(String input) { /* ... */ }
    private String fetchWeatherData(String location) { /* ... */ }
    private String formatWeatherInfo(String data) { /* ... */ }
}
```

Register your command with the assistant:

```java
CoreAssistant assistant = new CoreAssistant();
assistant.registerCommand("weather", new WeatherCommand());
```

### 3. Tool Framework

Implement the `Tool` interface to create a custom tool for LLM function calling:

```java
public class CalculatorTool implements Tool {
    
    @Override
    public String getName() {
        return "calculator";
    }
    
    @Override
    public String getDescription() {
        return "Performs mathematical calculations";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
            new ToolParameter("expression", "Mathematical expression to evaluate", "string", true)
        );
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            String expression = (String) parameters.get("expression");
            double result = evaluateExpression(expression);
            return new ToolResult(true, result, null);
        } catch (Exception e) {
            return new ToolResult(false, null, "Error: " + e.getMessage());
        }
    }
    
    private double evaluateExpression(String expression) {
        // Implementation of expression evaluation
        return 0.0;
    }
}
```

Register your tool with the tool manager:

```java
ToolManager toolManager = assistant.getToolManager();
toolManager.registerTool(new CalculatorTool());
```

## Core Module Internals

The Core module is the central component of Manorrock Assistant:

### CoreAssistant

The `CoreAssistant` class implements the `Assistant` interface and serves as the main entry point:

```java
// Key fields
private String activeLlm;
private LlmManager llmManager;
private Map<String, Command> commands;

// Key methods
public String getResponse(String prompt) {
    if (isCommand(prompt)) {
        return executeCommand(prompt);
    } else {
        return processWithLlm(prompt);
    }
}

public void registerCommand(String name, Command command) {
    commands.put(name, command);
}
```

### CoreLlm

The `CoreLlm` class implements the `Llm` interface and manages LLM interactions:

```java
// Key fields
private ChatLanguageModel model;
private ChatMemory chatMemory;
private Properties properties;
private boolean functionCallingEnabled;

// Key methods
public String process(String prompt) {
    if (functionCallingEnabled) {
        return processWithTools(prompt);
    } else {
        return processWithoutTools(prompt);
    }
}
```

### LLM Initialization Process

The initialization process for an LLM:

1. Create an instance of `CoreLlm`
2. Set properties for the LLM
3. Call `init()` which:
   - Initializes default properties if not set
   - Creates the appropriate LangChain4j model
   - Initializes chat memory
   - Sets up function calling if enabled
4. Register the LLM with the `LlmManager`

## Creating a New UI

To create a new UI for Manorrock Assistant:

1. Create a new module in the project
2. Add dependencies on the API and Core modules
3. Initialize the CoreAssistant
4. Create UI components that interact with the assistant
5. Handle user input and display responses

Example initialization:

```java
// Initialize the assistant
CoreAssistant assistant = new CoreAssistant();

// Initialize the LLM manager
CoreLlmManager llmManager = new CoreLlmManager(assistant);
assistant.setLlmManager(llmManager);

// Register a default LLM
CoreLlm defaultLlm = new CoreLlm(llmManager);
defaultLlm.init();
llmManager.registerLlm("default", defaultLlm);
assistant.setActiveLlm("default");

// Register built-in commands
assistant.registerCommand("llm", new CoreLlmCommand(assistant));
assistant.registerCommand("help", new HelpCommand(assistant));
```

## IDE Extension Development

### VS Code Extension

The VS Code extension is built using TypeScript and the VS Code Extension API:

1. Core backend is packaged as a JAR
2. TypeScript code communicates with the JAR via JNI or process communication
3. Extension contributes:
   - WebView panel for the assistant
   - Commands for the command palette
   - Context menu actions

### Eclipse Plugin

The Eclipse plugin is built using the Eclipse Plugin Development Environment:

1. Uses OSGi framework for modularity
2. Contributes:
   - Views
   - Commands
   - Preference pages
   - Context menus

### IntelliJ Plugin

The IntelliJ plugin is built using the IntelliJ Platform SDK:

1. Uses Gradle for building
2. Contributes:
   - Tool Windows
   - Actions
   - Settings
   - Intentions

### NetBeans Module

The NetBeans module uses the NetBeans Module System:

1. Uses Maven for building
2. Contributes:
   - Windows
   - Actions
   - Options

## Working with LangChain4j

Manorrock Assistant uses LangChain4j for LLM integration:

```java
// Creating a ChatLanguageModel with LangChain4j
ChatLanguageModel model = OllamaChatModel.builder()
    .baseUrl(properties.getProperty("baseUrl"))
    .modelName(properties.getProperty("modelName"))
    .temperature(temperature)
    .timeout(timeout)
    .build();

// Creating a chat request
ChatRequest request = ChatRequest.builder()
    .messages(
        systemMessage(properties.getProperty("systemMessage")),
        userMessage(prompt)
    )
    .build();

// Processing the request
ChatResponse response = model.chat(request);
```

## Adding Support for a New LLM

To add support for a new LLM vendor:

1. Update the `CoreLlm` class to handle the new vendor in the `initializeChatLanguageModel` method
2. Add a new case to the switch statement:

```java
case "new_vendor":
    model = NewVendorChatModel.builder()
        .apiKey(properties.getProperty("apiKey"))
        .modelName(properties.getProperty("modelName"))
        .temperature(temperature)
        .timeout(timeout)
        .build();
    break;
```

3. Create appropriate LangChain4j integration for the new vendor if not already available

## Debugging

### Logging

Manorrock Assistant uses java.util.logging for logging:

```java
private static final Logger LOGGER = Logger.getLogger(YourClass.class.getName());

// Log a message
LOGGER.info("Processing request");

// Log an error
LOGGER.log(Level.SEVERE, "Error processing request", exception);
```

### Remote Debugging

For remote debugging:

1. Run the application with debug parameters:
   ```
   java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar app.jar
   ```

2. Connect your IDE to port 5005

## Project Conventions

### Code Style

- 4 spaces for indentation
- Line width of 100 characters
- JavaDoc for all public methods and classes
- Use final where appropriate
- Prefer interfaces over concrete types in variable declarations

### Package Structure

```
com.manorrock.assistant.[module].[component]
```

Example:
```
com.manorrock.assistant.core.commands
com.manorrock.assistant.api.tools
```

### Testing

- JUnit 5 for unit tests
- Integration tests with suffix IT (e.g., `CoreLlmIT.java`)
- Mock LLM responses for unit tests

## Releasing

### Version Numbering

Format: `YY.MM.PATCH`
- YY: Year
- MM: Month
- PATCH: Patch number

Example: `23.12.1` for the first patch of December 2023

### Release Process

1. Update version in all `pom.xml` files
2. Run all tests: `mvn verify`
3. Create a tag: `git tag YY.MM.PATCH`
4. Push the tag: `git push origin YY.MM.PATCH`
5. Release artifacts are built by CI/CD

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for your changes
5. Ensure all tests pass
6. Submit a pull request

## Common Tasks

### Adding a New Command

1. Create a class implementing the `Command` interface
2. Implement the required methods
3. Register the command with `CoreAssistant`
4. Add documentation for the command

### Adding a New Tool

1. Create a class implementing the `Tool` interface
2. Implement the required methods
3. Register the tool with the `ToolManager`
4. Enable function calling in the LLM configuration

### Updating LangChain4j

1. Update the dependency version in the root `pom.xml`
2. Adapt code to any API changes
3. Test with different LLM providers

## Resources

- [API Documentation](https://manorrock.github.io/assistant/api/)
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [Source Code Repository](https://github.com/manorrock/assistant)
- [Issue Tracker](https://github.com/manorrock/assistant/issues)

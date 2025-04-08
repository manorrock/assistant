# Core Module

The Core Module is the central component of the Manorrock Assistant system, providing the business logic for managing Large Language Models (LLMs), handling commands, and orchestrating the overall assistant functionality.

## Overview

The Core Module serves as the bridge between the user interfaces (CLI, Desktop, and IDE plugins) and the underlying LLM implementations. It handles:

- LLM management and configuration
- Command processing
- Tool integration for function calling with LLMs
- Template management for system prompts

```mermaid
graph TD
    UI[User Interfaces] -->|use| Core[Core Module]
    Core -->|manages| LLMs[LLM Implementations]
    Core -->|processes| Commands[Command System]
    Core -->|integrates| Tools[Tool Framework]
    LLMs -->|use| LangChain[LangChain4j]
    LLMs -->|support| Ollama[Ollama]
    LLMs -->|support| OpenAI[OpenAI API]
    LLMs -->|support| AzureAI[Azure OpenAI]
```

## Key Components

### CoreAssistant

The `CoreAssistant` class implements the `Assistant` interface and serves as the main entry point for all user interactions. It:

- Manages the active LLM selection
- Processes user prompts and commands
- Routes command execution to appropriate handlers

```mermaid
classDiagram
    class Assistant {
        <<interface>>
        +getActiveLlm() String
        +setActiveLlm(String name)
        +getLlmManager() LlmManager
        +getResponse(String prompt) String
    }
    
    class CoreAssistant {
        -String activeLlm
        -LlmManager llmManager
        -Map~String, Command~ commands
        +CoreAssistant()
        +getActiveLlm() String
        +setActiveLlm(String name)
        +getLlmManager() LlmManager
        +getResponse(String prompt) String
        +registerCommand(String name, Command command)
        +unregisterCommand(String name)
    }
    
    Assistant <|.. CoreAssistant
```

### CoreLlm

The `CoreLlm` class implements the `Llm` interface and provides the core LLM functionality:

- Integration with LangChain4j for different LLM providers
- Support for streaming responses
- Tool integration through function calling
- Chat memory management for context retention

```mermaid
classDiagram
    class Llm {
        <<interface>>
        +init()
        +destroy()
        +process(String prompt) String
        +getProperties() Properties
        +setProperties(Properties props)
    }
    
    class CoreLlm {
        -ChatLanguageModel model
        -ChatMemory chatMemory
        -Properties properties
        -LlmManager manager
        -boolean functionCallingEnabled
        +CoreLlm(LlmManager manager)
        +process(String prompt) String
        -processWithTools(String prompt) String
        -processWithoutTools(String prompt) String
        -buildToolSpecifications() List~ToolSpecification~
        -initializeChatLanguageModel()
        -initializeChatMemory()
        -initializeFunctionCalling()
        -initializeDefaultProperties()
        +isFunctionCallingEnabled() boolean
        +setFunctionCallingEnabled(boolean enabled)
    }
    
    Llm <|.. CoreLlm
```

### CoreLlmManager

The `CoreLlmManager` class implements the `LlmManager` interface and is responsible for:

- Registering and unregistering LLMs
- Providing access to available LLMs
- Managing LLM lifecycle (initialization and destruction)

```mermaid
classDiagram
    class LlmManager {
        <<interface>>
        +getLlm(String name) Llm
        +getLlms() Map~String, Llm~
        +registerLlm(String name, Llm llm)
        +unregisterLlm(String name)
        +getAssistant() Assistant
    }
    
    class CoreLlmManager {
        -Map~String, Llm~ llms
        -Assistant assistant
        +CoreLlmManager(Assistant assistant)
        +getLlm(String name) Llm
        +getLlms() Map~String, Llm~
        +registerLlm(String name, Llm llm)
        +unregisterLlm(String name)
    }
    
    LlmManager <|.. CoreLlmManager
```

### CoreLlmCommand

The `CoreLlmCommand` class implements the `Command` interface and provides the `/llm` command functionality:

- Managing LLM configuration
- Switching between LLMs
- Setting templates for system prompts
- Enabling/disabling function calling

```mermaid
classDiagram
    class Command {
        <<interface>>
        +execute(String input) String
        +getDescription() String
        +getShortDescription() String
    }
    
    class CoreLlmCommand {
        -CoreAssistant assistant
        -Map~String, String~ systemMessageTemplates
        -Path templatesDir
        +CoreLlmCommand(CoreAssistant assistant)
        +execute(String input) String
        -getCurrentConfiguration() String
        -getProperty(String key, String defaultValue) String
        -setModel(String model) String
        -setEndpoint(String endpoint) String
        -setApiKey(String apiKey) String
        -setVendor(String vendor) String
        -setTemperature(String temperature) String
        -setFunctionCalling(String setting) String
        -isFunctionCallingEnabled() boolean
        -resetLlm() String
        -setActiveLlm(String llmName) String
        -listLlms() String
        -addLlm(String name) String
        -removeLlm(String name) String
        -loadTemplatesFromConfig() void
        -loadTemplateFile(Path path) void
    }
    
    Command <|.. CoreLlmCommand
```

## LLM Integration

The Core Module integrates with different LLM providers through LangChain4j. It supports:

1. **Ollama** - Local LLM hosting (default)
2. **OpenAI** - Cloud-based LLM service
3. **Azure OpenAI** - Microsoft's cloud-based LLM service

### Workflow

```mermaid
sequenceDiagram
    participant UI as User Interface
    participant Core as CoreAssistant
    participant Llm as CoreLlm
    participant LC4J as LangChain4j
    participant Provider as LLM Provider
    
    UI->>Core: getResponse(prompt)
    Core->>Llm: process(prompt)
    
    alt Function Calling Enabled
        Llm->>LC4J: chat with tool specifications
        LC4J->>Provider: API request
        Provider->>LC4J: Response with function calls
        LC4J->>Llm: Function call information
        Llm->>Llm: Execute tools
        Llm->>LC4J: Submit tool results
        LC4J->>Provider: Tool results
        Provider->>LC4J: Final response
        LC4J->>Llm: Formatted response
    else Function Calling Disabled
        Llm->>LC4J: Simple chat request
        LC4J->>Provider: API request
        Provider->>LC4J: Response
        LC4J->>Llm: Formatted response
    end
    
    Llm->>Core: Processed response
    Core->>UI: Display response
```

## System Message Templates

The Core Module manages system message templates that can be applied to customize the behavior of the LLM. Templates are stored as JSON files in the user's configuration directory and can be selected using the `/llm systemMessage` command. 

Built-in templates include:
- Default assistant
- Code reviewer
- Design pattern expert
- Performance optimizer
- Security analyst

## Configuration

The Core Module handles the configuration of LLMs through properties:

| Property | Description | Default Value |
|----------|-------------|---------------|
| baseUrl | LLM API endpoint URL | http://localhost:11434 |
| modelName | Name of the LLM model | llama3.2 |
| apiKey | API key for authentication | (empty) |
| vendor | LLM vendor (OLLAMA, OPENAI, AZURE_OPENAI) | ollama |
| temperature | Response randomness (0.0-1.0) | 0.7 |
| maxMessages | Maximum message history | 10 |
| systemMessage | System prompt template | You are a helpful assistant. |
| functionCalling | Enable/disable function calling | false |
| timeout | Request timeout in seconds | 30 |

## Extending the Core Module

### Implementing a Custom LLM

To add a new LLM implementation, you can:

1. Implement the `Llm` interface
2. Register the implementation with `CoreLlmManager`

```java
// Create a custom LLM implementation
public class CustomLlm implements Llm {
    // Implement required methods...
}

// Register with the LLM manager
CoreLlmManager manager = assistant.getLlmManager();
manager.registerLlm("custom-llm", new CustomLlm());
```

### Adding a New Command

To add a new command to the assistant:

1. Implement the `Command` interface
2. Register the command with `CoreAssistant`

```java
// Create a custom command
public class CustomCommand implements Command {
    @Override
    public String execute(String input) {
        // Implementation...
    }
    
    @Override
    public String getDescription() {
        return "Detailed help message for the command";
    }
    
    @Override
    public String getShortDescription() {
        return "Short command description";
    }
}

// Register with the assistant
CoreAssistant assistant = new CoreAssistant();
assistant.registerCommand("custom", new CustomCommand());
```

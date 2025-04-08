# Manorrock Assistant Documentation

> **Note:** This documentation was generated with the assistance of AI tools and represents our understanding of the project at the time of writing. While we strive for accuracy, some sections may describe aspirational functionality or contain unintentional inaccuracies. Manorrock Assistant is an open-source project, and we welcome contributions from the community to improve both the software and its documentation. If you find errors or wish to enhance these materials, please consider submitting a pull request.

> **Disclaimer:** This software is provided "as is", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose and noninfringement. In no event shall the authors or copyright holders be liable for any claim, damages or other liability, whether in an action of contract, tort or otherwise, arising from, out of or in connection with the software or the use or other dealings in the software. See the [LICENSE](../LICENSE) file for complete terms.

## Project Overview

Manorrock Assistant is a versatile platform that exposes Large Language Models through a chat-like interface across various environments. The project was initially developed using AI assistance as a proof of concept to explore the potential of AI-assisted development.

## Project Architecture

The project follows a modular architecture with clear separation of concerns. The following diagram illustrates the high-level architecture:

```mermaid
graph TD
    User[User] -->|interacts with| UI[User Interface]
    UI -->|implemented by| CLI[CLI Module]
    UI -->|implemented by| Desktop[Desktop Module]
    UI -->|implemented by| IDEs[IDE Extensions]
    IDEs -->|includes| VS[VS Code Extension]
    IDEs -->|includes| IJ[IntelliJ Plugin]
    IDEs -->|includes| EC[Eclipse Plugin]
    IDEs -->|includes| NB[NetBeans Plugin]
    CLI -->|depends on| Core[Core Module]
    Desktop -->|depends on| Core
    IDEs -->|depend on| Core
    Core -->|uses| API[API Module]
    Core -->|manages| LLM[LLM Integration]
    LLM -->|supports| Ollama[Ollama]
    LLM -->|supports| OpenAI[OpenAI]
    LLM -->|supports| AzureOpenAI[Azure OpenAI]
    Core -->|provides| Tools[Tool Framework]
```

## Module Structure

Manorrock Assistant is organized into the following key modules:

| Module | Description |
|--------|-------------|
| [api](/docs/api/README.md) | Core API interfaces and classes that define the contract between components |
| [core](/docs/core/README.md) | Core implementation providing the central business logic and orchestration |
| [cli](/docs/cli/README.md) | Command-line interface implementation |
| [desktop](/docs/desktop/README.md) | Desktop application implementation |
| [eclipse](/docs/eclipse/README.md) | Eclipse plugin implementation |
| [intellij](/docs/intellij/README.md) | IntelliJ IDEA plugin implementation |
| [mobile](/docs/mobile/README.md) | Mobile application implementation |
| [netbeans](/docs/netbeans/README.md) | NetBeans IDE plugin implementation |
| [vscode](/docs/vscode/README.md) | Visual Studio Code extension implementation |
| [mobile](/docs/mobile/README.md) | Mobile application implementation |
| [netbeans](/docs/netbeans/README.md) | NetBeans IDE plugin implementation |
| [vscode](/docs/vscode/README.md) | Visual Studio Code extension implementation |

## Workflow Diagrams

### LLM Processing Workflow

```mermaid
sequenceDiagram
    participant User
    participant UI as User Interface
    participant Core as Core Module
    participant LLM as LLM Manager
    participant Model as LLM Model
    participant Tools as Tool Framework
    
    User->>UI: Enters Prompt
    UI->>Core: Sends Prompt
    Core->>LLM: Processes with Active LLM
    
    alt Function Calling Enabled
        LLM->>Model: Send Prompt with Tool Specs
        Model->>LLM: Response with Tool Calls
        LLM->>Tools: Execute Tool
        Tools->>LLM: Return Tool Results
        LLM->>Model: Send Tool Results
        Model->>LLM: Final Response
    else Function Calling Disabled
        LLM->>Model: Send Plain Prompt
        Model->>LLM: Plain Response
    end
    
    LLM->>Core: Return Response
    Core->>UI: Format Response
    UI->>User: Display Response
```

### Command Processing Workflow

```mermaid
sequenceDiagram
    participant User
    participant UI as User Interface
    participant Core as Core Module
    participant CMD as Command Handler
    
    User->>UI: Enters Command
    UI->>Core: Passes Command
    Core->>Core: Identifies Command
    Core->>CMD: Routes to Handler
    CMD->>Core: Returns Result
    Core->>UI: Returns Formatted Output
    UI->>User: Displays Result
```

## Component Architecture

### LLM Integration

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
        +isFunctionCallingEnabled() boolean
        +setFunctionCallingEnabled(boolean enabled)
    }
    
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
    
    Llm <|.. CoreLlm
    LlmManager <|.. CoreLlmManager
    CoreLlm --> LlmManager
    CoreLlmManager --> Llm
```

### Command System

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
        -setFunctionCalling(String setting) String
        -isFunctionCallingEnabled() boolean
        -resetLlm() String
        -setActiveLlm(String llmName) String
        -listLlms() String
        -addLlm(String name) String
        -removeLlm(String name) String
    }
    
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
        +CoreAssistant()
        +getActiveLlm() String
        +setActiveLlm(String name)
        +getLlmManager() LlmManager
        +getResponse(String prompt) String
    }
    
    Command <|.. CoreLlmCommand
    CoreLlmCommand --> CoreAssistant
    Assistant <|.. CoreAssistant
    CoreAssistant --> LlmManager
```

## Extension Points

Manorrock Assistant provides several extension points for developers who wish to enhance or customize its functionality:

1. **LLM Integration**: Implement the `Llm` interface to add support for new Large Language Models.
2. **Command System**: Implement the `Command` interface to add new commands.
3. **Tool Framework**: Implement the `Tool` interface to add new capabilities for LLMs.

### LLM Extension Example

```mermaid
graph TD
    A[Custom LLM] -->|implements| B[Llm Interface]
    A -->|registers with| C[LlmManager]
    C -->|makes available to| D[Assistant]
    E[User] -->|interacts with| D
    D -->|uses| A
```

## Developer's Guide

See the dedicated guides for developers who want to extend or contribute to Manorrock Assistant:

- [Building from Source](/docs/development/building.md)
- [Adding a New LLM](/docs/development/adding-llm.md)
- [Implementing a New Command](/docs/development/adding-command.md)
- [Creating Custom Tools](/docs/development/developer-guide.md#3-tool-framework)
- [IDE Integration](/docs/development/ide-integration.md)

## User's Guide

- [Installation](/docs/user/installation.md)
- [Getting Started](/docs/user/getting-started.md)
- [Command Reference](/docs/user/command-reference.md)
- [Configuration](/docs/user/configuration.md)
- [Using LLMs](/docs/user/using-llms.md)

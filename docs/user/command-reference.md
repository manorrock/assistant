# Command Reference

This document provides a comprehensive reference for all commands available in Manorrock Assistant across different interfaces.

## Command Format

Commands in Manorrock Assistant:
- Begin with a forward slash (`/`)
- May include subcommands and arguments
- Are consistent across CLI, Desktop, and IDE interfaces

## Core Commands

### Help Command

```
/help
```

Displays a list of all available commands with brief descriptions.

```
/help [command]
```

Displays detailed help for a specific command.

### LLM Management

#### LLM Information

```
/llm
```

Shows help for LLM commands.

```
/llm info
```

Displays the current LLM configuration, including:
- Vendor (OLLAMA, OPENAI, AZURE_OPENAI)
- Model name
- Endpoint URL
- API key (masked)
- Temperature setting
- Function calling status

#### LLM Configuration

```
/llm list
```

Lists all configured LLMs with their basic information.

```
/llm add <name>
```

Adds a new LLM configuration with the specified name.

```
/llm remove <name>
```

Removes the LLM configuration with the specified name.

```
/llm use <name>
```

Sets the specified LLM as the active one.

```
/llm reset
```

Resets the current LLM, clearing its memory.

#### LLM Settings

```
/llm vendor <vendor>
```

Sets the LLM vendor. Supported values:
- `ollama` (default, local)
- `openai` (cloud)
- `azure_openai` (Azure cloud)

```
/llm model <model>
```

Sets the model name to use. Examples:
- For Ollama: `llama3.2` (default), `mixtral`, `gemma`
- For OpenAI: `gpt-4`, `gpt-4-turbo`, `gpt-3.5-turbo`
- For Azure: Deployment name

```
/llm apiKey <key>
```

Sets the API key for authentication (required for OpenAI and Azure).

```
/llm endpoint <url>
```

Sets the endpoint URL:
- Ollama default: `http://localhost:11434`
- OpenAI default: `https://api.openai.com`
- Azure: Your deployment endpoint

```
/llm temperature <value>
```

Sets the temperature parameter (0.0-1.0) controlling randomness.
- Lower values (e.g., 0.2): More deterministic
- Higher values (e.g., 0.8): More creative
- Default: 0.7

```
/llm functionCalling <on|off>
```

Enables or disables function calling (tool integration).

```
/llm systemMessage <template>
```

Sets the system message template. Built-in options:
- `default`: General assistant
- `code-review`: Code reviewer perspective
- `optimize`: Performance optimization expert
- `security`: Security specialist
- `design-patterns`: Design pattern expert
- Custom templates from your templates directory

### Session Management

```
/session
```

Shows help for session commands.

```
/session clear
```

Clears the current conversation history.

```
/session save [name]
```

Saves the current conversation with an optional name.

```
/session load <name>
```

Loads a previously saved conversation.

```
/session list
```

Lists all saved conversations.

```
/session delete <name>
```

Deletes a saved conversation.

### Context Management

```
/context
```

Shows help for context commands.

```
/context add <file>
```

Adds the content of the specified file as context.

```
/context list
```

Lists all currently active context files.

```
/context remove <file>
```

Removes a specific context file.

```
/context clear
```

Clears all context files.

### Code Commands

```
/explain
```

Explains the code in the current clipboard or the code that follows the command.

```
/code
```

Formats the response as code (useful for code generation).

## Tool Framework Commands

```
/tool
```

Shows help for tool commands.

```
/tool list
```

Lists all available tools.

```
/tool info <name>
```

Shows information about a specific tool.

```
/tool run <name> [parameters]
```

Runs a specific tool with optional parameters.

## Advanced Commands

### Ollama Integration

```
/ollama
```

Shows help for Ollama commands.

```
/ollama list
```

Lists all available Ollama models.

```
/ollama pull <model>
```

Pulls a new model from Ollama.

```
/ollama status
```

Shows the status of the Ollama server.

## Interface-Specific Commands

### CLI-Specific Commands

```
/exit
```

Exits the CLI application.

```
/clear
```

Clears the terminal screen.

### Desktop-Specific Commands

```
/theme <light|dark|system>
```

Changes the application theme (Desktop only).

```
/export <format>
```

Exports the conversation in the specified format (`txt`, `md`, `html`).

### IDE-Specific Commands

```
/jump <file>:<line>
```

Jumps to the specified file and line in the IDE.

```
/find <text>
```

Searches for text in the current project.

## Command Examples

### Basic Interaction

```
/help
/llm info
Hello, who are you?
```

### Configuring for OpenAI

```
/llm vendor openai
/llm apiKey your-api-key-here
/llm model gpt-4
/llm temperature 0.5
/llm info
```

### Working with Sessions

```
/session clear
Tell me about Java programming
/session save java-intro
/session list
/session clear
/session load java-intro
```

### Using Context

```
/context add project/src/main/java/App.java
Explain the main functionality of this code
/context clear
```

### Tool Usage

```
/tool list
/tool run calculator "5 + (3 * 4)"
/tool info weather
/tool run weather city=New York
```

## Environment Variables

Manorrock Assistant recognizes certain environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `OLLAMA_HOST` | Hostname for Ollama server | localhost |
| `OPENAI_API_KEY` | OpenAI API key | (none) |
| `AZURE_OPENAI_API_KEY` | Azure OpenAI API key | (none) |
| `AZURE_OPENAI_ENDPOINT` | Azure OpenAI endpoint | (none) |
| `MANORROCK_CONFIG_DIR` | Configuration directory | ~/.manorrock/assistant |

## Configuration Files

Commands manipulate configuration files stored in:
```
~/.manorrock/assistant/
  ├── config.properties  # General configuration
  ├── llms/              # LLM configurations
  ├── sessions/          # Chat session history
  ├── contexts/          # Context files
  └── templates/         # System message templates
```

## Troubleshooting Commands

```
/version
```

Displays the current version of Manorrock Assistant.

```
/debug
```

Toggles debug mode for additional logging.

```
/status
```

Shows the status of the LLM connection and other components.

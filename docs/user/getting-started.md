# Getting Started with Manorrock Assistant

This guide will help you get up and running with Manorrock Assistant, including installation, initial setup, and basic usage.

## Installation Options

Manorrock Assistant is available in multiple forms. Choose the option that best fits your workflow:

### Command Line Interface (CLI)

For terminal users who prefer a text-based interface:

```bash
# One-line installation script (macOS/Linux)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/manorrock/assistant/current/install.sh)"

# After installation, run:
manorrock-assistant
```

### Desktop Application

For users who prefer a graphical interface:

1. Download the latest JAR file from [GitHub releases](https://github.com/manorrock/assistant/releases/latest)
2. Ensure Java 17+ is installed on your system
3. Double-click the JAR file or run:
   ```
   java -jar manorrock-assistant-desktop.jar
   ```

### IDE Extensions

For developers who want AI assistance within their IDE:

- **VS Code**: Install from the [VS Code Marketplace](https://marketplace.visualstudio.com/items?itemName=manorrock.assistant)
- **IntelliJ IDEA**: Install from [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/manorrock-assistant)
- **Eclipse**: Install from [Eclipse Marketplace](https://marketplace.eclipse.org/content/manorrock-assistant)
- **NetBeans**: Install from the NetBeans Plugin Portal

## Prerequisites

Manorrock Assistant requires:

1. **Java Runtime Environment (JRE) 17+**: Required for all interfaces
2. **LLM Backend**: Default is Ollama (local) but can use OpenAI or Azure OpenAI

### Setting Up Ollama (Recommended for Local Use)

Ollama allows you to run LLMs locally on your machine:

1. Download and install [Ollama](https://ollama.ai/download)
2. Pull a model (one-time setup):
   ```bash
   ollama pull llama3.2
   ```
3. Start the Ollama server:
   ```bash
   ollama serve
   ```

## First-Time Setup

When you first launch Manorrock Assistant, follow these steps:

1. **LLM Configuration**:
   - The default configuration uses Ollama with the llama3.2 model
   - To view current configuration in CLI/Desktop: 
     ```
     /llm info
     ```
   - To configure for OpenAI:
     ```
     /llm vendor openai
     /llm apiKey your-api-key-here
     /llm model gpt-4
     ```

2. **Test Connection**:
   - Once configured, test with a simple prompt:
     ```
     Hello, who are you?
     ```
   - You should receive a response from the configured LLM

## Basic Usage

### Chat Interface

The primary way to interact is through the chat interface:

1. **Ask Questions**: Type your question and press Enter/Send
2. **Use Commands**: Commands start with `/` (e.g., `/help`)

### Key Commands

All interfaces support these core commands:

| Command | Description | Example |
|---------|-------------|---------|
| `/help` | Show available commands | `/help` |
| `/llm` | Manage LLM configuration | `/llm list` |
| `/llm info` | Show current LLM configuration | `/llm info` |
| `/llm list` | List available LLMs | `/llm list` |
| `/llm add` | Add a new LLM configuration | `/llm add openai-gpt4` |
| `/session clear` | Clear the current conversation | `/session clear` |
| `/session save` | Save the current conversation | `/session save meeting-notes` |

### Context Management

Manorrock Assistant can use context to provide more relevant responses:

- **CLI**: Use `/context add filename.txt` to add file context
- **Desktop**: Use the "Add Context" button to select files
- **IDE Extensions**: Automatically use selected code as context

## Using with Code

Manorrock Assistant excels at coding tasks:

### Code Explanation

To explain code:

```
/explain

function calculateFactorial(n) {
    if (n === 0 || n === 1) return 1;
    return n * calculateFactorial(n - 1);
}
```

### Code Generation

To generate code:

```
Generate a Python function that checks if a string is a palindrome.
```

### Code Improvement

To improve existing code:

```
Improve this code:

public void processData(List<String> items) {
    for (int i = 0; i < items.size(); i++) {
        String item = items.get(i);
        if (item != null && item.length() > 0) {
            System.out.println(item.toUpperCase());
        }
    }
}
```

## System Message Templates

Manorrock Assistant includes specialized templates for different tasks:

```
/llm systemMessage code-review
```

Available templates:
- `default`: General assistant
- `code-review`: Code reviewer perspective
- `optimize`: Performance optimization specialist
- `security`: Security expert perspective
- `design-patterns`: Design pattern specialist

## Advanced Configuration

### Adjusting Temperature

Control the randomness of responses:

```
/llm temperature 0.7  # Default - balanced
/llm temperature 0.2  # More deterministic
/llm temperature 1.0  # More creative
```

### Function Calling

Enable tool integration (when available):

```
/llm functionCalling on
```

### Using Different Models

Switch between different models:

```
# For Ollama
/llm model llama3.2

# For OpenAI
/llm model gpt-4-turbo
```

## IDE-Specific Features

When using Manorrock Assistant in IDEs, take advantage of these additional features:

### VS Code

- Right-click on code selections to access assistant actions
- Use Command Palette (Ctrl+Shift+P) and search for "Manorrock Assistant"

### IntelliJ IDEA

- Use intention actions (light bulb icons) for AI suggestions
- Access from the tool window or editor context menu

### Eclipse

- Use the dedicated view for chat interactions
- Right-click in the editor for context menu options

### NetBeans

- Access from the Assistant window
- Use the context menu in the editor

## Next Steps

As you get comfortable with Manorrock Assistant, explore these advanced topics:

1. [Using Tools](/docs/user/tools.md) - Learn how to use tools to enhance assistant capabilities
2. [LLM Configuration Guide](/docs/user/configuration.md) - Detailed configuration options
3. [Command Reference](/docs/user/command-reference.md) - Complete list of available commands

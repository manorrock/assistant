# Using Tools in Manorrock Assistant

This guide explains how to use the Tool Framework in Manorrock Assistant to enhance your conversations with LLMs.

## What Are Tools?

Tools allow the Large Language Models (LLMs) in Manorrock Assistant to perform specific actions or access external information. They extend the capabilities of the assistant beyond simple text generation, enabling it to:

- Perform calculations
- Fetch real-time information
- Execute system commands
- Access and manipulate files
- And much more

## Available Tools

Manorrock Assistant comes with several built-in tools:

| Tool | Description | Example Usage |
|------|-------------|---------------|
| `calculator` | Performs mathematical calculations | `/tool run calculator "5 + (3 * 4)"` |
| `weather` | Retrieves current weather conditions | `/tool run weather city="New York"` |
| `search` | Performs web searches | `/tool run search query="Manorrock Assistant"` |
| `file` | Reads or writes files | `/tool run file path="/home/user/file.txt" mode="read"` |
| `system` | Executes system commands | `/tool run system command="ls -la"` |

## Using Tools

### Basic Tool Commands

#### Listing Available Tools

To see all available tools:

```
/tool list
```

#### Getting Tool Information

To learn about a specific tool and its parameters:

```
/tool info calculator
```

#### Running a Tool Manually

To run a tool directly:

```
/tool run calculator expression="(15 * 7) / 3 + 10"
```

### Enabling Automatic Tool Use

For a more natural experience, you can enable function calling, which allows LLMs to automatically use tools when appropriate:

```
/llm functionCalling on
```

With function calling enabled, you can simply ask questions that might require tools:

```
What's the weather like in New York today?
```

The assistant will automatically use the weather tool to answer this question.

## Tool Parameters

Each tool requires specific parameters:

### Calculator

```
/tool run calculator expression="<mathematical expression>"
```

Example:
```
/tool run calculator expression="sqrt(16) + log(100)"
```

### Weather

```
/tool run weather city="<city name>" [country="<country code>"]
```

Example:
```
/tool run weather city="London" country="UK"
```

### Search

```
/tool run search query="<search query>" [limit=<number>]
```

Example:
```
/tool run search query="Java programming best practices" limit=5
```

### File

```
/tool run file path="<file path>" mode="<read|write>" [content="<content>"]
```

Examples:
```
/tool run file path="/home/user/notes.txt" mode="read"
/tool run file path="/home/user/notes.txt" mode="write" content="Meeting notes for April 8"
```

### System

```
/tool run system command="<command>"
```

Example:
```
/tool run system command="echo Hello, World!"
```

## Tool Security

Tools that access system resources (like file and system) have security implications:

- In CLI and Desktop applications, tools run with your user permissions
- In IDE extensions, tools are restricted to the workspace context
- Some tools may be disabled in certain environments for security reasons

## Troubleshooting

### Common Issues

1. **Tool Not Found**: Ensure you're using the correct tool name with `/tool list`
2. **Missing Parameters**: Check required parameters with `/tool info <tool name>`
3. **Permission Errors**: For file and system tools, ensure you have necessary permissions
4. **Function Calling Not Working**: Verify function calling is enabled with `/llm info`

### Enabling Debug Output

For more detailed information about tool execution:

```
/debug on
/tool run calculator expression="5+5"
```

## Tool Configuration

Some tools have configurable options in the main configuration file:

```
~/.manorrock/assistant/config.properties
```

Example configuration options:

```properties
# Enable/disable specific tools
tools.calculator.enabled=true
tools.weather.enabled=true
tools.system.enabled=false

# Tool-specific settings
tools.weather.api_key=your-api-key
tools.search.engine=duckduckgo
```

See the [Configuration Guide](/docs/user/configuration.md) for more details.

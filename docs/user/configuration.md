# Configuration Guide

This guide provides detailed information on configuring Manorrock Assistant to suit your specific needs and preferences.

## Configuration Locations

Manorrock Assistant stores its configuration in the following location:

```
~/.manorrock/assistant/
  ├── config.properties     # General application settings
  ├── llms/                 # LLM-specific configurations
  │   ├── default.properties   # Default LLM settings
  │   └── custom-llm.properties # Custom LLM configurations
  ├── sessions/             # Saved conversation sessions
  ├── contexts/             # Saved context files
  └── templates/            # System message templates
      ├── default.json      # Default assistant template
      ├── code-review.json  # Code reviewer template
      └── custom.json       # User-created templates
```

On Windows, the configuration is stored in `%USERPROFILE%\.manorrock\assistant\`.

## Core Configuration

The main `config.properties` file contains application-wide settings:

```properties
# Example config.properties
activeLlm=default
colorMode=auto
debugMode=false
functionCalling=true
```

Key settings include:

| Setting | Description | Default |
|---------|-------------|---------|
| `activeLlm` | The name of the currently active LLM | `default` |
| `colorMode` | UI color scheme (auto, light, dark) | `auto` |
| `debugMode` | Enable detailed logging | `false` |
| `maxHistory` | Maximum conversation history to keep | `1000` |

## LLM Configuration

Each LLM configuration is stored in a separate file in the `llms/` directory:

```properties
# Example default.properties
apiKey=
baseUrl=http://localhost:11434
deploymentName=
functionCalling=false
maxMessages=10
modelName=llama3.2
systemMessage=You are a helpful assistant.
temperature=0.7
timeout=30
vendor=ollama
```

### Common LLM Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `vendor` | LLM provider (ollama, openai, azure_openai) | `ollama` |
| `modelName` | Name of the model to use | `llama3.2` |
| `baseUrl` | API endpoint URL | `http://localhost:11434` |
| `apiKey` | Authentication key (for OpenAI/Azure) | (empty) |
| `temperature` | Response randomness (0.0-1.0) | `0.7` |
| `timeout` | Request timeout in seconds | `30` |
| `maxMessages` | Context window size | `10` |
| `systemMessage` | System prompt or template name | `You are a helpful assistant.` |

### Vendor-Specific Settings

#### Ollama

```properties
vendor=ollama
baseUrl=http://localhost:11434
modelName=llama3.2
```

#### OpenAI

```properties
vendor=openai
baseUrl=https://api.openai.com
modelName=gpt-4
apiKey=your-openai-api-key
```

#### Azure OpenAI

```properties
vendor=azure_openai
baseUrl=https://your-resource-name.openai.azure.com
apiKey=your-azure-api-key
deploymentName=your-deployment-name
```

## System Message Templates

System message templates are stored in JSON files in the `templates/` directory:

```json
{
  "name": "code-review",
  "description": "Code review specialist",
  "systemMessage": "You are a code review specialist focusing on best practices, potential bugs, and performance issues. Analyze the code carefully and provide actionable feedback with clear examples when possible."
}
```

Template fields include:

| Field | Description |
|-------|-------------|
| `name` | Unique identifier for the template |
| `description` | Brief description of the template's purpose |
| `systemMessage` | The actual system message text to use |

## Configuration Methods

There are multiple ways to configure Manorrock Assistant:

### 1. Command-Line Configuration

Use commands in the assistant interface:

```
/llm vendor openai
/llm apiKey your-api-key-here
/llm model gpt-4
/llm temperature 0.5
```

### 2. Direct File Editing

Edit the configuration files directly using a text editor.

### 3. Environment Variables

Set environment variables to override configuration:

```bash
# Set Ollama host
export OLLAMA_HOST=192.168.1.100

# Set OpenAI key
export OPENAI_API_KEY=your-api-key
```

Key environment variables:

| Variable | Description | Overrides |
|----------|-------------|-----------|
| `OLLAMA_HOST` | Hostname for Ollama server | `baseUrl` in ollama configs |
| `OPENAI_API_KEY` | OpenAI API key | `apiKey` in openai configs |
| `AZURE_OPENAI_API_KEY` | Azure OpenAI API key | `apiKey` in azure configs |
| `AZURE_OPENAI_ENDPOINT` | Azure OpenAI endpoint | `baseUrl` in azure configs |
| `MANORROCK_CONFIG_DIR` | Custom config directory | Default config location |

### 4. GUI Configuration

Use the settings UI in desktop or IDE interfaces.

## Advanced Configuration

### Tool Configuration

Enable or disable specific tools:

```properties
# In config.properties
tools.calculator.enabled=true
tools.weather.enabled=true
tools.web-search.enabled=false
```

### Custom System Messages

Create custom system message templates:

1. Create a new JSON file in the `templates/` directory:
   ```json
   {
     "name": "my-template",
     "description": "My custom assistant",
     "systemMessage": "You are a specialized assistant that helps with..."
   }
   ```

2. Use the template with:
   ```
   /llm systemMessage my-template
   ```

### Proxy Configuration

Configure network proxy settings:

```properties
# In config.properties
proxy.enabled=true
proxy.host=proxy.example.com
proxy.port=8080
proxy.username=user
proxy.password=pass
```

### Memory and Performance

Adjust memory usage and performance:

```properties
# In config.properties
maxMessages=15     # Increase context window (more memory)
chunkSize=1000     # Adjust streaming chunk size
parallelRequests=2 # Maximum parallel LLM requests
```

## Interface-Specific Configuration

### CLI Configuration

CLI-specific settings in `config.properties`:

```properties
cli.colorEnabled=true
cli.historySize=100
cli.promptSymbol="> "
```

### Desktop Configuration

Desktop-specific settings:

```properties
desktop.windowWidth=800
desktop.windowHeight=600
desktop.fontSize=12
desktop.fontFamily=Monospace
```

### IDE Extensions Configuration

IDE-specific settings:

```properties
ide.showStatusBar=true
ide.autoContext=true
ide.contextLines=50
```

## Multi-User Configuration

For shared environments, create user-specific configurations:

```
/home/user1/.manorrock/assistant/
/home/user2/.manorrock/assistant/
```

Each user can maintain separate LLM configurations and settings.

## Configuration Profiles

Create multiple configuration profiles:

1. Create a directory for each profile:
   ```
   ~/.manorrock/assistant/profiles/work/
   ~/.manorrock/assistant/profiles/personal/
   ```

2. Switch profiles:
   ```
   export MANORROCK_CONFIG_DIR=~/.manorrock/assistant/profiles/work
   ```

## Configuration Backup and Restore

### Backup

To back up your configuration:

```bash
cp -r ~/.manorrock/assistant ~/assistant-backup
```

### Restore

To restore your configuration:

```bash
cp -r ~/assistant-backup ~/.manorrock/assistant
```

## Troubleshooting Configuration

### Reset to Defaults

To reset to default configuration:

```
/llm reset
```

Or delete specific configuration files to reset them.

### Debug Mode

Enable debug mode to troubleshoot configuration issues:

```
/debug on
```

### Configuration Validation

Manorrock Assistant validates configuration settings and will warn about invalid values.

# Manorrock Assistant Tool Framework

## Overview
The Tool Framework enables integration of custom tools with LLMs through a standardized interface.

## Tool Interface
Tools implement the `Tool` interface which defines:
- Name and description for LLM discovery
- Parameter specifications
- Execution logic

## Developing Custom Tools
1. Implement the Tool interface
```java
public class CustomTool implements Tool {
    @Override
    public String getName() {
        return "custom-tool";
    }
    
    @Override 
    public String getDescription() {
        return "Description of what the tool does";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        // Define expected parameters
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        // Implement tool logic
    }
}
```

2. Register with ToolManager
```java
toolManager.registerTool(new CustomTool());
```

## Tool Description Format
Tools are described to LLMs using a standardized JSON format:
```json
{
  "name": "tool-name",
  "description": "What the tool does",
  "parameters": [
    {
      "name": "param1",
      "description": "Parameter description",
      "type": "string|number|boolean"
    }
  ]
}
```

## Tool Result Format
Tools return results in a standardized format:
```json
{
  "success": true,
  "result": "Output data",
  "error": "Optional error message if success is false"
}
```

package com.manorrock.assistant.shared.tools;

import com.manorrock.assistant.shared.Tool;
import com.manorrock.assistant.shared.ToolParameter;
import com.manorrock.assistant.shared.ToolResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EchoTool implements Tool {
    
    @Override
    public String getName() {
        return "echo";
    }
    
    @Override
    public String getDescription() {
        return "Echoes back the input message. Useful for testing tool integration.";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Collections.singletonList(
            new ToolParameter("message", "string", "The message to echo back", true)
        );
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String message = (String) parameters.get("message");
        if (message == null) {
            return ToolResult.failure("Message parameter is required");
        }
        Map<String, Object> resultData = Collections.singletonMap("echo", (Object) message);
        return ToolResult.success(resultData, "Echo successful");
    }
}

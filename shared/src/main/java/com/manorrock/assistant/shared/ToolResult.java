package com.manorrock.assistant.shared;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of executing a tool.
 * Contains success/failure status and result data.
 */
public class ToolResult {
    
    private final boolean success;
    private final Map<String, Object> data;
    private final String message;
    
    /**
     * Creates a successful tool result.
     * 
     * @param data the result data
     * @param message success message
     * @return a successful ToolResult
     */
    public static ToolResult success(Map<String, Object> data, String message) {
        return new ToolResult(true, data, message);
    }
    
    /**
     * Creates a successful tool result with just data.
     * 
     * @param data the result data
     * @return a successful ToolResult
     */
    public static ToolResult success(Map<String, Object> data) {
        return new ToolResult(true, data, "Tool executed successfully");
    }
    
    /**
     * Creates a failed tool result.
     * 
     * @param message the error message
     * @return a failed ToolResult
     */
    public static ToolResult failure(String message) {
        return new ToolResult(false, null, message);
    }
    
    // Package-private constructor for tools in same package
    ToolResult(boolean success, Map<String, Object> data, String message) {
        this.success = success;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.message = message;
    }
    
    /**
     * @return true if the tool execution was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * @return the result data if successful, null otherwise
     */
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * @return a message describing the result or error
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Converts this result to a map suitable for serialization.
     * 
     * @return a map representation of this result
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", message);
        if (data != null) {
            result.put("data", data);
        }
        return result;
    }
    
    /**
     * Creates a JSON representation of this result suitable for LLM consumption.
     * 
     * @return a JSON string representing the result
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":").append(success).append(",");
        json.append("\"message\":\"").append(escapeJson(message)).append("\"");
        if (!data.isEmpty()) {
            json.append(",\"data\":{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                json.append("\"").append(escapeJson(entry.getKey())).append("\":");
                json.append(formatJsonValue(entry.getValue()));
            }
            json.append("}");
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private String formatJsonValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public Map<String, Object> data() {
        return data;
    }

    /**
     * @return output message for successful results, null otherwise
     */
    public String output() {
        return success ? message : null;
    }

    /**
     * @return error message for failed results, null otherwise
     */
    public String error() {
        return success ? null : message;
    }
}

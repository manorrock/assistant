package com.manorrock.assistant.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;

/**
 * Utility class for serializing and deserializing tool-related objects.
 * This ensures consistent formats between Java and TypeScript implementations.
 */
public class ToolSerializer {
    
    /**
     * Serializes a tool to a JSON string according to the tool-description-schema.json.
     * 
     * @param tool the tool to serialize
     * @return JSON string representation of the tool
     */
    public static String serializeTool(Tool tool) {
        StringBuilder json = new StringBuilder();
        
        json.append("{");
        json.append("\"name\":\"").append(escapeJson(tool.getName())).append("\",");
        json.append("\"description\":\"").append(escapeJson(tool.getDescription())).append("\",");
        json.append("\"parameters\":[");
        
        List<ToolParameter> params = tool.getParameters();
        for (int i = 0; i < params.size(); i++) {
            ToolParameter param = params.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("{");
            json.append("\"name\":\"").append(escapeJson(param.getName())).append("\",");
            json.append("\"type\":\"").append(escapeJson(param.getType())).append("\",");
            json.append("\"description\":\"").append(escapeJson(param.getDescription())).append("\",");
            json.append("\"required\":").append(param.isRequired());
            json.append("}");
        }
        
        json.append("]");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Creates a JSON representation of a tool request according to tool-request-schema.json.
     * 
     * @param toolName name of the tool to execute
     * @param parameters parameters to pass to the tool
     * @param context optional context information
     * @return JSON string representing the tool request
     */
    public static String createToolRequestJson(String toolName, Map<String, Object> parameters, Map<String, Object> context) {
        StringBuilder json = new StringBuilder();
        
        json.append("{");
        json.append("\"toolName\":\"").append(escapeJson(toolName)).append("\"");
        
        if (parameters != null && !parameters.isEmpty()) {
            json.append(",\"parameters\":{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                json.append("\"").append(escapeJson(entry.getKey())).append("\":");
                json.append(serializeValue(entry.getValue()));
            }
            json.append("}");
        }
        
        if (context != null && !context.isEmpty()) {
            json.append(",\"context\":{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                json.append("\"").append(escapeJson(entry.getKey())).append("\":");
                json.append(serializeValue(entry.getValue()));
            }
            json.append("}");
        }
        
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Parse a tool request JSON string into its components.
     * 
     * @param json the JSON string representing a tool request
     * @return map containing toolName, parameters, and context
     * @throws IllegalArgumentException if the JSON is invalid
     */
    public static Map<String, Object> parseToolRequest(String json) {
        // Note: In a real implementation, use a proper JSON parser
        // This is a simplified version for demonstration purposes
        Map<String, Object> result = new HashMap<>();
        
        // For demonstration, we're returning an empty map
        // In a real implementation, parse the JSON string
        result.put("toolName", "");
        result.put("parameters", new HashMap<String, Object>());
        result.put("context", new HashMap<String, Object>());
        
        return result;
    }
    
    /**
     * Escapes special characters in a string for use in JSON.
     * 
     * @param input the string to escape
     * @return the escaped string
     */
    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(c);
            }
        }
        
        return escaped.toString();
    }
    
    /**
     * Serializes a value to its JSON representation.
     * 
     * @param value the value to serialize
     * @return JSON representation of the value
     */
    private static String serializeValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            StringBuilder json = new StringBuilder();
            json.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                json.append("\"").append(escapeJson(entry.getKey())).append("\":");
                json.append(serializeValue(entry.getValue()));
            }
            json.append("}");
            return json.toString();
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            StringBuilder json = new StringBuilder();
            json.append("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                json.append(serializeValue(item));
            }
            json.append("]");
            return json.toString();
        } else {
            // For other types, try to use toString()
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }
}

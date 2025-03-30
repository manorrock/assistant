package com.manorrock.assistant.command;

import java.util.HashMap;
import java.util.Map;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;

/**
 * Utility class for serializing and deserializing tool-related objects.
 * This ensures consistent formats between Java and TypeScript implementations.
 */
public class ToolSerializer {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    /**
     * Serializes a tool to a JSON string according to the tool-description-schema.json.
     * 
     * @param tool the tool to serialize
     * @return JSON string representation of the tool
     */
    public static String serializeTool(Tool tool) {
        try {
            ObjectNode rootNode = MAPPER.createObjectNode();
            rootNode.put("name", tool.getName());
            rootNode.put("description", tool.getDescription());
            
            ArrayNode paramsNode = rootNode.putArray("parameters");
            for (ToolParameter param : tool.getParameters()) {
                ObjectNode paramNode = paramsNode.addObject();
                paramNode.put("name", param.getName());
                paramNode.put("type", param.getType());
                paramNode.put("description", param.getDescription());
                paramNode.put("required", param.isRequired());
            }
            
            return MAPPER.writeValueAsString(rootNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize tool", e);
        }
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
        try {
            ObjectNode rootNode = MAPPER.createObjectNode();
            rootNode.put("toolName", toolName);
            
            if (parameters != null && !parameters.isEmpty()) {
                rootNode.set("parameters", MAPPER.valueToTree(parameters));
            }
            
            if (context != null && !context.isEmpty()) {
                rootNode.set("context", MAPPER.valueToTree(context));
            }
            
            return MAPPER.writeValueAsString(rootNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create tool request JSON", e);
        }
    }
    
    /**
     * Parse a tool request JSON string into its components.
     * 
     * @param json the JSON string representing a tool request
     * @return map containing toolName, parameters, and context
     * @throws IllegalArgumentException if the JSON is invalid
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseToolRequest(String json) {
        try {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> rootMap = MAPPER.readValue(json, Map.class);
            
            // Extract the main components
            result.put("toolName", rootMap.get("toolName"));
            result.put("parameters", rootMap.getOrDefault("parameters", new HashMap<String, Object>()));
            result.put("context", rootMap.getOrDefault("context", new HashMap<String, Object>()));
            
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }
}

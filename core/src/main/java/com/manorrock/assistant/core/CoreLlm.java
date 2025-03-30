package com.manorrock.assistant.core;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.manorrock.assistant.api.Llm;
import com.manorrock.assistant.api.LlmManager;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import static dev.langchain4j.data.message.UserMessage.userMessage;
import org.json.JSONObject;

/**
 * The Core LLM.
 * 
 * <p>
 * This class delivers the core LLM implementation we expose to the core Assistant.
 * Underneath its covers we dispatch the request to a LangChain4J ChatModel. Note 
 * that if you want to use a different LLM you can do so by implementing the LLM
 * interface and registering it with the LLM manager.
 * </p>
 * <p>
 *  Note this implementations supports a curated set of LangChain4J models. If you
 *  want a model that is currently not available please open an issue on our GitHub
 *  repository and we will see if we can add it. Or go ahead and implement it using
 *  the same pattern as mentioned above.
 * </p>
 * <p>
 *  Currently we only support Ollama models.
 * </p>
 */
public class CoreLlm implements Llm {

    /**
     * Stores the model used to process the prompt (with default local Llama3.2 model).
     */
    ChatLanguageModel model = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("llama3.2")
            .build();

    /**
     * Stores the chat memory.
     */
    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

    /**
     * Stores the properties.
     */
    Properties properties = new Properties();
    
    /**
     * Stores the LLM manager.
     */
    private LlmManager manager;
    
    /**
     * Flag to enable or disable tool integration.
     */
    private boolean useToolIntegration = false;
    
    /**
     * Constructor.
     * 
     * @param manager the LLM manager.
     */
    public CoreLlm(LlmManager manager) {
        this.manager = manager;
    }

    @Override
    public String process(String prompt) {
        chatMemory.add(userMessage(prompt));
        
        // Check if we should use tools
        if (useToolIntegration) {
            return processWithTools(prompt);
        } else {
            // Standard processing without tools
            AiMessage answer = model.chat(chatMemory.messages()).aiMessage();
            chatMemory.add(answer);
            return answer.text();
        }
    }
    
    /**
     * Process a prompt with tool integration.
     * 
     * @param prompt The prompt to process
     * @return The response text
     */
    private String processWithTools(String prompt) {
        StringBuilder resultBuilder = new StringBuilder();
        
        try {
            // Build tool specifications from available tools
            List<ToolSpecification> toolSpecifications = buildToolSpecifications();
            
            // Add system message for tool-enabled interactions
            // chatMemory.add(SystemMessage.from(
                // "You are a helpful assistant with access to tools. When appropriate, use tools to accomplish tasks. " +
                // "Always think step by step and explain your reasoning clearly."));
            
            // Create initial request with chat memory and tool specifications
            ChatRequest initialRequest = ChatRequest.builder()
                .toolSpecifications(toolSpecifications)
                .messages(chatMemory.messages())
                .build();
            
            // Get initial response from the model
            ChatResponse initialResponse = model.chat(initialRequest);
            AiMessage aiMessage = initialResponse.aiMessage();
            
            // Add the initial response to our result if it has text content
            if (aiMessage.text() != null && !aiMessage.text().isEmpty()) {
                resultBuilder.append(aiMessage.text());
            }
            
            // Handle tool execution if needed
            if (aiMessage.hasToolExecutionRequests()) {
                // Add the AI message with tool requests to chat memory
                chatMemory.add(aiMessage);
                
                // Process each tool execution request
                for (var toolRequest : aiMessage.toolExecutionRequests()) {
                    try {
                        // Extract tool name and arguments
                        String toolName = toolRequest.name();
                        String arguments = toolRequest.arguments();
                        
                        // Execute the tool using the assistant's tool manager
                        JSONObject argsJson = new JSONObject(arguments);
                        var result = manager.getAssistant().getToolManager().executeTool(toolName, argsJson.toMap());
                        
                        // Create a JSON object containing both status and result data
                        JSONObject resultJson = new JSONObject();
                        resultJson.put("status", result.success() ? "success" : "error");
                        resultJson.put("message", result.getMessage());
                        
                        if (result.getData() != null) {
                            resultJson.put("data", result.getData());
                        }
                        
                        // Create tool execution result message and add to chat memory
                        ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(
                            toolRequest,
                            resultJson.toString()
                        );
                        chatMemory.add(resultMessage);
                    } catch (Exception e) {
                        // Handle any errors during tool execution
                        JSONObject errorJson = new JSONObject();
                        errorJson.put("status", "error");
                        errorJson.put("message", e.getMessage());
                        
                        ToolExecutionResultMessage errorMessage = ToolExecutionResultMessage.from(
                            toolRequest,
                            errorJson.toString()
                        );
                        chatMemory.add(errorMessage);
                    }
                }
                
                // Create follow-up request with updated chat memory
                ChatRequest followUpRequest = ChatRequest.builder()
                    .messages(chatMemory.messages())
                    .toolSpecifications(toolSpecifications)
                    .build();
                
                // Get follow-up response with tool results included
                ChatResponse followUpResponse = model.chat(followUpRequest);
                AiMessage followUpMessage = followUpResponse.aiMessage();
                
                // Add the follow-up response to our result
                if (followUpMessage.text() != null && !followUpMessage.text().isEmpty()) {
                    if (resultBuilder.length() > 0) {
                        resultBuilder.append("\n\n");
                    }
                    resultBuilder.append(followUpMessage.text());
                }
                
                // Add the final response to chat memory
                chatMemory.add(followUpMessage);
            } else {
                // For responses without tool requests, simply add to chat memory
                chatMemory.add(aiMessage);
            }
        } catch (Exception e) {
            return "Error processing with tools: " + e.getMessage();
        }
        
        return resultBuilder.toString();
    }
    
    /**
     * Builds tool specifications from the assistant's available tools.
     * 
     * @return List of tool specifications for LangChain4j
     */
    private List<ToolSpecification> buildToolSpecifications() {
        if (manager.getAssistant() == null || manager.getAssistant().getToolManager() == null) {
            return Collections.emptyList();
        }
        
        return manager.getAssistant().getToolManager().getAvailableTools().stream()
            .map(tool -> {
                ToolSpecification.Builder toolSpecBuilder = ToolSpecification.builder()
                    .name(tool.getName())
                    .description(tool.getDescription());
        
                JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
                
                // Add each parameter to the schema
                tool.getParameters().forEach(param -> {
                    switch (param.getType().toLowerCase()) {
                        case "string":
                            schemaBuilder.addStringProperty(param.getName(), param.getDescription());
                            break;
                        case "integer":
                        case "int":
                            schemaBuilder.addIntegerProperty(param.getName(), param.getDescription());
                            break;
                        case "number":
                        case "float":
                        case "double":
                            schemaBuilder.addNumberProperty(param.getName(), param.getDescription());
                            break;
                        case "boolean":
                        case "bool":
                            schemaBuilder.addBooleanProperty(param.getName(), param.getDescription());
                            break;
                        default:
                            // Default to string for unknown types
                            schemaBuilder.addStringProperty(param.getName(), param.getDescription());
                    }
                    // Mark required parameters
                    if (param.isRequired()) {
                        schemaBuilder.required(param.getName());
                    }
                });
                
                // Build and return the tool specification
                return toolSpecBuilder
                    .parameters(schemaBuilder.build())
                    .build();
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Enable or disable tool integration.
     * 
     * @param enabled true to enable tool integration, false to disable
     */
    public void setToolIntegration(boolean enabled) {
        this.useToolIntegration = enabled;
    }
    
    /**
     * Check if tool integration is enabled.
     * 
     * @return true if tool integration is enabled, false otherwise
     */
    public boolean isToolIntegrationEnabled() {
        return useToolIntegration;
    }    

    @Override
    public void destroy() {
        chatMemory = null;
        model = null;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public void init() {
        model = OllamaChatModel.builder()
                .baseUrl(properties.getOrDefault("baseUrl", "http://localhost:11434").toString())
                .modelName(properties.getOrDefault("modelName", "llama3.2").toString())
                .build();
        chatMemory = MessageWindowChatMemory.withMaxMessages(10);    
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Get the model.
     * 
     * @return the model.
     */
    public ChatLanguageModel getChatLanguageModel() {
        return model;
    }
}

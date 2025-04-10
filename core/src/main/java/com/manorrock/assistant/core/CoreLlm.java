package com.manorrock.assistant.core;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.manorrock.assistant.api.Llm;
import com.manorrock.assistant.api.LlmManager;
import com.manorrock.assistant.api.LlmStreamingResponseHandler;
import com.manorrock.assistant.api.TokenUsageTracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.type.TypeReference;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import static dev.langchain4j.data.message.UserMessage.userMessage;

/**
 * The Core LLM.
 * 
 * <p>
 * This class delivers the core LLM implementation we expose to the core
 * Assistant. Underneath its covers we dispatch the request to a LangChain4J
 * ChatModel. Note that if you want to use a different LLM you can do so by
 * implementing the LLM interface and registering it with the LLM manager.
 * </p>
 * <p>
 * Note this implementations supports a curated set of LangChain4J models. If
 * you want a model that is currently not available please open an issue on our
 * GitHub repository and we will see if we can add it. Or go ahead and implement it
 * using the same pattern as mentioned above.
 * </p>
 */
public class CoreLlm implements Llm {

    /**
     * JSON Object Mapper.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Stores the model used to process the prompt (with default local Llama3.2
     * model).
     */
    ChatLanguageModel model = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("llama3.2")
            .build();
            
    /**
     * Stores the streaming model used for real-time token generation.
     */
    StreamingChatLanguageModel streamingModel;

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
     * Stores the flag to enable or disable function calling.
     */
    private boolean functionCallingEnabled = false;

    /**
     * Constructor.
     * 
     * @param manager the LLM manager.
     */
    public CoreLlm(LlmManager manager) {
        this.manager = manager;
        init();
    }

    /**
     * Process a prompt with tool integration.
     * 
     * @param prompt The prompt to process
     * @return The response text
     */
    private String processWithTools(String prompt) {
        StringBuilder resultBuilder = new StringBuilder();

        // Only add system message if chat memory is empty
        if (chatMemory.messages().isEmpty() && properties.getProperty("systemMessage") != null) {
            chatMemory.add(SystemMessage.from(properties.getProperty("systemMessage")));
        }

        chatMemory.add(userMessage(prompt));

        try {
            // Build tool specifications from available tools
            List<ToolSpecification> toolSpecifications = buildToolSpecifications();

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

                        // Parse arguments as Map using Jackson
                        var argsMap = MAPPER.readValue(arguments, new TypeReference<java.util.Map<String, Object>>() {
                        });

                        // Execute the tool using the assistant's tool manager
                        var result = manager.getAssistant().getToolManager().executeTool(toolName, argsMap);

                        // Create a JSON object containing both status and result data
                        ObjectNode resultJson = MAPPER.createObjectNode();
                        resultJson.put("status", result.success() ? "success" : "error");
                        resultJson.put("message", result.getMessage());

                        if (result.getData() != null) {
                            // Convert result data to JsonNode
                            resultJson.set("data", MAPPER.valueToTree(result.getData()));
                        }

                        // Create tool execution result message and add to chat memory
                        ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(
                                toolRequest,
                                MAPPER.writeValueAsString(resultJson));
                        chatMemory.add(resultMessage);
                    } catch (Exception e) {
                        // Handle any errors during tool execution
                        ObjectNode errorJson = MAPPER.createObjectNode();
                        errorJson.put("status", "error");
                        errorJson.put("message", e.getMessage());

                        ToolExecutionResultMessage errorMessage = ToolExecutionResultMessage.from(
                                toolRequest,
                                MAPPER.writeValueAsString(errorJson));
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
        if (manager == null || manager.getAssistant() == null || manager.getAssistant().getToolManager() == null) {
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

    @Override
    public void destroy() {
        chatMemory = null;
        functionCallingEnabled = false;
        model = null;
        streamingModel = null;
    }

    /**
     * Get the chat language model.
     * 
     * @return the chat language model.
     */
    public ChatLanguageModel getChatLanguageModel() {
        return model;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public void init() {
        initializeDefaultProperties();
        initializeChatLanguageModel();
        initializeStreamingChatLanguageModel();
        initializeChatMemory();
        initializeFunctionCalling();
    }

    /**
     * Initialize default properties if not already set.
     */
    private void initializeDefaultProperties() {
        if (!properties.containsKey("baseUrl")) {
            properties.setProperty("baseUrl", "http://localhost:11434");
        }
        if (!properties.containsKey("functionCalling")) {
            properties.setProperty("functionCalling", "false");
        }
        if (!properties.containsKey("maxMessages")) {
            properties.setProperty("maxMessages", "10");
        }
        if (!properties.containsKey("modelName")) {
            properties.setProperty("modelName", "llama3.2");
        }
        if (!properties.containsKey("systemMessage")) {
            properties.setProperty("systemMessage", "You are a helpful assistant.");
        }
        if (!properties.containsKey("temperature")) {
            properties.setProperty("temperature", "0.7");
        }
        if (!properties.containsKey("timeout")) {
            properties.setProperty("timeout", "30");
        }
        if (!properties.containsKey("vendor")) {
            properties.setProperty("vendor", "ollama");
        }
    }

    /**
     * Initialize the chat language model with properties.
     */
    private void initializeChatLanguageModel() {
        Double temperature;
        try {
            temperature = Double.parseDouble(
                    properties.getProperty("temperature"));
        } catch (NumberFormatException e) {
            temperature = 0.7;
        }
        Duration timeout;
        try {
            timeout = Duration.ofSeconds(
                    Long.parseLong(properties.getProperty("timeout")));
        } catch (NumberFormatException e) {
            timeout = Duration.ofSeconds(30);
        }
        String vendor = properties.getProperty("vendor", "ollama");
        switch(vendor.toLowerCase()) {
            case "azure_openai":
                model = AzureOpenAiChatModel.builder()
                    .apiKey(properties.getProperty("apiKey"))
                    .deploymentName(properties.getProperty("deploymentName"))
                    .temperature(temperature)
                    .timeout(timeout)
                    .build();
                break;
            case "openai":
                model = OpenAiChatModel.builder()
                    .apiKey(properties.getProperty("apiKey"))
                    .modelName(properties.getProperty("modelName"))
                    .temperature(temperature)
                    .timeout(timeout)
                    .build();
                break;
            case "ollama":
            default:
                model = OllamaChatModel.builder()
                    .baseUrl(properties.getProperty("baseUrl"))
                    .modelName(properties.getProperty("modelName"))
                    .temperature(temperature)
                    .timeout(timeout)
                    .build();
            break;
        }
    }

    /**
     * Initialize the streaming chat language model with properties.
     */
    private void initializeStreamingChatLanguageModel() {
        Double temperature;
        try {
            temperature = Double.parseDouble(
                    properties.getProperty("temperature"));
        } catch (NumberFormatException e) {
            temperature = 0.7;
        }
        Duration timeout;
        try {
            timeout = Duration.ofSeconds(
                    Long.parseLong(properties.getProperty("timeout")));
        } catch (NumberFormatException e) {
            timeout = Duration.ofSeconds(30);
        }
        String vendor = properties.getProperty("vendor", "ollama");
        switch(vendor.toLowerCase()) {
            case "azure_openai":
                streamingModel = AzureOpenAiStreamingChatModel.builder()
                    .apiKey(properties.getProperty("apiKey"))
                    .deploymentName(properties.getProperty("deploymentName"))
                    .temperature(temperature)
                    .timeout(timeout)
                    .build();
                break;
            case "openai":
                streamingModel = OpenAiStreamingChatModel.builder()
                    .apiKey(properties.getProperty("apiKey"))
                    .modelName(properties.getProperty("modelName"))
                    .temperature(temperature)
                    .timeout(timeout)
                    .build();
                break;
            case "ollama":
            default:
                streamingModel = OllamaStreamingChatModel.builder()
                    .baseUrl(properties.getProperty("baseUrl"))
                    .modelName(properties.getProperty("modelName"))
                    .temperature(temperature)
                    .timeout(timeout)
                    .build();
            break;
        }
    }

    /**
     * Initialize the chat memory with max messages from properties.
     */
    private void initializeChatMemory() {
        try {
            chatMemory = MessageWindowChatMemory.withMaxMessages(
                    Integer.parseInt(properties.getProperty("maxMessages")));
        } catch (NumberFormatException e) {
            chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        }
    }

    /**
     * Initialize function calling based on properties.
     */
    private void initializeFunctionCalling() {
        functionCallingEnabled = Boolean.parseBoolean(
                properties.getProperty("functionCalling"));
    }

    /**
     * Check if function calling is enabled.
     * 
     * @return true if function calling is enabled, false otherwise
     */
    public boolean isFunctionCallingEnabled() {
        return functionCallingEnabled;
    }

    @Override
    public String process(String prompt) {
        // Handle empty prompts to prevent IllegalArgumentException
        if (prompt == null || prompt.trim().isEmpty()) {
            return "I need some input to provide a helpful response.";
        }

        String response;
        String modelName = properties.getProperty("modelName", "unknown");
        int promptTokens = CoreTokenCounterUtil.countTokens(prompt, modelName);
        
        // Process based on whether function calling is enabled
        if (functionCallingEnabled) {
            response = processWithTools(prompt);
        } else {
            response = processWithoutTools(prompt);
        }
        
        // Track token usage
        int completionTokens = CoreTokenCounterUtil.countTokens(response, modelName);
        int totalTokens = promptTokens + completionTokens;
        
        // Record usage in the token tracker if available
        if (manager instanceof CoreLlmManager) {
            TokenUsageTracker tracker = ((CoreLlmManager) manager).getTokenTracker();
            if (tracker != null) {
                // Create a map of token usage data
                Map<String, Integer> usageData = new HashMap<>();
                usageData.put("promptTokens", promptTokens);
                usageData.put("completionTokens", completionTokens);
                usageData.put("totalTokens", totalTokens);
                
                // Record the usage data
                tracker.recordUsage(modelName, usageData);
            }
        }
        
        return response;
    }

    /**
     * Process a prompt without tool integration.
     * 
     * @param prompt the prompt to process
     * @return the response text
     */
    private String processWithoutTools(String prompt) {
        // Only add system message if chat memory is empty
        if (chatMemory.messages().isEmpty() && properties.getProperty("systemMessage") != null) {
            chatMemory.add(SystemMessage.from(properties.getProperty("systemMessage")));
        }

        chatMemory.add(userMessage(prompt));
        ChatResponse response = model.chat(chatMemory.messages());
        chatMemory.add(response.aiMessage());
        return response.aiMessage().text();
    }

    /**
     * Enable or disable function calling.
     * 
     * @param functionCallingEnabled true to function calling, false to disable
     */
    public void setFunctionCallingEnabled(boolean functionCallingEnabled) {
        this.functionCallingEnabled = functionCallingEnabled;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void processStreaming(String prompt, LlmStreamingResponseHandler handler) {
        // Handle empty prompts
        if (prompt == null || prompt.trim().isEmpty()) {
            handler.onToken("I need some input to provide a helpful response.");
            handler.onComplete("I need some input to provide a helpful response.");
            return;
        }
        
        // Track prompt tokens
        String modelName = properties.getProperty("modelName", "unknown");
        int promptTokens = CoreTokenCounterUtil.countTokens(prompt, modelName);
        
        // Initialize a StringBuilder to accumulate the full response for token tracking
        StringBuilder fullResponseBuilder = new StringBuilder();
        
        try {
            // Process based on whether function calling is enabled
            if (functionCallingEnabled) {
                processWithToolsStreaming(prompt, new LlmStreamingResponseHandler() {
                    @Override
                    public void onToken(String token) {
                        fullResponseBuilder.append(token);
                        handler.onToken(token);
                    }
                    
                    @Override
                    public void onComplete(String fullResponse) {
                        trackTokenUsage(promptTokens, fullResponse);
                        handler.onComplete(fullResponse);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        handler.onError(error);
                    }
                });
            } else {
                processWithoutToolsStreaming(prompt, new LlmStreamingResponseHandler() {
                    @Override
                    public void onToken(String token) {
                        fullResponseBuilder.append(token);
                        handler.onToken(token);
                    }
                    
                    @Override
                    public void onComplete(String fullResponse) {
                        trackTokenUsage(promptTokens, fullResponse);
                        handler.onComplete(fullResponse);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        handler.onError(error);
                    }
                });
            }
        } catch (Exception e) {
            handler.onError(e);
        }
    }
    
    /**
     * Track token usage for the given prompt and response.
     * 
     * @param promptTokens the number of tokens in the prompt
     * @param response the full response text
     */
    private void trackTokenUsage(int promptTokens, String response) {
        if (manager instanceof CoreLlmManager) {
            String modelName = properties.getProperty("modelName", "unknown");
            int completionTokens = CoreTokenCounterUtil.countTokens(response, modelName);
            int totalTokens = promptTokens + completionTokens;
            
            TokenUsageTracker tracker = ((CoreLlmManager) manager).getTokenTracker();
            if (tracker != null) {
                Map<String, Integer> usageData = new HashMap<>();
                usageData.put("promptTokens", promptTokens);
                usageData.put("completionTokens", completionTokens);
                usageData.put("totalTokens", totalTokens);
                
                tracker.recordUsage(modelName, usageData);
            }
        }
    }
    
    /**
     * Process a prompt without tool integration in streaming mode.
     * 
     * @param prompt the prompt to process
     * @param handler the callback handler for streaming responses
     */
    private void processWithoutToolsStreaming(String prompt, LlmStreamingResponseHandler handler) {
        // Only add system message if chat memory is empty
        if (chatMemory.messages().isEmpty() && properties.getProperty("systemMessage") != null) {
            chatMemory.add(SystemMessage.from(properties.getProperty("systemMessage")));
        }

        chatMemory.add(userMessage(prompt));
        
        // Create a request with chat memory
        ChatRequest request = ChatRequest.builder()
                .messages(chatMemory.messages())
                .build();
        
        // Create an adapter from our handler to LangChain4j's StreamingChatResponseHandler
        StreamingChatResponseHandler responseHandler = new StreamingChatResponseHandler() {
            private final StringBuilder responseBuilder = new StringBuilder();
            
            @Override
            public void onPartialResponse(String token) {
                handler.onToken(token);
                responseBuilder.append(token);
            }
            
            @Override
            public void onCompleteResponse(ChatResponse response) {
                String fullResponse = responseBuilder.toString();
                chatMemory.add(response.aiMessage());
                handler.onComplete(fullResponse);
            }
            
            @Override
            public void onError(Throwable error) {
                handler.onError(error);
            }
        };
        
        // Use the chat method with our StreamingChatResponseHandler adapter
        streamingModel.chat(request, responseHandler);
    }

    /**
     * Process a prompt with tool integration in streaming mode.
     * 
     * @param prompt the prompt to process
     * @param handler the callback handler for streaming responses
     */
    private void processWithToolsStreaming(String prompt, LlmStreamingResponseHandler handler) {
        StringBuilder resultBuilder = new StringBuilder();

        // Only add system message if chat memory is empty
        if (chatMemory.messages().isEmpty() && properties.getProperty("systemMessage") != null) {
            chatMemory.add(SystemMessage.from(properties.getProperty("systemMessage")));
        }

        chatMemory.add(userMessage(prompt));

        try {
            // Build tool specifications from available tools
            List<ToolSpecification> toolSpecifications = buildToolSpecifications();

            // Create a request with chat memory and tool specifications
            ChatRequest initialRequest = ChatRequest.builder()
                    .toolSpecifications(toolSpecifications)
                    .messages(chatMemory.messages())
                    .build();

            // Create a response handler for the initial response
            final AiMessage[] initialAiMessage = {null};
            final boolean[] hasToolRequests = {false};
            
            StreamingChatResponseHandler initialResponseHandler = new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String token) {
                    resultBuilder.append(token);
                    handler.onToken(token);
                }
                
                @Override
                public void onCompleteResponse(ChatResponse response) {
                    AiMessage aiMessage = response.aiMessage();
                    initialAiMessage[0] = aiMessage;
                    hasToolRequests[0] = aiMessage.hasToolExecutionRequests();
                    
                    // If no tool requests, we're done
                    if (!hasToolRequests[0]) {
                        chatMemory.add(aiMessage);
                        handler.onComplete(resultBuilder.toString());
                    }
                    // If there are tool requests, they'll be handled after this callback completes
                }
                
                @Override
                public void onError(Throwable error) {
                    handler.onError(error);
                }
            };
            
            // Stream the initial response using the chat method with our StreamingChatResponseHandler
            streamingModel.chat(initialRequest, initialResponseHandler);
            
            // Process tool requests if any
            if (hasToolRequests[0] && initialAiMessage[0] != null) {
                AiMessage aiMessage = initialAiMessage[0];
                
                // Add the AI message with tool requests to chat memory
                chatMemory.add(aiMessage);
                
                // Process each tool execution request
                for (var toolRequest : aiMessage.toolExecutionRequests()) {
                    try {
                        // Extract tool name and arguments
                        String toolName = toolRequest.name();
                        String arguments = toolRequest.arguments();
                        
                        // Notify handler that we're executing a tool
                        handler.onToken("\n\n[Executing tool: " + toolName + "]");
                        
                        // Parse arguments as Map using Jackson
                        var argsMap = MAPPER.readValue(arguments, new TypeReference<java.util.Map<String, Object>>() {});
                        
                        // Execute the tool
                        var result = manager.getAssistant().getToolManager().executeTool(toolName, argsMap);
                        
                        // Create a JSON object containing both status and result data
                        ObjectNode resultJson = MAPPER.createObjectNode();
                        resultJson.put("status", result.success() ? "success" : "error");
                        resultJson.put("message", result.getMessage());
                        
                        if (result.getData() != null) {
                            // Convert result data to JsonNode
                            resultJson.set("data", MAPPER.valueToTree(result.getData()));
                        }
                        
                        // Create tool execution result message and add to chat memory
                        ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(
                                toolRequest,
                                MAPPER.writeValueAsString(resultJson));
                        chatMemory.add(resultMessage);
                        
                        // Notify handler about tool execution result
                        handler.onToken("\n[Tool result: " + (result.success() ? "Success" : "Error") + "]");
                    } catch (Exception e) {
                        // Handle any errors during tool execution
                        ObjectNode errorJson = MAPPER.createObjectNode();
                        errorJson.put("status", "error");
                        errorJson.put("message", e.getMessage());
                        
                        ToolExecutionResultMessage errorMessage = ToolExecutionResultMessage.from(
                                toolRequest,
                                MAPPER.writeValueAsString(errorJson));
                        chatMemory.add(errorMessage);
                        
                        // Notify handler about tool execution error
                        handler.onToken("\n[Tool error: " + e.getMessage() + "]");
                    }
                }
                
                // Notify handler that we're getting follow-up response
                handler.onToken("\n\n[Getting final response]");
                
                // Create follow-up request with updated chat memory
                ChatRequest followUpRequest = ChatRequest.builder()
                        .messages(chatMemory.messages())
                        .toolSpecifications(toolSpecifications)
                        .build();
                
                // Get follow-up response with tool results included
                StringBuilder followUpResponseBuilder = new StringBuilder();
                
                // Create a response handler for the follow-up response
                StreamingChatResponseHandler followUpResponseHandler = new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        followUpResponseBuilder.append(token);
                        handler.onToken(token);
                    }
                    
                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        AiMessage followUpMessage = response.aiMessage();
                        // Add the final response to chat memory
                        chatMemory.add(followUpMessage);
                        
                        // Notify handler that streaming is complete
                        String fullResponse = resultBuilder.toString() + "\n\n" + followUpResponseBuilder.toString();
                        handler.onComplete(fullResponse);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        handler.onError(error);
                    }
                };
                
                // Stream the follow-up response
                streamingModel.chat(followUpRequest, followUpResponseHandler);
            }
        } catch (Exception e) {
            handler.onError(e);
        }
    }
}

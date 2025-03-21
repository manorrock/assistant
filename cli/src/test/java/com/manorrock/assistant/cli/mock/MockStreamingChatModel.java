package com.manorrock.assistant.cli.mock;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.TokenStream;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A mock implementation of StreamingChatLanguageModel for testing tool integration.
 * This model provides predictable responses for tool execution testing.
 */
public class MockStreamingChatModel implements StreamingChatLanguageModel {

    private final Map<Pattern, Function<String, String>> responsePatterns;
    private final Map<String, Function<Map<String, Object>, String>> toolResponses;
    private final long streamingDelayMs;
    private final ScheduledExecutorService executor;
    private final Random random = new Random();
    private final double toolCallProbability;

    /**
     * Constructs a MockStreamingChatModel with default settings.
     */
    public MockStreamingChatModel() {
        this(100, 0.7);
    }

    /**
     * Constructs a MockStreamingChatModel with custom settings.
     *
     * @param streamingDelayMs delay between streaming chunks in milliseconds
     * @param toolCallProbability probability (0.0-1.0) of using a tool when tools are available
     */
    public MockStreamingChatModel(long streamingDelayMs, double toolCallProbability) {
        this.streamingDelayMs = streamingDelayMs;
        this.toolCallProbability = toolCallProbability;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.responsePatterns = new HashMap<>();
        this.toolResponses = new HashMap<>();
        
        // Initialize with some default responses
        setupDefaultResponses();
    }

    private void setupDefaultResponses() {
        // Add a default echo response
        responsePatterns.put(Pattern.compile(".*"), input -> 
            "I received your message: " + input);
        
        // Common question patterns
        responsePatterns.put(Pattern.compile("(?i).*what is your name.*"), 
            input -> "I am MockStreamingChatModel, a test implementation for tool integration.");
        
        responsePatterns.put(Pattern.compile("(?i).*how are you.*"), 
            input -> "As a mock model, I'm functioning as expected. Thanks for asking!");
        
        // Default tool response handlers
        toolResponses.put("echo", params -> {
            String message = (String) params.getOrDefault("message", "No message provided");
            return "ECHO: " + message;
        });
        
        toolResponses.put("calculator", params -> {
            String operation = (String) params.getOrDefault("operation", "add");
            Number a = (Number) params.getOrDefault("a", 0);
            Number b = (Number) params.getOrDefault("b", 0);
            
            double result = switch(operation.toLowerCase()) {
                case "add" -> a.doubleValue() + b.doubleValue();
                case "subtract" -> a.doubleValue() - b.doubleValue();
                case "multiply" -> a.doubleValue() * b.doubleValue();
                case "divide" -> a.doubleValue() / b.doubleValue();
                default -> 0;
            };
            
            return String.format("The result of %s %s %s is %s", a, operation, b, result);
        });
    }

    /**
     * Add a custom response pattern.
     *
     * @param pattern regex pattern to match against user input
     * @param responseGenerator function that generates a response based on the matched input
     * @return this instance for method chaining
     */
    public MockStreamingChatModel addResponsePattern(String pattern, Function<String, String> responseGenerator) {
        this.responsePatterns.put(Pattern.compile(pattern), responseGenerator);
        return this;
    }

    /**
     * Add a custom tool response handler.
     *
     * @param toolName name of the tool
     * @param responseGenerator function that generates a response based on tool parameters
     * @return this instance for method chaining
     */
    public MockStreamingChatModel addToolResponse(String toolName, Function<Map<String, Object>, String> responseGenerator) {
        this.toolResponses.put(toolName, responseGenerator);
        return this;
    }

    // @Override
    // public void generate(List<ChatMessage> messages, List<dev.langchain4j.model.output.ToolSpecification> tools, StreamingResponseHandler<AiMessage> handler) {
    //     String userInput = getLastUserMessage(messages);
        
    //     // Determine if we should use a tool
    //     boolean shouldUseTool = !tools.isEmpty() && random.nextDouble() < toolCallProbability;
        
    //     if (shouldUseTool) {
    //         // Select a random tool
    //         int toolIndex = random.nextInt(tools.size());
    //         dev.langchain4j.model.output.ToolSpecification selectedTool = tools.get(toolIndex);
            
    //         // Create a mock tool execution request
    //         Map<String, Object> mockParams = generateMockParameters(selectedTool);
    //         ToolExecutionRequest request = ToolExecutionRequest.builder()
    //                 .name(selectedTool.name())
    //                 .arguments(mockToolArguments(mockParams))
    //                 .build();
            
    //         // Stream thinking message
    //         streamResponse("I need to use the " + selectedTool.name() + " tool to help with this.", handler, true);
            
    //         // Notify of tool call
    //         handler.onToolExecutionRequest(request);
            
    //         // Process the tool execution result when provided
    //         CompletableFuture.runAsync(() -> {
    //             try {
    //                 // Simulate processing time
    //                 Thread.sleep(500);
                    
    //                 // When tool execution result is given back, continue with a response that uses it
    //                 Function<Map<String, Object>, String> toolResponseFn = 
    //                         toolResponses.getOrDefault(selectedTool.name(), 
    //                                 params -> "I have processed the information from the " + selectedTool.name() + " tool.");
                    
    //                 String finalResponse = toolResponseFn.apply(mockParams);
                    
    //                 // Stream the tool-based response
    //                 streamResponse(finalResponse, handler, false);
    //             } catch (InterruptedException e) {
    //                 Thread.currentThread().interrupt();
    //                 handler.onError(e);
    //             }
    //         });
    //     } else {
    //         // Generate a regular response based on pattern matching
    //         String response = findMatchingResponse(userInput);
    //         streamResponse(response, handler, false);
    //     }
    // }

    private String getLastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message instanceof UserMessage userMessage) {
                return userMessage.text();
            }
        }
        return "";
    }

    private String findMatchingResponse(String userInput) {
        for (Map.Entry<Pattern, Function<String, String>> entry : responsePatterns.entrySet()) {
            Matcher matcher = entry.getKey().matcher(userInput);
            if (matcher.matches()) {
                return entry.getValue().apply(userInput);
            }
        }
        
        // Fall back to the default response pattern
        return "I don't have a specific response for that input.";
    }

    // private Map<String, Object> generateMockParameters(dev.langchain4j.model.output.ToolSpecification tool) {
    //     Map<String, Object> params = new HashMap<>();
        
    //     // Simple mock data for common parameter types
    //     if (tool.name().equals("echo")) {
    //         params.put("message", "This is a test message from the mock tool");
    //     } else if (tool.name().equals("calculator")) {
    //         params.put("operation", "add");
    //         params.put("a", 10);
    //         params.put("b", 15);
    //     } else if (tool.name().equals("file_reader")) {
    //         params.put("path", "/mock/path/to/file.txt");
    //     } else if (tool.name().equals("weather")) {
    //         params.put("location", "MockCity");
    //     } else {
    //         // Generic parameters for unknown tools
    //         params.put("query", "mock query for " + tool.name());
    //     }
        
    //     return params;
    // }

    private String mockToolArguments(Map<String, Object> params) {
        StringBuilder json = new StringBuilder("{\n");
        int i = 0;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            json.append("  \"").append(entry.getKey()).append("\": ");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
            
            if (i < params.size() - 1) {
                json.append(",");
            }
            json.append("\n");
            i++;
        }
        json.append("}");
        return json.toString();
    }

    // private void streamResponse(String response, StreamingResponseHandler<AiMessage> handler, boolean isThinking) {
    //     // Split the response into chunks to simulate streaming
    //     List<String> chunks = splitIntoChunks(response, 5, 10);
        
    //     // Stream each chunk with a delay
    //     for (int i = 0; i < chunks.size(); i++) {
    //         int index = i;
    //         executor.schedule(() -> {
    //             String chunk = chunks.get(index);
    //             boolean isFirst = index == 0;
    //             boolean isLast = index == chunks.size() - 1;
                
    //             try {
    //                 if (isFirst) {
    //                     handler.onNext(isThinking ? 
    //                             AiMessage.thinking(chunk) : 
    //                             AiMessage.from(chunk));
    //                 } else {
    //                     handler.onNext(chunk);
    //                 }
                    
    //                 if (isLast) {
    //                     TokenUsage tokenUsage = new TokenUsage(response.length() / 4, chunks.size(), response.length() / 4);
    //                     handler.onComplete(Response.from(
    //                             AiMessage.from(response),
    //                             tokenUsage,
    //                             Duration.ofMillis(streamingDelayMs * chunks.size())
    //                     ));
    //                 }
    //             } catch (Exception e) {
    //                 handler.onError(e);
    //             }
    //         }, streamingDelayMs * i, TimeUnit.MILLISECONDS);
    //     }
    // }

    private List<String> splitIntoChunks(String text, int minChunkSize, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        int position = 0;
        
        while (position < textLength) {
            // Randomize chunk size between min and max
            int chunkSize = minChunkSize + random.nextInt(maxChunkSize - minChunkSize + 1);
            chunkSize = Math.min(chunkSize, textLength - position);
            
            // Find a good breaking point (space, period, etc.)
            int endPos = position + chunkSize;
            if (endPos < textLength) {
                // Try to find a natural break point
                int breakPoint = text.lastIndexOf(' ', endPos);
                int periodPoint = text.lastIndexOf('.', endPos);
                int commaPoint = text.lastIndexOf(',', endPos);
                
                // Use the closest break point that's after our current position
                int naturalBreak = Math.max(Math.max(breakPoint, periodPoint), commaPoint);
                if (naturalBreak > position) {
                    endPos = naturalBreak + 1; // Include the break character
                }
            } else {
                endPos = textLength;
            }
            
            chunks.add(text.substring(position, endPos));
            position = endPos;
        }
        
        return chunks;
    }

    // @Override
    // public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
    //     generate(messages, List.of(), handler);
    // }

    // @Override
    // public void generate(String userMessage, StreamingResponseHandler<AiMessage> handler) {
    //     generate(List.of(UserMessage.from(userMessage)), handler);
    // }

    // @Override
    // public CompletableFuture<Response<AiMessage>> generateAsync(List<ChatMessage> messages, List<dev.langchain4j.model.output.ToolSpecification> tools) {
    //     CompletableFuture<Response<AiMessage>> future = new CompletableFuture<>();
        
    //     this.generate(messages, tools, new StreamingResponseHandler<AiMessage>() {
    //         private final StringBuilder fullResponse = new StringBuilder();
            
    //         @Override
    //         public void onNext(String token) {
    //             fullResponse.append(token);
    //         }

    //         @Override
    //         public void onNext(AiMessage message) {
    //             if (message.text() != null) {
    //                 fullResponse.append(message.text());
    //             }
    //         }

    //         @Override
    //         public void onComplete(Response<AiMessage> response) {
    //             AiMessage message = AiMessage.from(fullResponse.toString());
    //             TokenUsage tokenUsage = response.tokenUsage().orElse(new TokenUsage(0, 0, 0));
    //             future.complete(Response.from(message, tokenUsage, response.duration()));
    //         }

    //         @Override
    //         public void onError(Throwable error) {
    //             future.completeExceptionally(error);
    //         }

    //         @Override
    //         public void onToolExecutionRequest(ToolExecutionRequest toolExecutionRequest) {
    //             // Tool execution in async mode is not fully supported in this mock
    //         }
    //     });
        
    //     return future;
    // }

    // @Override
    // public void generate(String userMessage, List<dev.langchain4j.model.output.ToolSpecification> tools, StreamingResponseHandler<AiMessage> handler) {
    //     generate(List.of(UserMessage.from(userMessage)), tools, handler);
    // }

    // @Override
    // public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler, List<ToolExecutionResultMessage> toolExecutionHistoryMessages) {
    //     List<ChatMessage> allMessages = new ArrayList<>(messages);
    //     allMessages.addAll(toolExecutionHistoryMessages);
    //     generate(allMessages, handler);
    // }

    /**
     * Shuts down the executor service used for streaming responses.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Streamingly generates a response for the specified messages with tool usage support.
     * This matches the method used in the CLI class.
     *
     * @param messages Chat messages
     * @param tools Tool specifications the model can use
     * @return A token stream that can be consumed
     */
    // public TokenStream streamingChatWithTools(List<ChatMessage> messages, List<ToolSpecification> tools) {
    //     return new TokenStream() {
    //         @Override
    //         public void onNext(TokenStreamHandler handler) {
    //             String userInput = getLastUserMessage(messages);
                
    //             // Determine if we should use a tool
    //             boolean shouldUseTool = !tools.isEmpty() && random.nextDouble() < toolCallProbability;
                
    //             if (shouldUseTool) {
    //                 // Select a random tool
    //                 int toolIndex = random.nextInt(tools.size());
    //                 ToolSpecification selectedTool = tools.get(toolIndex);
                    
    //                 // Create a mock tool execution request
    //                 Map<String, Object> mockParams = generateMockParameters(selectedTool);
    //                 String toolName = selectedTool.name();
    //                 String toolArgs = mockToolArguments(mockParams);
                    
    //                 // Stream thinking message
    //                 streamTokensWithDelay(
    //                     "I need to use the " + selectedTool.name() + " tool to help with this.",
    //                     handler,
    //                     true
    //                 );
                    
    //                 // Notify of tool call
    //                 handler.onToolCall(toolName, toolArgs);
                    
    //                 // Process the tool execution result after some delay
    //                 CompletableFuture.runAsync(() -> {
    //                     try {
    //                         // Simulate processing time
    //                         Thread.sleep(500);
                            
    //                         // Generate response that uses the tool result
    //                         Function<Map<String, Object>, String> toolResponseFn = 
    //                                 toolResponses.getOrDefault(selectedTool.name(), 
    //                                         params -> "I have processed the information from the " + selectedTool.name() + " tool.");
                            
    //                         String finalResponse = toolResponseFn.apply(mockParams);
                            
    //                         // Stream the tool-based response
    //                         streamTokensWithDelay(finalResponse, handler, false);
                            
    //                         // Signal completion
    //                         handler.onComplete();
    //                     } catch (InterruptedException e) {
    //                         Thread.currentThread().interrupt();
    //                         handler.onError(e);
    //                     }
    //                 });
    //             } else {
    //                 // Generate a regular response based on pattern matching
    //                 String response = findMatchingResponse(userInput);
    //                 streamTokensWithDelay(response, handler, false);
    //                 handler.onComplete();
    //             }
    //         }
    //     };
    // }
    
    // Map Manorrock Tool to LangChain4j ToolSpecification
    // private dev.langchain4j.model.output.ToolSpecification convertToLangChain4jToolSpec(ToolSpecification tool) {
    //     dev.langchain4j.model.output.ToolSpecification.Builder builder = dev.langchain4j.model.output.ToolSpecification.builder()
    //         .name(tool.name())
    //         .description(tool.description());
        
    //     // Add parameters
    //     if (tool.parameters() != null) {
    //         tool.parameters().forEach((name, schema) -> {
    //             builder.parameter(name, schema);
    //         });
    //     }
        
    //     return builder.build();
    // }
    
    // Convert LangChain4j ToolSpecification to Manorrock format
    // private dev.langchain4j.agent.tool.ToolSpecification convertToManorrockToolSpec(dev.langchain4j.model.output.ToolSpecification tool) {
    //     Map<String, Object> parameters = new HashMap<>();
        
    //     if (tool.parameters() != null) {
    //         tool.parameters().forEach((name, schema) -> {
    //             parameters.put(name, schema);
    //         });
    //     }
        
    //     return ToolSpecification.builder()
    //         .name(tool.name())
    //         .description(tool.description())
    //         .parameters(parameters)
    //         .build();
    // }
}
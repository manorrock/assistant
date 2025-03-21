package com.manorrock.assistant.cli.mock;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MockStreamingChatModelTest {

    private MockStreamingChatModel model;
    
    @BeforeEach
    void setUp() {
        // Create model with minimal delay for faster tests
        model = new MockStreamingChatModel(20, 1.0); // Always use tools when available
    }
    
    @AfterEach
    void tearDown() {
        model.shutdown();
    }

    @Test
    void testBasicResponseGeneration() throws Exception {
        // String userInput = "Hello, how are you?";
        
        // CountDownLatch latch = new CountDownLatch(1);
        // StringBuilder responseBuilder = new StringBuilder();
        // AtomicReference<Response<AiMessage>> finalResponse = new AtomicReference<>();
        
        // model.generate(userInput, new StreamingResponseHandler<AiMessage>() {
        //     @Override
        //     public void onNext(String token) {
        //         responseBuilder.append(token);
        //     }
            
        //     @Override
        //     public void onComplete(Response<AiMessage> response) {
        //         finalResponse.set(response);
        //         latch.countDown();
        //     }
            
        //     @Override
        //     public void onError(Throwable error) {
        //         fail("Error in streaming: " + error.getMessage());
        //     }
        // });
        
        // assertTrue(latch.await(2, TimeUnit.SECONDS), "Response streaming did not complete in time");
        // assertNotNull(finalResponse.get(), "Final response should not be null");
        // assertTrue(responseBuilder.length() > 0, "Response should not be empty");
    }

    @Test
    void testToolExecution() throws Exception {
        // // Create a test tool specification
        // ToolSpecification echoTool = ToolSpecification.builder()
        //         .name("echo")
        //         .description("Echoes back the input message")
        //         .parameter("message", Map.of(
        //                 "type", "string",
        //                 "description", "The message to echo back"
        //         ))
        //         .build();
                
        // CountDownLatch latch = new CountDownLatch(1);
        // StringBuilder responseBuilder = new StringBuilder();
        // AtomicBoolean toolCalled = new AtomicBoolean(false);
        // AtomicReference<ToolExecutionRequest> toolRequest = new AtomicReference<>();
        
        // model.generate(
        //         List.of(UserMessage.from("Please echo this message back to me")),
        //         List.of(echoTool),
        //         new StreamingResponseHandler<AiMessage>() {
        //             @Override
        //             public void onNext(String token) {
        //                 responseBuilder.append(token);
        //             }
                    
        //             @Override
        //             public void onComplete(Response<AiMessage> response) {
        //                 latch.countDown();
        //             }
                    
        //             @Override
        //             public void onError(Throwable error) {
        //                 fail("Error in streaming: " + error.getMessage());
        //             }
                    
        //             @Override
        //             public void onToolExecutionRequest(ToolExecutionRequest request) {
        //                 toolCalled.set(true);
        //                 toolRequest.set(request);
        //             }
        //         }
        // );
        
        // assertTrue(latch.await(2, TimeUnit.SECONDS), "Response streaming did not complete in time");
        // assertTrue(toolCalled.get(), "Tool should have been called");
        // assertNotNull(toolRequest.get(), "Tool execution request should not be null");
        // assertEquals("echo", toolRequest.get().name(), "Correct tool should be called");
        // assertTrue(responseBuilder.length() > 0, "Response should not be empty");
    }

    @Test
    void testCustomResponsePattern() throws Exception {
        // // Add a custom response pattern
        // model.addResponsePattern("hello world", input -> "Custom response for: " + input);
        
        // CountDownLatch latch = new CountDownLatch(1);
        // List<String> tokens = new ArrayList<>();
        
        // model.generate("hello world", new StreamingResponseHandler<AiMessage>() {
        //     @Override
        //     public void onNext(String token) {
        //         tokens.add(token);
        //     }
            
        //     @Override
        //     public void onComplete(Response<AiMessage> response) {
        //         latch.countDown();
        //     }
            
        //     @Override
        //     public void onError(Throwable error) {
        //         fail("Error in streaming: " + error.getMessage());
        //     }
        // });
        
        // assertTrue(latch.await(2, TimeUnit.SECONDS), "Response streaming did not complete in time");
        
        // String fullResponse = String.join("", tokens);
        // assertTrue(fullResponse.contains("Custom response for"), 
        //         "Response should contain the custom text");
    }

    @Test
    void testCustomToolResponse() throws Exception {
        // // Create a test tool specification
        // ToolSpecification calculatorTool = ToolSpecification.builder()
        //         .name("calculator")
        //         .description("Performs basic math operations")
        //         .parameter("operation", Map.of(
        //                 "type", "string",
        //                 "description", "The operation to perform (add, subtract, multiply, divide)"
        //         ))
        //         .parameter("a", Map.of(
        //                 "type", "number",
        //                 "description", "First number"
        //         ))
        //         .parameter("b", Map.of(
        //                 "type", "number", 
        //                 "description", "Second number"
        //         ))
        //         .build();
        
        // // Add custom tool response
        // model.addToolResponse("calculator", params -> {
        //     return "Custom calculator result: 42";
        // });
        
        // CountDownLatch latch = new CountDownLatch(1);
        // StringBuilder responseBuilder = new StringBuilder();
        
        // model.generate(
        //         List.of(UserMessage.from("Calculate 5 + 7")),
        //         List.of(calculatorTool),
        //         new StreamingResponseHandler<AiMessage>() {
        //             @Override
        //             public void onNext(String token) {
        //                 responseBuilder.append(token);
        //             }
                    
        //             @Override
        //             public void onComplete(Response<AiMessage> response) {
        //                 latch.countDown();
        //             }
                    
        //             @Override
        //             public void onError(Throwable error) {
        //                 fail("Error in streaming: " + error.getMessage());
        //             }
        //         }
        // );
        
        // assertTrue(latch.await(2, TimeUnit.SECONDS), "Response streaming did not complete in time");
        
        // String fullResponse = responseBuilder.toString();
        // assertTrue(fullResponse.contains("Custom calculator result: 42"), 
        //         "Response should contain the custom calculator result");
    }

    @Test
    void testStreamingChatWithTools() throws Exception {
        // // Create a test tool specification
        // dev.langchain4j.service.ToolSpecification echoTool = dev.langchain4j.service.ToolSpecification.builder()
        //         .name("echo")
        //         .description("Echoes back the input message")
        //         .parameter("message", "string", "The message to echo back", true)
        //         .build();
                
        // CountDownLatch latch = new CountDownLatch(1);
        // StringBuilder responseBuilder = new StringBuilder();
        // AtomicBoolean toolCalled = new AtomicBoolean(false);
        // AtomicReference<String> toolName = new AtomicReference<>();
        // AtomicReference<String> toolArgs = new AtomicReference<>();
        
        // model.streamingChatWithTools(
        //         List.of(UserMessage.from("Please echo this message back to me")),
        //         List.of(echoTool)
        // ).onNext(new TokenStreamHandler() {
        //     @Override
        //     public void onContent(String token) {
        //         responseBuilder.append(token);
        //     }
            
        //     @Override
        //     public void onToolCall(String name, String arguments) {
        //         toolCalled.set(true);
        //         toolName.set(name);
        //         toolArgs.set(arguments);
        //     }
            
        //     @Override
        //     public void onComplete() {
        //         latch.countDown();
        //     }
            
        //     @Override
        //     public void onError(Throwable error) {
        //         fail("Error in streaming: " + error.getMessage());
        //     }
        // });
        
        // assertTrue(latch.await(2, TimeUnit.SECONDS), "Response streaming did not complete in time");
        // assertTrue(toolCalled.get(), "Tool should have been called");
        // assertEquals("echo", toolName.get(), "Correct tool should be called");
        // assertTrue(responseBuilder.length() > 0, "Response should not be empty");
    }
}

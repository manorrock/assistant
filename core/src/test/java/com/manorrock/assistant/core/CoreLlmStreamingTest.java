package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.manorrock.assistant.api.LlmStreamingResponseHandler;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * Unit tests for the streaming functionality of CoreLlm.
 */
public class CoreLlmStreamingTest {

    private CoreLlm coreLlm;
    private StreamingChatLanguageModel mockStreamingModel;

    @BeforeEach
    public void setUp() {
        mockStreamingModel = mock(StreamingChatLanguageModel.class);
        coreLlm = new CoreLlm(null);
        coreLlm.setFunctionCallingEnabled(false); // Disable tool integration for basic tests
        coreLlm.streamingModel = mockStreamingModel;
    }

    @Test
    public void testProcessStreamingWithEmptyPrompt() {
        // Create a test handler that records tokens and notifies when complete
        TestHandler handler = new TestHandler();
        
        // Call the streaming method with an empty prompt
        coreLlm.processStreaming("", handler);
        
        // Verify that the handler received the expected message for empty prompt
        String expectedResponse = "I need some input to provide a helpful response.";
        assertEquals(expectedResponse, handler.getCollectedTokens());
        assertEquals(expectedResponse, handler.getCompleteResponse());
    }
    
    @Test
    public void testProcessStreamingWithoutTools() throws Exception {
        // Setup test data
        final String prompt = "Hello, how are you?";
        final String[] tokens = {"Hello", ",", " ", "I", " ", "am", " ", "doing", " ", "well", "!"};
        final String expectedResponse = "Hello, I am doing well!";
        
        // Create a test handler
        TestHandler handler = new TestHandler();
        
        // Configure the mock streaming model to call the StreamingChatResponseHandler methods
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                StreamingChatResponseHandler handler = invocation.getArgument(1);
                
                // Simulate token generation with onPartialResponse
                for (String token : tokens) {
                    handler.onPartialResponse(token);
                }
                
                // Create an AI message for the complete response
                AiMessage aiMessage = AiMessage.from(expectedResponse);
                // Create ChatResponse directly
                ChatResponse response = ChatResponse.builder()
                        .aiMessage(aiMessage)
                        .build();
                
                // Complete the stream
                handler.onCompleteResponse(response);
                return null;
            }
        }).when(mockStreamingModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));
        
        // Call the method being tested
        coreLlm.processStreaming(prompt, handler);
        
        // Wait for async processing to complete
        handler.await(5);
        
        // Verify results
        assertEquals(expectedResponse, handler.getCollectedTokens());
        assertEquals(expectedResponse, handler.getCompleteResponse());
        verify(mockStreamingModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));
    }
    
    @Test
    public void testStreamingErrorHandling() throws Exception {
        // Setup test data
        String prompt = "Generate an error";
        Exception testException = new RuntimeException("Test error");
        
        // Create a test handler
        TestHandler handler = new TestHandler();
        
        // Configure the mock to throw an error
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                StreamingChatResponseHandler handler = invocation.getArgument(1);
                handler.onError(testException);
                return null;
            }
        }).when(mockStreamingModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));
        
        // Call the method being tested
        coreLlm.processStreaming(prompt, handler);
        
        // Wait for async processing to complete
        handler.await(5);
        
        // Verify the error was passed to the handler
        assertEquals(testException, handler.getError());
    }
    
    /**
     * A test implementation of LlmStreamingResponseHandler that records tokens and responses
     * for verification in tests.
     */
    private static class TestHandler implements LlmStreamingResponseHandler {
        private final StringBuilder tokensBuilder = new StringBuilder();
        private String completeResponse;
        private Throwable error;
        private final CountDownLatch latch = new CountDownLatch(1);
        
        @Override
        public void onToken(String token) {
            tokensBuilder.append(token);
        }
        
        @Override
        public void onComplete(String fullResponse) {
            this.completeResponse = fullResponse;
            latch.countDown();
        }
        
        @Override
        public void onError(Throwable error) {
            this.error = error;
            latch.countDown();
        }
        
        public String getCollectedTokens() {
            return tokensBuilder.toString();
        }
        
        public String getCompleteResponse() {
            return completeResponse;
        }
        
        public Throwable getError() {
            return error;
        }
        
        public void await(int timeoutSeconds) throws InterruptedException {
            if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                fail("Test timed out waiting for streaming to complete");
            }
        }
    }
}

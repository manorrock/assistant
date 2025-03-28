package com.manorrock.assistant.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.data.message.AiMessage;

/**
 * The test class for the LLM.
 */
class LlmTest {

    /**
     * Test the Ollama LLM if the OLLAMA_ENDPOINT is specified.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "OLLAMA_ENDPOINT", matches = ".+")
    void testOllamaProcess() {
        LlmConfiguration config = new LlmConfiguration(
            System.getenv("OLLAMA_ENDPOINT"),
            "llama3.1",
            "OLLAMA",
            "",
            0.7
        );
        
        Llm llm = new Llm();
        llm.setConfiguration(config);
        llm.initialize();
        LlmRequest request = new LlmRequest();
        request.setRequest("What is 2+2?");
        LlmResponse response = llm.process(request);
        assertNotNull(response.getContent(), "Response content should not be null");
        assertFalse(response.getContent().isEmpty(), "Response content should not be empty");
    }
    
    /**
     * Test the Echo model provider to make sure we can use a LlmModelProvider to deliver new models.
     */
    @Test
    void testEchoModelProvider() {
        LlmConfiguration config = new LlmConfiguration(
            "",
            "",
            "ECHO",
            "",
            0.7
        );
        Llm llm = new Llm();
        llm.setConfiguration(config);
        llm.setProvider(new EchoModelProvider());
        llm.initialize();
        LlmRequest request = new LlmRequest();
        String testRequest = "What is 2+2?";
        request.setRequest(testRequest);
        LlmResponse response = llm.process(request);
        assertFalse(response.getContent().isEmpty(), "Response content should not be empty");
        assertEquals(testRequest, response.getContent(), "Response content should match input");
    }

    /**
     * Delivers an echo model provider.
     */
    private static class EchoModelProvider implements LlmModelProvider<StreamingChatLanguageModel> {
        @Override
        public StreamingChatLanguageModel createModel(LlmConfiguration config) {
            return new EchoStreamingChatLanguageModel();
        }
    }
    
    /**
     * Delivers an echo streaming chat language model.
     */
    private static class EchoStreamingChatLanguageModel implements StreamingChatLanguageModel {

        @Override
        public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
            chat(chatRequest.messages().get(0).text(), handler);
        }

        @Override
        public void chat(String userMessage, StreamingChatResponseHandler handler) {
            handler.onPartialResponse(userMessage);
            handler.onCompleteResponse(
                ChatResponse.builder()
                    .finishReason(FinishReason.STOP)
                    .aiMessage(new AiMessage(userMessage))
                    .build());
        }
    }
}

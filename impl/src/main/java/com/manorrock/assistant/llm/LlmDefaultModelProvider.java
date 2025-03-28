package com.manorrock.assistant.llm;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

/**
 * The default model provider for Large Language Models.
 */
public class LlmDefaultModelProvider implements LlmModelProvider<StreamingChatLanguageModel> {

    /**
     * Create the model.
     * 
     * @return the model.
     */    
    @Override
    public StreamingChatLanguageModel createModel(LlmConfiguration config) {
        return switch (config.vendor().toUpperCase()) {
            case "OLLAMA" -> OllamaStreamingChatModel.builder()
                .baseUrl(config.endpoint().substring(0, config.endpoint().lastIndexOf("/api/chat")))
                .modelName(config.model())
                .temperature(config.temperature())
                .build();
            case "OPENAI" -> OpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model())
                .temperature(config.temperature())
                .build();
            case "AZURE_OPENAI" -> AzureOpenAiStreamingChatModel.builder()
                .endpoint(config.endpoint())
                .apiKey(config.apiKey())
                .deploymentName(config.model())
                .temperature(config.temperature())
                .build();
            default -> throw new IllegalArgumentException("Unknown vendor: " + config.vendor());
        };
    }
}

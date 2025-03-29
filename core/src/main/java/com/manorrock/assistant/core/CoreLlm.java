package com.manorrock.assistant.core;

import java.util.Properties;

import com.manorrock.assistant.api.Llm;

import dev.langchain4j.data.message.AiMessage;
import static dev.langchain4j.data.message.UserMessage.userMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
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

    @Override
    public String process(String prompt) {
        chatMemory.add(userMessage(prompt));
        AiMessage answer = model.chat(chatMemory.messages()).aiMessage();
        chatMemory.add(answer);
        return answer.text();
    }

    @Override
    public void destroy() {
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

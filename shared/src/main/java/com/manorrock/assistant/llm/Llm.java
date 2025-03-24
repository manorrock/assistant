/*
 * Copyright (c) 2002-2025, Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright 
 *        notice, this list of conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.manorrock.assistant.llm;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * The LLM that is the reusable component when interacting with a Large Language Model.
 */
public class Llm {
    
    /**
     * Stores the configuration.
     */
    private LlmConfiguration configuration;

    /**
     * Stores the Large Language Model.
     */
    private StreamingChatLanguageModel model;

    /**
     * Stores the provider.
     */
    private LlmModelProvider<StreamingChatLanguageModel> provider;

    /**
     * Constructor.
     */
    public Llm() {
        this.configuration = LlmConfiguration.defaultConfig();
        this.provider = new LlmDefaultModelProvider();
    }

    /**
     * Get the configuration.
     * 
     * @return the configuration
     */
    public LlmConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Set the configuration.
     * 
     * @param configuration the configuration
     */
    public void setConfiguration(LlmConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Set the LLM model provider.
     * 
     * @param provider the provider
     */
    @SuppressWarnings("unchecked")
    public void setProvider(@SuppressWarnings("rawtypes") LlmModelProvider provider) {
        this.provider = provider;
        this.model = (StreamingChatLanguageModel) provider.createModel(configuration);
    }

    /**
     * Process an LLM request.
     * 
     * @param request the request to process
     * @return the response from the LLM
     */
    public LlmResponse process(LlmRequest request) {
        String mavenValue = getMavenSettingsValue(request.getRequest());
        String envValue = System.getenv(request.getRequest());
        
        if (mavenValue != null || envValue != null) {
            LlmResponse response = new LlmResponse();
            response.setContent(mavenValue != null ? mavenValue : envValue);
            return response;
        }

        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final StringBuilder responseBuilder = new StringBuilder();

            ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.from(request.getRequest()))
                .build();
            
            model.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    responseBuilder.append(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse chatResponse) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    responseBuilder.append("Error: ").append(error.getMessage());
                    latch.countDown();
                }
            });

            latch.await();
            
            LlmResponse response = new LlmResponse();
            response.setContent(responseBuilder.toString());
            return response;

        } catch (InterruptedException e) {
            LlmResponse response = new LlmResponse();
            response.setContent("Error: " + e.getMessage());
            return response;
        }
    }

    /**
     * Get value from Maven settings.xml
     * 
     * @param key the key to look for
     * @return the value or null if not found
     */
    private String getMavenSettingsValue(String key) {
        String userHome = System.getProperty("user.home");
        File settingsFile = new File(userHome + "/.m2/settings.xml");
        if (!settingsFile.exists()) {
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(settingsFile);
            NodeList properties = doc.getElementsByTagName("properties");
            if (properties.getLength() > 0) {
                Element props = (Element) properties.item(0);
                NodeList children = props.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(key)) {
                        return node.getTextContent();
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore any parsing errors
        }
        return null;
    }

    /**
     * Initialize the LLM.
     */
    public void initialize() {
        model = provider.createModel(configuration);
    }
}

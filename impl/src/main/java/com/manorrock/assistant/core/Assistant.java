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
package com.manorrock.assistant.core;

import com.manorrock.assistant.CommandRegistry;
import com.manorrock.assistant.command.CommandRegistryImpl;
import com.manorrock.assistant.llm.Llm;
import com.manorrock.assistant.llm.LlmRequest;
import com.manorrock.assistant.llm.LlmResponse;

/**
 * The Assistant class is the reusable component of the Assistant application.
 */
public class Assistant {

    /**
     * Stores the command registry.
     */
    private CommandRegistry commandRegistry;

    /**
     * Stores the LLM.
     */
    private Llm llm;
    
    /**
     * Constructor.
     */
    public Assistant() {
        this.llm = new Llm();
        this.commandRegistry = new CommandRegistryImpl();
    }

    /**
     * Get the command registry.
     * 
     * @return the command registry
     */
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * Get the LLM.
     */
    public Llm getLlm() {
        return llm;
    }

    /**
     * Set the command registry.
     * 
     * @param commandRegistry the command registry
     */
    public void setCommandRegistry(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    /**
     * Set the LLM.
     * 
     * @param llm the LLM
     */
    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    /**
     * Process an assistant request.
     * 
     * @param request the assistant request
     * @return the assistant response
     */
    public AssistantResponse process(AssistantRequest request) {
        LlmRequest llmRequest = new LlmRequest();
        llmRequest.setRequest(request.getPrompt());
        LlmResponse llmResponse = llm.process(llmRequest);
        return new AssistantResponse(llmResponse.getContent());
    }
}

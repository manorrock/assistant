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
package com.manorrock.assistant.api;

import java.time.LocalDateTime;

/**
 * Interface defining a message that can be sent, received, or processed by an Assistant.
 */
public interface AssistantMessage {
    
    /**
     * Get the content of the message.
     * 
     * @return the message content
     */
    String getContent();
    
    /**
     * Set the content of the message.
     * 
     * @param content the message content
     */
    void setContent(String content);
    
    /**
     * Get the message type (e.g., "user", "assistant", "system").
     * 
     * @return the message type
     */
    String getType();
    
    /**
     * Set the message type.
     * 
     * @param type the message type
     */
    void setType(String type);
    
    /**
     * Get the timestamp when the message was created.
     * 
     * @return the timestamp
     */
    LocalDateTime getTimestamp();
    
    /**
     * Set the timestamp.
     * 
     * @param timestamp the timestamp
     */
    void setTimestamp(LocalDateTime timestamp);
    
    /**
     * Get the ID of the message.
     * 
     * @return the message ID
     */
    String getId();
    
    /**
     * Set the ID of the message.
     * 
     * @param id the message ID
     */
    void setId(String id);
}
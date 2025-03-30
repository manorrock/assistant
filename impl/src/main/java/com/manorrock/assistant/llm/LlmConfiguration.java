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

@Deprecated
public class LlmConfiguration {
  private String endpoint;
  private String model;
  private String vendor;
  private String apiKey;
  private double temperature;

  public LlmConfiguration(String endpoint, String model, String vendor, String apiKey, double temperature) {
    this.endpoint = endpoint;
    this.model = model;
    this.vendor = vendor;
    this.apiKey = apiKey;
    this.temperature = temperature;
  }

  public String endpoint() {
    return endpoint;
  }

  public String model() {
    return model;
  }

  public String vendor() {
    return vendor;
  }

  public String apiKey() {
    return apiKey;
  }

  public double temperature() {
    return temperature;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public void setTemperature(double temperature) {
    this.temperature = temperature;
  }

  public static LlmConfiguration defaultConfig() {
    return new LlmConfiguration("http://localhost:11434/api/chat", "llama3.1", "OLLAMA", "", 0.75);
  }
}

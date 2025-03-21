package com.manorrock.assistant.shared;

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
    return new LlmConfiguration("http://localhost:11434/api/chat", "llama3.1", "OLLAMA", "", 0.0);
  }
}
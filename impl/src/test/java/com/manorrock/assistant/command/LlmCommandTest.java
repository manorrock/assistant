package com.manorrock.assistant.command;

import com.manorrock.assistant.llm.LlmConfiguration;
import org.junit.Test;
import java.io.InputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class LlmCommandTest {

    @Test
    public void testGetDescription() {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        String description = command.getDescription();
        
        assertThat(description, containsString("Display and manage LLM configuration"));
        assertThat(description, containsString("/llm vendor <name>"));
        assertThat(description, containsString("/llm model <name>"));
        assertThat(description, containsString("/llm endpoint <url>"));
        assertThat(description, containsString("/llm apikey <key>"));
        assertThat(description, containsString("/llm temperature <value>"));
    }
    
    @Test
    public void testExecuteToStringWithEmptyInput() {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        String result = command.executeToString("");
        
        assertThat(result, containsString("Current LLM Configuration:"));
        assertThat(result, containsString("Vendor: TEST_VENDOR"));
        assertThat(result, containsString("Model: test-model"));
        assertThat(result, containsString("Endpoint: http://test.com/api/chat"));
        assertThat(result, containsString("API Key: test****1234"));
        assertThat(result, containsString("Temperature: 0.7"));
    }
    
    @Test
    public void testExecuteToStringWithNullInput() {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        String result = command.executeToString(null);
        
        assertThat(result, containsString("Current LLM Configuration:"));
    }
    
    @Test
    public void testExecuteToStringWithStatusCommand() {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        String result = command.executeToString("status");
        
        assertThat(result, containsString("Current LLM Configuration:"));
    }
    
    @Test
    public void testSetVendor() {
        LlmConfiguration[] capturedConfig = new LlmConfiguration[1];
        LlmCommand command = new LlmCommand(
            () -> createDefaultConfig(),
            config -> capturedConfig[0] = config
        );
        
        String result = command.executeToString("vendor openai");
        
        assertThat(result, is("Vendor updated to: OPENAI"));
        assertNotNull(capturedConfig[0]);
        assertEquals("OPENAI", capturedConfig[0].vendor());
        assertEquals("test-model", capturedConfig[0].model());
        assertEquals("http://test.com/api/chat", capturedConfig[0].endpoint());
        assertEquals("test12341234", capturedConfig[0].apiKey());
        assertEquals(0.7, capturedConfig[0].temperature(), 0.001);
    }
    
    @Test
    public void testGetVendor() {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        String result = command.executeToString("vendor");
        
        assertThat(result, is("Current vendor: TEST_VENDOR"));
    }
    
    @Test
    public void testSetModel() {
        LlmConfiguration[] capturedConfig = new LlmConfiguration[1];
        LlmCommand command = new LlmCommand(
            () -> createDefaultConfig(),
            config -> capturedConfig[0] = config
        );
        
        String result = command.executeToString("model gpt-4");
        
        assertThat(result, is("Model updated to: gpt-4"));
        assertNotNull(capturedConfig[0]);
        assertEquals("TEST_VENDOR", capturedConfig[0].vendor());
        assertEquals("gpt-4", capturedConfig[0].model());
        assertEquals("http://test.com/api/chat", capturedConfig[0].endpoint());
        assertEquals("test12341234", capturedConfig[0].apiKey());
        assertEquals(0.7, capturedConfig[0].temperature(), 0.001);
    }
    
    @Test
    public void testGetModel() {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        String result = command.executeToString("model");
        
        assertThat(result, is("Current model: test-model"));
    }
    
    @Test
    public void testSetEndpointWithProtocolMissing() {
        LlmConfiguration[] capturedConfig = new LlmConfiguration[1];
        LlmCommand command = new LlmCommand(
            () -> createDefaultConfig(),
            config -> capturedConfig[0] = config
        );
        
        String result = command.executeToString("endpoint api.example.com");
        
        assertThat(result, is("Endpoint updated to: http://api.example.com/api/chat"));
        assertNotNull(capturedConfig[0]);
        assertEquals("http://api.example.com/api/chat", capturedConfig[0].endpoint());
    }
    
    @Test
    public void testSetEndpointWithProtocolIncluded() {
        LlmConfiguration[] capturedConfig = new LlmConfiguration[1];
        LlmCommand command = new LlmCommand(
            () -> createDefaultConfig(),
            config -> capturedConfig[0] = config
        );
        
        String result = command.executeToString("endpoint https://api.example.com");
        
        assertThat(result, is("Endpoint updated to: https://api.example.com/api/chat"));
        assertNotNull(capturedConfig[0]);
        assertEquals("https://api.example.com/api/chat", capturedConfig[0].endpoint());
    }
    
    @Test
    public void testGetEndpoint() {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        String result = command.executeToString("endpoint");
        
        assertThat(result, is("Current endpoint: http://test.com/api/chat"));
    }
    
    @Test
    public void testSetApiKey() {
        LlmConfiguration[] capturedConfig = new LlmConfiguration[1];
        LlmCommand command = new LlmCommand(
            () -> createDefaultConfig(),
            config -> capturedConfig[0] = config
        );
        
        String result = command.executeToString("apikey sk-12345abcdef");
        
        assertThat(result, is("API key updated"));
        assertNotNull(capturedConfig[0]);
        assertEquals("sk-12345abcdef", capturedConfig[0].apiKey());
    }
    
    @Test
    public void testGetApiKey() {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        String result = command.executeToString("apikey");
        
        assertThat(result, is("Current API key: test****1234"));
    }
    
    @Test
    public void testSetValidTemperature() {
        LlmConfiguration[] capturedConfig = new LlmConfiguration[1];
        LlmCommand command = new LlmCommand(
            () -> createDefaultConfig(),
            config -> capturedConfig[0] = config
        );
        
        String result = command.executeToString("temperature 0.3");
        
        assertThat(result, is("Temperature updated to: 0.3"));
        assertNotNull(capturedConfig[0]);
        assertEquals(0.3, capturedConfig[0].temperature(), 0.001);
    }
    
    @Test
    public void testSetInvalidTemperatureValue() {
        LlmCommand command = new LlmCommand(
            () -> createDefaultConfig(),
            config -> {}
        );
        
        String result = command.executeToString("temperature 1.5");
        
        assertThat(result, is("Temperature must be between 0.0 and 1.0"));
    }
    
    @Test
    public void testSetInvalidTemperatureFormat() {
        LlmCommand command = new LlmCommand(
            () -> createDefaultConfig(),
            config -> {}
        );
        
        String result = command.executeToString("temperature xyz");
        
        assertThat(result, is("Invalid temperature format. Must be a number between 0.0 and 1.0"));
    }
    
    @Test
    public void testGetTemperature() {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        String result = command.executeToString("temperature");
        
        assertThat(result, is("Current temperature: 0.7"));
    }
    
    @Test
    public void testUnknownSubcommand() {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        String result = command.executeToString("invalid");
        
        assertThat(result, containsString("Unknown subcommand: invalid"));
        assertThat(result, containsString("Available subcommands:"));
    }
    
    @Test
    public void testExecuteToStream() throws IOException {
        LlmCommand command = new LlmCommand(() -> createDefaultConfig(), config -> {});
        InputStream stream = command.executeToStream("model");
        
        byte[] buffer = new byte[100];
        int bytesRead = stream.read(buffer);
        String result = new String(buffer, 0, bytesRead);
        
        assertThat(result, is("Current model: test-model"));
    }
    
    @Test
    public void testMaskApiKeyShort() {
        LlmConfiguration[] capturedConfig = new LlmConfiguration[1];
        LlmCommand command = new LlmCommand(
            () -> capturedConfig[0] != null ? capturedConfig[0] : createDefaultConfig(),
            config -> capturedConfig[0] = config
        );
        
        String result = command.executeToString("apikey abc");
        
        assertThat(result, is("API key updated"));
        // We can't directly test maskApiKey since it's private, but we can test it indirectly
        result = command.executeToString("apikey");
        assertThat(result, is("Current API key: ***"));
    }
    
    @Test
    public void testMaskApiKeyEmpty() {
        LlmConfiguration emptyKeyConfig = new LlmConfiguration(
            "http://test.com/api/chat", "test-model", "TEST_VENDOR", "", 0.7
        );
        LlmCommand command = new LlmCommand(() -> emptyKeyConfig, config -> {});
        String result = command.executeToString("apikey");
        
        assertThat(result, is("Current API key: (not set)"));
    }
    
    private LlmConfiguration createDefaultConfig() {
        return new LlmConfiguration(
            "http://test.com/api/chat",
            "test-model",
            "TEST_VENDOR",
            "test12341234",
            0.7
        );
    }
}
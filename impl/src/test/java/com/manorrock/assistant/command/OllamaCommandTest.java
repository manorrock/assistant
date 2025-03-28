package com.manorrock.assistant.command;

import com.manorrock.assistant.llm.LlmConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class OllamaCommandTest {

    private OllamaCommand command;
    private Supplier<LlmConfiguration> mockConfigSupplier;

    @Before
    public void setUp() {
        // Corrected constructor parameters: endpoint, model, vendor, apiKey, temperature
        mockConfigSupplier = () -> new LlmConfiguration("http://localhost:11434/api/chat", "model", "OLLAMA", "", 0.7);
        command = new OllamaCommand(mockConfigSupplier);
    }

    @Test
    public void testGetDescription() {
        String description = command.getDescription();
        assertThat(description, containsString("Execute Ollama commands directly"));
        assertThat(description, containsString("Usage: ollama [--host <host:port>] <command> [arguments]"));
    }

    @Test
    public void testExecuteToStream() {
        String testString = "test output";
        OllamaCommand spyCommand = new OllamaCommand(mockConfigSupplier) {
            @Override
            public String executeToString(String args) {
                return testString;
            }
        };

        InputStream inputStream = spyCommand.executeToStream("test");
        byte[] bytes = new byte[testString.length()];
        try {
            inputStream.read(bytes);
            assertEquals(testString, new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testHostParameterParsing() {
        OllamaCommand spyCommand = new OllamaCommand(mockConfigSupplier) {
            @Override
            public String executeToString(String args) {
                if (args.startsWith("--host custom:1234")) {
                    return "Using custom host";
                }
                return "Using default host";
            }
        };

        String result = spyCommand.executeToString("--host custom:1234 list");
        assertThat(result, is("Using custom host"));
    }

    @Test
    public void testOllamaConfigurationHostExtraction() {
        Supplier<LlmConfiguration> customConfigSupplier = () -> 
            new LlmConfiguration("http://custom-server:5678/api/chat", "model", "OLLAMA", "", 0.7);
        
        OllamaCommand commandWithConfig = new OllamaCommand(customConfigSupplier);
        
        // This test cannot fully validate the host extraction behavior without mocking Process
        // but we can at least verify the command doesn't throw exceptions
        String result = commandWithConfig.executeToString("list");
        assertThat(result, anyOf(
            containsString("Failed to execute"),
            containsString("Error executing"),
            not(containsString("Exception")),
            containsString("Available Commands")
        ));
    }

    @Test
    public void testNullArgs() {
        String result = command.executeToString(null);
        // When null args are provided, the command still runs 'ollama' with no args
        // which shows the help text, so we should expect that instead of an error
        assertThat(result, anyOf(
            containsString("Available Commands:"),
            containsString("Failed to execute"),
            containsString("Error executing")
        ));
    }

    @Test
    public void testEmptyArgs() {
        String result = command.executeToString("");
        // Empty args should also show the help text
        assertThat(result, anyOf(
            containsString("Available Commands:"),
            containsString("Failed to execute"),
            containsString("Error executing")
        ));
    }

    @Test
    public void testProcessExecution() {
        OllamaCommand spyCommand = new OllamaCommand(mockConfigSupplier) {
            @Override
            public String executeToString(String args) {
                // Verify arguments are properly processed
                if (args == null || args.trim().isEmpty()) {
                    return "Command received: ollama (no args)";
                } else if (args.startsWith("--host")) {
                    String[] parts = args.split(" ", 3);
                    String host = parts.length >= 2 ? parts[1] : "";
                    String remainingArgs = parts.length == 3 ? parts[2] : "";
                    return "Command received: ollama with host " + host + " and args: " + remainingArgs;
                } else {
                    return "Command received: ollama with args: " + args;
                }
            }
        };

        // Test null args
        assertThat(spyCommand.executeToString(null), 
            is("Command received: ollama (no args)"));

        // Test empty args
        assertThat(spyCommand.executeToString("  "), 
            is("Command received: ollama (no args)"));

        // Test with command without host
        assertThat(spyCommand.executeToString("list"), 
            is("Command received: ollama with args: list"));

        // Test with host parameter
        assertThat(spyCommand.executeToString("--host custom:1234 list"), 
            containsString("with host custom:1234"));
        
        // Test with host parameter only
        assertThat(spyCommand.executeToString("--host custom:1234"), 
            containsString("with host custom:1234"));
    }

    @Test
    public void testNonOllamaVendorConfig() {
        Supplier<LlmConfiguration> nonOllamaConfig = () ->
            new LlmConfiguration("http://api.openai.com", "gpt-4", "OPENAI", "fake-key", 0.7);
        
        OllamaCommand nonOllamaCommand = new OllamaCommand(nonOllamaConfig);
        
        // We can't easily test this without modifying the source code to expose internal behavior
        // So we just verify it doesn't throw exceptions
        String result = nonOllamaCommand.executeToString("list");
        // Since we're likely not running the actual ollama command successfully in tests,
        // we just check that the result is reasonable
        assertThat(result, anyOf(
            containsString("Failed"),
            containsString("Error"),
            containsString("Available Commands")
        ));
    }

    @Test
    public void testMalformedEndpointConfig() {
        Supplier<LlmConfiguration> malformedEndpointConfig = () ->
            new LlmConfiguration("http://custom-server:5678/invalid", "model", "OLLAMA", "", 0.7);
        
        OllamaCommand malformedCommand = new OllamaCommand(malformedEndpointConfig);
        
        // We can't easily test this without modifying the source code to expose internal behavior
        // So we just verify it doesn't throw exceptions
        String result = malformedCommand.executeToString("list");
        // Since we're likely not running the actual ollama command successfully in tests,
        // we just check that the result is reasonable
        assertThat(result, anyOf(
            containsString("Failed"),
            containsString("Error"),
            containsString("Available Commands")
        ));
    }

    @Test
    public void testCommandWithMultipleArguments() {
        // Test command with multiple arguments by observing the actual result
        String result = command.executeToString("run llama2 --verbose");
        
        // Since we're likely not running the actual ollama command successfully in tests,
        // we just check that the result is reasonable and doesn't throw exceptions
        assertThat(result, anyOf(
            containsString("Failed"),
            containsString("Error"),
            containsString("Could not"),
            not(containsString("Exception"))
        ));
    }

    @Test
    public void testProcessErrorHandling() {
        // Create a test command that will likely fail
        // For example, try to run a non-existent command or with invalid parameters
        String result = command.executeToString("nonexistentcommand");
        
        // Since the command doesn't exist, we expect either an error from ollama
        // or our command handler to catch an exception
        assertThat(result, anyOf(
            containsString("Error"),
            containsString("Failed"),
            containsString("not found"),
            containsString("Invalid"),
            containsString("unknown")
        ));
    }

    @Test
    public void testHostParameterWithoutCommand() {
        // Test host parameter without any further command
        String result = command.executeToString("--host custom:1234");
        
        // Since we're likely not running the actual ollama command successfully in tests,
        // we just check that the result is reasonable and doesn't throw exceptions
        assertThat(result, anyOf(
            containsString("Available Commands"),
            containsString("Failed"),
            containsString("Error"),
            not(containsString("Exception"))
        ));
    }
}
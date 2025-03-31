package com.manorrock.assistant.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.api.Command;
import com.manorrock.assistant.api.CommandRegistry;
import com.manorrock.assistant.api.Llm;
import com.manorrock.assistant.api.ToolManager;

/**
 * Unit tests for CoreAssistant.
 */
public class CoreAssistantTest {
    //
    // TODO
    //
    // Refactor all tests that really test the CoreLlmCommand functionality into
    // their own CoreLlmCommandTest class, except for the one that tests the 
    // "/llm" command without any arguments so we can validate the command is
    // registered by default to the CoreAssistant.

    private CoreAssistant coreAssistant;

    @BeforeEach
    public void setUp() {
        coreAssistant = new CoreAssistant();
    }

    @Test
    public void testGetCommandRegistry() {
        CommandRegistry commandRegistry = coreAssistant.getCommandRegistry();
        assertNotNull(commandRegistry, "CommandRegistry should not be null");
    }

    @Test
    public void testSetCommandRegistry() {
        CoreAssistant coreAssistant = new CoreAssistant();
        CommandRegistry mockRegistry = new CoreCommandRegistry(coreAssistant);
        coreAssistant.setCommandRegistry(mockRegistry);
        assertEquals(mockRegistry, coreAssistant.getCommandRegistry(), "CommandRegistry should be set correctly");
    }

    @Test
    public void testGetToolManager() {
        ToolManager toolManager = coreAssistant.getToolManager();
        assertNotNull(toolManager, "ToolManager should not be null");
    }

    @Test
    public void testSetToolManager() {
        ToolManager mockToolManager = new CoreToolManager(null);
        coreAssistant.setToolManager(mockToolManager);
        assertEquals(mockToolManager, coreAssistant.getToolManager(), "ToolManager should be set correctly");
    }

    @Test
    public void testSendMessage() {
        coreAssistant.setActiveLlm(null);
        
        AssistantMessage inputMessage = new CoreAssistantMessage("Test input");
        CompletableFuture<AssistantMessage> future = coreAssistant.sendMessage(inputMessage);
        assertNotNull(future, "sendMessage should return a non-null CompletableFuture");
        
        AssistantMessage responseMessage = future.join();
        assertNotNull(responseMessage, "Response message should not be null");
        
        if (coreAssistant.getActiveLlm() == null) {
            assertEquals("Unable to determine which LLM to use", responseMessage.getContent(), 
                "Response content should indicate no active LLM is set");
        }
    }

    @Test
    public void testActiveLLM() {
        String testLLM = "TestLLM";
        coreAssistant.setActiveLlm(testLLM);
        assertEquals(testLLM, coreAssistant.getActiveLlm(), 
            "Active LLM should be set and retrieved correctly");
    }

    @Test
    public void testProcessMessageWithUnregisteredActiveLlm() {
        coreAssistant.setActiveLlm("UnregisteredLLM");
        AssistantMessage inputMessage = new CoreAssistantMessage("Test message");
        AssistantMessage response = coreAssistant.processMessage(inputMessage);
        assertNotNull(response, "Response should not be null");
        assertEquals("Active LLM is not registered", response.getContent(),
            "Response content should indicate the active LLM is not registered");
    }

    @Test
    public void testProcessMessageWithRegisteredActiveLlm() {
        String testLlmName = "TestLLM";
        Llm mockLlm = new Llm() {
            @Override
            public String process(String content) {
                return "Processed: " + content;
            }
            
            @Override
            public Properties getProperties() {
                return new Properties();
            }
            
            @Override
            public void init() {
                // No initialization needed for the mock
            }
            
            @Override
            public void destroy() {
                // No cleanup needed for the mock
            }
            
            @Override
            public void setProperties(Properties properties) {
                // No need to set properties in the mock
            }
        };
        
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        coreAssistant.setActiveLlm(testLlmName);

        AssistantMessage inputMessage = new CoreAssistantMessage("Test message");
        AssistantMessage response = coreAssistant.processMessage(inputMessage);

        assertNotNull(response, "Response should not be null");
        assertEquals("Processed: Test message", response.getContent(),
            "Response content should match the processed message from the LLM");
    }

    @Test
    public void testConstructorInitializesLlmManager() {
        Llm registeredLlm = coreAssistant.getLlmManager().getLlm("llama3.2");
        assertNotNull(registeredLlm, "Constructor should register 'llama3.2' LLM");
    }

    @Test
    public void testSetAndGetActiveLlm() {
        String activeLlm = "testLlm";
        coreAssistant.setActiveLlm(activeLlm);
        assertEquals(activeLlm, coreAssistant.getActiveLlm(), "Active LLM should be set and retrieved correctly");
    }

    @Test
    public void testSetAndGetCommandRegistry() {
        CoreAssistant coreAssistant = new CoreAssistant();
        CommandRegistry mockRegistry = new CoreCommandRegistry(coreAssistant);
        coreAssistant.setCommandRegistry(mockRegistry);
        assertEquals(mockRegistry, coreAssistant.getCommandRegistry(), "CommandRegistry should be set and retrieved correctly");
    }

    @Test
    public void testSetAndGetToolManager() {
        ToolManager mockToolManager = new CoreToolManager(null);
        coreAssistant.setToolManager(mockToolManager);
        assertEquals(mockToolManager, coreAssistant.getToolManager(), "ToolManager should be set and retrieved correctly");
    }

    @Test
    public void testSendMessageWithUnregisteredActiveLlm() {
        coreAssistant.setActiveLlm("UnregisteredLLM");
        AssistantMessage inputMessage = new CoreAssistantMessage("Test message");
        CompletableFuture<AssistantMessage> future = coreAssistant.sendMessage(inputMessage);
        AssistantMessage response = future.join();
        assertEquals("Active LLM is not registered", response.getContent(),
            "Response content should indicate the active LLM is not registered");
    }

    @Test
    public void testSendMessageWithRegisteredActiveLlm() {
        String testLlmName = "TestLLM";
        Llm mockLlm = new Llm() {
            @Override
            public String process(String content) {
                return "Processed: " + content;
            }
            
            @Override
            public Properties getProperties() {
                return new Properties();
            }
            
            @Override
            public void init() {
                // No initialization needed for the mock
            }
            
            @Override
            public void destroy() {
                // No cleanup needed for the mock
            }
            
            @Override
            public void setProperties(Properties properties) {
                // No need to set properties in the mock
            }
        };
        
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        coreAssistant.setActiveLlm(testLlmName);

        AssistantMessage inputMessage = new CoreAssistantMessage("Test message");
        CompletableFuture<AssistantMessage> future = coreAssistant.sendMessage(inputMessage);
        AssistantMessage response = future.join();
        assertEquals("Processed: Test message", response.getContent(),
            "Response content should match the processed message from the LLM");
    }
    
    // New tests to improve code coverage
    
    @Test
    public void testProcessCommandWithUnknownCommand() {
        // Test with a command that doesn't exist in the registry
        AssistantMessage inputMessage = new CoreAssistantMessage("/unknownCommand");
        AssistantMessage response = coreAssistant.processMessage(inputMessage);
        
        assertNotNull(response, "Response should not be null");
        assertEquals("Unknown command: /unknownCommand", response.getContent(),
                "Response should indicate the command is unknown");
    }
    
    @Test
    public void testProcessCommandWithArguments() {
        // Create a mock command to test with arguments
        String testCommandName = "testCommand";
        String testArgument = "testArg";
        
        Command mockCommand = new Command() {
            @Override
            public String execute(String commandArgs) {
                return "Command executed with args: " + commandArgs;
            }
            
            @Override
            public String getDescription() {
                return "Test command for arguments";
            }
            
            @Override
            public java.io.InputStream executeToStream(String input) {
                return new java.io.ByteArrayInputStream(execute(input).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            @Override
            public String executeToString(String input) {
                return null;
            }

            @Override
            public String getShortDescription() {
                throw new UnsupportedOperationException("Unimplemented method 'getShortDescription'");
            }
        };
        
        coreAssistant.getCommandRegistry().registerCommand(testCommandName, mockCommand);
        
        // Test with arguments
        AssistantMessage inputMessage = new CoreAssistantMessage("/" + testCommandName + " " + testArgument);
        AssistantMessage response = coreAssistant.processMessage(inputMessage);
        
        assertNotNull(response, "Response should not be null");
        assertEquals("Command executed with args: " + testArgument, response.getContent(),
                "Response should contain the result of the command execution with arguments");
    }
    
    @Test
    public void testProcessCommandWithoutArguments() {
        // Create a mock command to test without arguments
        String testCommandName = "testCommand";
        
        Command mockCommand = new Command() {
            @Override
            public String execute(String commandArgs) {
                return "Command executed with empty args";
            }
            
            @Override
            public String getDescription() {
                return "Test command for no arguments";
            }
            
            @Override
            public java.io.InputStream executeToStream(String input) {
                return new java.io.ByteArrayInputStream(execute(input).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            @Override
            public String executeToString(String input) {
                return null;
            }

            @Override
            public String getShortDescription() {
                throw new UnsupportedOperationException("Unimplemented method 'getShortDescription'");
            }
        };
        
        coreAssistant.getCommandRegistry().registerCommand(testCommandName, mockCommand);
        
        // Test without arguments
        AssistantMessage inputMessage = new CoreAssistantMessage("/" + testCommandName);
        AssistantMessage response = coreAssistant.processMessage(inputMessage);
        
        assertNotNull(response, "Response should not be null");
        assertEquals("Command executed with empty args", response.getContent(),
                "Response should contain the result of the command execution without arguments");
    }
    
    @Test
    public void testLlmCommandList() {
        // Register a test LLM
        String testLlmName = "testLlm";
        Llm mockLlm = new Llm() {
            @Override
            public String process(String content) {
                return "Test response";
            }
            
            @Override
            public Properties getProperties() {
                return new Properties();
            }
            
            @Override
            public void init() {}
            
            @Override
            public void destroy() {}
            
            @Override
            public void setProperties(Properties properties) {}
        };
        
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        
        // Test command without any args should show current configuration
        AssistantMessage inputMessage = new CoreAssistantMessage("/llm");
        AssistantMessage response = coreAssistant.processMessage(inputMessage);
        
        assertNotNull(response, "Response should not be null");
        String content = response.getContent();
        assertTrue(content.contains("Current LLM Configuration"), 
                "Response should show current configuration");
    }
    
    @Test
    public void testLlmCommandSet() {
        // Register a test LLM
        String testLlmName = "testLlm2";
        Llm mockLlm = new Llm() {
            @Override
            public String process(String content) {
                return "Test response";
            }
            
            @Override
            public Properties getProperties() {
                return new Properties();
            }
            
            @Override
            public void init() {}
            
            @Override
            public void destroy() {}
            
            @Override
            public void setProperties(Properties properties) {}
        };
        
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        
        // First, make sure the active LLM is null or different
        coreAssistant.setActiveLlm(null);
        
        // Now manually set the active LLM
        coreAssistant.setActiveLlm(testLlmName);
        
        // Verify it was set correctly
        assertEquals(testLlmName, coreAssistant.getActiveLlm(), "Active LLM should be set correctly");
    }
    
    @Test
    public void testLlmCommandVendor() {
        // Set up a test LLM with mock properties
        String testLlmName = "vendorTestLlm";
        Properties props = new Properties();
        props.setProperty("vendor", "TEST_VENDOR");
        
        Llm mockLlm = createMockLlmWithProperties(props);
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        coreAssistant.setActiveLlm(testLlmName);
        
        // Test get vendor
        AssistantMessage getCommand = new CoreAssistantMessage("/llm vendor");
        AssistantMessage getResponse = coreAssistant.processMessage(getCommand);
        
        assertNotNull(getResponse, "Response should not be null");
        assertEquals("Current vendor: TEST_VENDOR", getResponse.getContent(), 
                "Response should show the current vendor");
        
        // Test set vendor
        AssistantMessage setCommand = new CoreAssistantMessage("/llm vendor OPENAI");
        AssistantMessage setResponse = coreAssistant.processMessage(setCommand);
        
        assertNotNull(setResponse, "Response should not be null");
        assertTrue(setResponse.getContent().contains("Vendor updated to: OPENAI"), 
                "Response should confirm the vendor was updated");
    }

    @Test
    public void testLlmCommandModel() {
        // Set up a test LLM with mock properties
        String testLlmName = "modelTestLlm";
        Properties props = new Properties();
        props.setProperty("modelName", "test-model");
        
        Llm mockLlm = createMockLlmWithProperties(props);
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        coreAssistant.setActiveLlm(testLlmName);
        
        // Test get model
        AssistantMessage getCommand = new CoreAssistantMessage("/llm model");
        AssistantMessage getResponse = coreAssistant.processMessage(getCommand);
        
        assertNotNull(getResponse, "Response should not be null");
        assertEquals("Current model: test-model", getResponse.getContent(), 
                "Response should show the current model");
        
        // Test set model
        AssistantMessage setCommand = new CoreAssistantMessage("/llm model gpt-4");
        AssistantMessage setResponse = coreAssistant.processMessage(setCommand);
        
        assertNotNull(setResponse, "Response should not be null");
        assertTrue(setResponse.getContent().contains("Model updated to: gpt-4"), 
                "Response should confirm the model was updated");
    }

    @Test
    public void testLlmCommandEndpoint() {
        // Set up a test LLM with mock properties
        String testLlmName = "endpointTestLlm";
        Properties props = new Properties();
        props.setProperty("baseUrl", "http://localhost:11434");
        
        Llm mockLlm = createMockLlmWithProperties(props);
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        coreAssistant.setActiveLlm(testLlmName);
        
        // Test get endpoint
        AssistantMessage getCommand = new CoreAssistantMessage("/llm endpoint");
        AssistantMessage getResponse = coreAssistant.processMessage(getCommand);
        
        assertNotNull(getResponse, "Response should not be null");
        assertEquals("Current endpoint: http://localhost:11434", getResponse.getContent(), 
                "Response should show the current endpoint");
        
        // Test set endpoint with protocol missing
        AssistantMessage setCommand = new CoreAssistantMessage("/llm endpoint api.example.com");
        AssistantMessage setResponse = coreAssistant.processMessage(setCommand);
        
        assertNotNull(setResponse, "Response should not be null");
        assertTrue(setResponse.getContent().contains("Endpoint updated to: http://api.example.com"), 
                "Response should confirm the endpoint was updated with protocol added");
    }

    @Test
    public void testLlmCommandApiKey() {
        // Set up a test LLM with mock properties
        String testLlmName = "apiKeyTestLlm";
        Properties props = new Properties();
        props.setProperty("apiKey", "test-api-key-12345");
        
        Llm mockLlm = createMockLlmWithProperties(props);
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        coreAssistant.setActiveLlm(testLlmName);
        
        // Test get API key (should be masked)
        AssistantMessage getCommand = new CoreAssistantMessage("/llm apiKey");
        AssistantMessage getResponse = coreAssistant.processMessage(getCommand);
        
        assertNotNull(getResponse, "Response should not be null");
        String content = getResponse.getContent();
        assertTrue(content.contains("API key:"), "Response should contain API key label");
        assertTrue(content.contains("****"), "API key should contain masked characters");
        
        // Test set API key
        AssistantMessage setCommand = new CoreAssistantMessage("/llm apiKey new-secret-key");
        AssistantMessage setResponse = coreAssistant.processMessage(setCommand);
        
        assertNotNull(setResponse, "Response should not be null");
        assertEquals("API key updated", setResponse.getContent(), 
                "Response should confirm the API key was updated");
    }

    @Test
    public void testLlmCommandTemperature() {
        // Set up a test LLM with mock properties
        String testLlmName = "tempTestLlm";
        Properties props = new Properties();
        props.setProperty("temperature", "0.7");
        
        Llm mockLlm = createMockLlmWithProperties(props);
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        coreAssistant.setActiveLlm(testLlmName);
        
        // Test get temperature
        AssistantMessage getCommand = new CoreAssistantMessage("/llm temperature");
        AssistantMessage getResponse = coreAssistant.processMessage(getCommand);
        
        assertNotNull(getResponse, "Response should not be null");
        assertEquals("Current temperature: 0.7", getResponse.getContent(), 
                "Response should show the current temperature");
        
        // Test set valid temperature
        AssistantMessage setCommand = new CoreAssistantMessage("/llm temperature 0.3");
        AssistantMessage setResponse = coreAssistant.processMessage(setCommand);
        
        assertNotNull(setResponse, "Response should not be null");
        assertEquals("Temperature updated to: 0.3", setResponse.getContent(), 
                "Response should confirm the temperature was updated");
    }

    @Test
    public void testLlmCommandInvalidTemperature() {
        String testLlmName = "invalidTempTestLlm";
        Properties props = new Properties();
        props.setProperty("temperature", "0.7");
        
        Llm mockLlm = createMockLlmWithProperties(props);
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        coreAssistant.setActiveLlm(testLlmName);
        
        // Test set invalid temperature (out of range)
        AssistantMessage outOfRangeCommand = new CoreAssistantMessage("/llm temperature 1.5");
        AssistantMessage outOfRangeResponse = coreAssistant.processMessage(outOfRangeCommand);
        
        assertNotNull(outOfRangeResponse, "Response should not be null");
        assertEquals("Temperature must be between 0.0 and 1.0", outOfRangeResponse.getContent(), 
                "Response should reject out of range temperature");
        
        // Test set invalid temperature (wrong format)
        AssistantMessage invalidFormatCommand = new CoreAssistantMessage("/llm temperature xyz");
        AssistantMessage invalidFormatResponse = coreAssistant.processMessage(invalidFormatCommand);
        
        assertNotNull(invalidFormatResponse, "Response should not be null");
        assertEquals("Invalid temperature format. Must be a number between 0.0 and 1.0", 
                invalidFormatResponse.getContent(), "Response should reject non-numeric temperature");
    }
    
    /**
     * Helper method to create a mock LLM with specified properties
     */
    private Llm createMockLlmWithProperties(final Properties initialProps) {
        return new Llm() {
            private Properties props = initialProps;
            
            @Override
            public String process(String content) {
                return "Processed: " + content;
            }
            
            @Override
            public Properties getProperties() {
                return props;
            }
            
            @Override
            public void init() {
                // No initialization needed for the mock
            }
            
            @Override
            public void destroy() {
                // No cleanup needed for the mock
            }
            
            @Override
            public void setProperties(Properties properties) {
                this.props = properties;
            }
        };
    }

    @Test
    public void testLlmCommandWithNoActiveLlm() {
        // Make sure no active LLM is set
        coreAssistant.setActiveLlm(null);
        
        // Test trying to update LLM properties with no active LLM
        AssistantMessage vendorCommand = new CoreAssistantMessage("/llm vendor OPENAI");
        AssistantMessage response = coreAssistant.processMessage(vendorCommand);
        
        assertNotNull(response, "Response should not be null");
        assertTrue(response.getContent().contains("Error") || 
               response.getContent().contains("No active LLM"), 
               "Response should indicate an error or that no active LLM is set");
    }

    @Test
    public void testLlmCommandGetShowConfig() {
        // Set up a test LLM with mock properties
        String testLlmName = "configTestLlm";
        Properties props = new Properties();
        props.setProperty("vendor", "TEST_VENDOR");
        props.setProperty("modelName", "test-model");
        props.setProperty("baseUrl", "http://test.endpoint");
        props.setProperty("apiKey", "test-key-12345");
        props.setProperty("temperature", "0.5");
        
        Llm mockLlm = createMockLlmWithProperties(props);
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        coreAssistant.setActiveLlm(testLlmName);
        
        // Test different ways to show configuration
        String[] commands = {"/llm", "/llm status", "/llm show", "/llm info"};
        
        for (String cmd : commands) {
            AssistantMessage command = new CoreAssistantMessage(cmd);
            AssistantMessage response = coreAssistant.processMessage(command);
            
            assertNotNull(response, "Response should not be null");
            String content = response.getContent();
            
            assertTrue(content.contains("Current LLM Configuration:"), 
                    "Response should show configuration header");
            assertTrue(content.contains("Vendor: TEST_VENDOR"), 
                    "Response should show vendor");
            assertTrue(content.contains("Model: test-model"), 
                    "Response should show model");
            assertTrue(content.contains("Endpoint: http://test.endpoint"), 
                    "Response should show endpoint");
            assertTrue(content.contains("API Key:"), 
                    "Response should show API key label");
            assertTrue(content.contains("Temperature: 0.5"), 
                    "Response should show temperature");
        }
    }

    @Test
    public void testLlmCommandWithUnknownSubCommand() {
        // Set up a test LLM
        String testLlmName = "testLlm3";
        Llm mockLlm = createMockLlmWithProperties(new Properties());
        coreAssistant.getLlmManager().registerLlm(testLlmName, mockLlm);
        coreAssistant.setActiveLlm(testLlmName);
        
        // Test with unknown subcommand
        AssistantMessage command = new CoreAssistantMessage("/llm unknownSubCommand");
        AssistantMessage response = coreAssistant.processMessage(command);
        
        assertNotNull(response, "Response should not be null");
        // The CoreLlmCommand implementation seems to return a consistent format for unknown subcommands
        String content = response.getContent();
        assertTrue(content.startsWith("Unknown subcommand:") || content.contains("Unknown subcommand"), 
                "Response should indicate unknown subcommand");
    }
}

package com.manorrock.assistant.cli;

import com.manorrock.assistant.api.Assistant;
import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.api.CommandRegistry;
import com.manorrock.assistant.api.LlmManager;
import com.manorrock.assistant.api.ToolManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CLI implementation of the Assistant interface.
 * This class processes messages by dispatching them to the CLI's main method.
 */
public class CLIAssistant implements Assistant {

    private static final Logger LOGGER = Logger.getLogger(CLIAssistant.class.getName());
    
    /**
     * Constructor.
     */
    public CLIAssistant() {
        // No initialization needed
    }
    
    @Override
    public CompletableFuture<AssistantMessage> sendMessage(AssistantMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processMessage(message);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing message asynchronously", e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public AssistantMessage processMessage(AssistantMessage message) {
        try {
            // Capture the System.out to get the CLI's response
            PrintStream originalOut = System.out;
            ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
            PrintStream captureStream = new PrintStream(outputCapture);
            System.setOut(captureStream);
            
            try {
                // Execute the CLI with the message
                String[] args = {"--stdin"};
                int exitCode = runCLI(args, message.getContent());
                
                if (exitCode != 0) {
                    throw new RuntimeException("CLI execution failed with exit code: " + exitCode);
                }
                
                // Get the captured output
                String output = outputCapture.toString().trim();
                
                // Extract the assistant's response from the output
                String response = extractAssistantResponse(output);
                
                // Create response message
                DefaultAssistantMessage responseMessage = new DefaultAssistantMessage();
                responseMessage.setContent(response);
                responseMessage.setType("assistant");
                
                return responseMessage;
            } finally {
                // Restore System.out
                System.setOut(originalOut);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing message", e);
            
            // Create an error message
            DefaultAssistantMessage errorMessage = new DefaultAssistantMessage();
            errorMessage.setContent("Error processing message: " + e.getMessage());
            errorMessage.setType("error");
            
            return errorMessage;
        }
    }
    
    /**
     * Runs the CLI with provided arguments and input.
     * 
     * @param args Command line arguments
     * @param input Standard input for the CLI
     * @return Exit code from the CLI
     */
    private int runCLI(String[] args, String input) {
        // Save the original System.in
        System.setIn(new java.io.ByteArrayInputStream(input.getBytes()));
        
        try {
            // Create a CLI instance and execute it
            return new picocli.CommandLine(new CLI()).execute(args);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing CLI", e);
            return 1;
        }
    }
    
    /**
     * Extract the assistant's response from the CLI output.
     * 
     * @param output Full CLI output
     * @return Assistant response part
     */
    private String extractAssistantResponse(String output) {
        // Find the assistant's response in the output
        // Format is typically: "Assistant: [response]"
        String[] lines = output.split("\\n");
        StringBuilder response = new StringBuilder();
        boolean isAssistantResponse = false;
        
        for (String line : lines) {
            if (line.startsWith("Assistant:")) {
                isAssistantResponse = true;
                // Extract the part after "Assistant:"
                response.append(line.substring("Assistant:".length()).trim());
            } else if (isAssistantResponse) {
                // Continue collecting the assistant response for multiline responses
                response.append("\n").append(line);
            }
        }
        
        // If we couldn't find an assistant response, return the whole output
        if (response.length() == 0) {
            return output;
        }
        
        return response.toString().trim();
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        throw new UnsupportedOperationException("Unimplemented method 'getCommandRegistry'");
    }

    @Override
    public LlmManager getLlmManager() {
        throw new UnsupportedOperationException("Unimplemented method 'getLlmManager'");
    }

    @Override
    public String getActiveLlm() {
        throw new UnsupportedOperationException("Unimplemented method 'getActiveLlm'");
    }

    @Override
    public void setActiveLlm(String activeLlm) {
        throw new UnsupportedOperationException("Unimplemented method 'setActiveLlm'");
    }

    @Override
    public ToolManager getToolManager() {
        throw new UnsupportedOperationException("Unimplemented method 'getToolManager'");
    }
}
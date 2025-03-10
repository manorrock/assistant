package com.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Clipboard;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.time.Duration;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.ChatResponse;
import com.example.shared.LlmConfiguration;

/**
 * Controller class for the JavaFX-based LLM chat interface.
 * Handles user interactions, command processing, and LLM communication.
 */
public class FXMLController {

    @FXML
    private TextArea responseArea;
    
    @FXML
    private TextArea requestArea;
    
    @FXML
    private Button sendButton;

    @FXML
    private Button startOverButton;

    @FXML
    private ProgressBar progressBar;

    private LinkedList<ChatMessage> history = new LinkedList<>();
    private LlmConfiguration config = LlmConfiguration.defaultConfig();
    
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    /**
     * Initializes the controller with default settings and UI event handlers.
     * - Sets welcome message in response area
     * - Initializes progress bar to 0
     * - Shows help message
     * - Sets up Enter key handler for request area (Enter sends, Shift+Enter adds newline)
     */
    @FXML
    public void initialize() {
        responseArea.setText("Welcome to Manorrock Assistant");
        progressBar.setProgress(0);
        showHelp();

        requestArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                handleSendAction();
                event.consume();
            }
        });
    }
    
    /**
     * Handles the Send button action and Enter key press.
     * - If message starts with '/', processes as command
     * - Otherwise sends message to LLM for processing
     * - Clears request area after processing
     */
    @FXML
    private void handleSendAction() {
        String userMessage = requestArea.getText().trim();
        
        if (!userMessage.isEmpty()) {
            // Check if the message is a command
            if (userMessage.startsWith("/")) {
                handleCommand(userMessage);
                return;
            }
            
            // Display the user's message in the response area
            responseArea.appendText("\n\nYou: " + userMessage);
            
            // Clear the request area
            requestArea.clear();
            
            // Process the message and display a response
            processMessage(userMessage);
        }
    }

    /**
     * Routes commands to their appropriate handlers.
     * Supported commands:
     * - /llmEndpoint: Change API endpoint
     * - /llmModel: Change LLM model
     * - /llmVendor: Change LLM vendor
     * - /llmApiKey: Set API key
     * - /llmTemperature: Set temperature
     * - /help: Show help
     * - /clear: Clear response area
     * - /explain: Explain clipboard content
     *
     * @param command The command string to process
     */
    private void handleCommand(String command) {
        if (command.startsWith("/llmEndpoint ")) {
            changeEndpoint(command);
        } else if (command.startsWith("/llmModel ")) {
            changeModel(command);
        } else if (command.startsWith("/llmVendor")) {
            changeVendor(command);
        } else if (command.startsWith("/llmApiKey ")) {
            changeApiKey(command);
        } else if (command.startsWith("/llmTemperature ")) {
            changeTemperature(command);
        } else if (command.equals("/help")) {
            showHelp();
        } else if (command.equals("/clear")) {
            clearResponseArea();
        } else if (command.equals("/explain")) {
            explainSelection();
        } else {
            responseArea.appendText("\n\nSystem: Unknown command. Type /help for a list of commands.");
        }
        requestArea.clear();
    }

    /**
     * Changes the LLM endpoint.
     * Format: /llmEndpoint hostname:port
     * Constructs full URL with http:// prefix and /api/chat suffix.
     *
     * @param command The endpoint change command
     */
    private void changeEndpoint(String command) {
        Pattern pattern = Pattern.compile("/llmEndpoint\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String newEndpoint = "http://" + matcher.group(1) + "/api/chat";
            config = new LlmConfiguration(newEndpoint, config.model(), config.vendor(), 
                                       config.apiKey(), config.temperature());
            responseArea.appendText("\n\nSystem: Endpoint changed to " + newEndpoint);
        } else {
            responseArea.appendText("\n\nSystem: Invalid endpoint format. Use /llmEndpoint myhostname:myport");
        }
    }

    /**
     * Changes the LLM model.
     * Format: /llmModel modelname
     *
     * @param command The model change command
     */
    private void changeModel(String command) {
        Pattern pattern = Pattern.compile("/llmModel\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String newModel = matcher.group(1);
            config = new LlmConfiguration(config.endpoint(), newModel, config.vendor(), 
                                       config.apiKey(), config.temperature());
            responseArea.appendText("\n\nSystem: Model changed to " + newModel);
        } else {
            responseArea.appendText("\n\nSystem: Invalid model format. Use /model <name>");
        }
    }

    /**
     * Changes the LLM vendor.
     * Format: /llmVendor vendorname
     * Converts vendor name to uppercase.
     *
     * @param command The vendor change command
     */
    private void changeVendor(String command) {
        Pattern pattern = Pattern.compile("/llmVendor\\s*(\\S*)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find() && !matcher.group(1).isEmpty()) {
            String newVendor = matcher.group(1).toUpperCase();
            if (newVendor.equals("OLLAMA") || newVendor.equals("OPENAI") || newVendor.equals("AZURE_OPENAI")) {
                config = new LlmConfiguration(config.endpoint(), config.model(), newVendor, 
                                           config.apiKey(), config.temperature());
                responseArea.appendText("\n\nSystem: Vendor changed to " + newVendor);
            } else {
                responseArea.appendText("\n\nSystem: Invalid vendor. Supported vendors: OLLAMA, OPENAI, AZURE_OPENAI");
            }
        } else {
            responseArea.appendText("\n\nSystem: Please specify a vendor. Supported vendors: OLLAMA, OPENAI, AZURE_OPENAI");
        }
    }

    /**
     * Sets the API key for OpenAI or Azure OpenAI.
     * Format: /llmApiKey keyvalue
     *
     * @param command The API key change command
     */
    private void changeApiKey(String command) {
        Pattern pattern = Pattern.compile("/llmApiKey\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String newKey = matcher.group(1);
            config = new LlmConfiguration(config.endpoint(), config.model(), config.vendor(), 
                                       newKey, config.temperature());
            responseArea.appendText("\n\nSystem: API key updated");
        }
    }

    /**
     * Sets the temperature parameter for the LLM.
     * Format: /llmTemperature value
     * Value must be between 0.0 and 1.0.
     *
     * @param command The temperature change command
     */
    private void changeTemperature(String command) {
        Pattern pattern = Pattern.compile("/llmTemperature\\s+(\\d*\\.?\\d+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            try {
                double newTemp = Double.parseDouble(matcher.group(1));
                if (newTemp >= 0.0 && newTemp <= 1.0) {
                    config = new LlmConfiguration(config.endpoint(), config.model(), 
                                               config.vendor(), config.apiKey(), newTemp);
                    responseArea.appendText("\n\nSystem: Temperature changed to " + newTemp);
                } else {
                    responseArea.appendText("\n\nSystem: Temperature must be between 0.0 and 1.0");
                }
            } catch (NumberFormatException e) {
                responseArea.appendText("\n\nSystem: Invalid temperature format. Use /llmTemperature <number>");
            }
        }
    }

    /**
     * Shows help message listing all available commands
     * and their descriptions.
     */
    private void showHelp() {
        String helpMessage = "\n\nSystem: Available commands:\n" +
                           "/clear - Clear the response window\n" +
                           "/explain - Explain the selected text\n" +
                           "/help - Show this help message\n" +
                           "/llmApiKey <apikey> - Set API key for OpenAI or Azure\n" +
                           "/llmEndpoint myhostname:myport - Change the endpoint\n" +
                           "/llmModel <name> - Change the model used\n" +
                           "/llmTemperature <number> - Set temperature (0.0-1.0)\n" +
                           "/llmVendor <name> - Change vendor";
        responseArea.appendText(helpMessage);
    }

    /**
     * Clears all text from the response area.
     */
    private void clearResponseArea() {
        responseArea.clear();
    }

    /**
     * Handles Start Over button action.
     * - Clears conversation history
     * - Generates new session ID
     * - Clears response area
     * - Shows welcome message and help
     */
    @FXML
    private void handleStartOverAction() {
        history.clear();
        responseArea.clear();
        responseArea.setText("Welcome to Manorrock Assistant");
        showHelp();
    }
    
    /**
     * Creates a streaming chat language model based on current configuration.
     * Configures model-specific settings for:
     * - OLLAMA: Uses baseUrl, model name, timeout, temperature
     * - OPENAI: Uses API key, model name, timeout, temperature
     * - AZURE_OPENAI: Uses endpoint, API key, deployment name, timeout, temperature
     *
     * @return Configured StreamingChatLanguageModel instance
     * @throws IllegalArgumentException if vendor is unknown
     */
    private StreamingChatLanguageModel createLanguageModel() {
        String vendor = config.vendor();
        return switch (vendor.toUpperCase()) {
            case "OLLAMA" -> OllamaStreamingChatModel.builder()
                .baseUrl(config.endpoint().substring(0, config.endpoint().lastIndexOf("/api/chat")))
                .modelName(config.model())
                .timeout(TIMEOUT)
                .temperature(config.temperature())
                .build();
            case "OPENAI" -> OpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model())
                .timeout(TIMEOUT)
                .temperature(config.temperature())
                .build();
            case "AZURE_OPENAI" -> AzureOpenAiStreamingChatModel.builder()
                .endpoint(config.endpoint())
                .apiKey(config.apiKey())
                .deploymentName(config.model())
                .timeout(TIMEOUT)
                .temperature(config.temperature())
                .build();
            default -> throw new IllegalArgumentException("Unknown vendor: " + vendor);
        };
    }

    /**
     * Processes a message through the LLM.
     * - Adds message to history
     * - Disables send button during processing
     * - Shows progress indicator
     * - Streams response tokens to UI
     * - Handles errors and timeouts
     * - Re-enables UI after completion
     *
     * @param message The message to process
     */
    private void processMessage(String message) {
        try {
            UserMessage userMessage = UserMessage.from(message);
            history.add(userMessage);
            if (history.size() > 50) {
                history.removeFirst();
            }

            sendButton.setDisable(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

            StreamingChatLanguageModel langChainModel = createLanguageModel();

            StringBuilder responseBuilder = new StringBuilder();
            final boolean[] isFirstLine = {true};

            ArrayList<ChatMessage> messages = new ArrayList<>(history);
            
            langChainModel.chat(messages, new StreamingChatResponseHandler() {
                /**
                 * Handles each token of the streaming response.
                 * Updates UI with new content and maintains formatting.
                 */
                @Override
                public void onPartialResponse(String token) {
                    responseBuilder.append(token);
                    Platform.runLater(() -> {
                        if (isFirstLine[0]) {
                            responseArea.appendText("\n\nAssistant: " + token);
                            isFirstLine[0] = false;
                        } else {
                            responseArea.appendText(token);
                        }
                        responseArea.positionCaret(responseArea.getText().length());
                    });
                }

                /**
                 * Handles completion of the response.
                 * Re-enables UI controls and updates conversation history.
                 */
                @Override
                public void onCompleteResponse(ChatResponse response) {
                    String fullResponse = responseBuilder.toString().trim();
                    Platform.runLater(() -> {
                        sendButton.setDisable(false);
                        progressBar.setProgress(0);

                        // Add the assistant's response to the history
                        history.add(AiMessage.from(fullResponse));
                        if (history.size() > 50) {
                            history.removeFirst();
                        }
                    });
                }

                @Override
                public void onError(Throwable error) {
                    Platform.runLater(() -> {
                        String errorMessage = "Error: " + error.getMessage();
                        responseArea.appendText("\n\nAssistant: " + errorMessage);
                        responseArea.positionCaret(responseArea.getText().length());
                        sendButton.setDisable(false);
                        progressBar.setProgress(0);
                    });
                }
            });
        } catch (Exception e) {
            String errorMessage;
            if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                errorMessage = "Request timed out after " + TIMEOUT.getSeconds() + " seconds";
            } else {
                errorMessage = "Error: " + e.getMessage();
            }
            responseArea.appendText("\n\nAssistant: " + errorMessage);
            responseArea.positionCaret(responseArea.getText().length());
            sendButton.setDisable(false);
            progressBar.setProgress(0);
        }
    }

    /**
     * Explains text from clipboard.
     * - Gets content from system clipboard
     * - If content exists, creates explanation prompt
     * - Sends prompt to LLM for processing
     * - Shows error if clipboard is empty
     */
    private void explainSelection() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String clipboardContent = clipboard.getString();
        
        if (clipboardContent != null && !clipboardContent.isEmpty()) {
            String prompt = "Please explain the content below the line\n-----------------------------------------\n" + clipboardContent;
            responseArea.appendText("\n\nYou: " + prompt);
            processMessage(prompt);
        } else {
            responseArea.appendText("\n\nSystem: No text found in clipboard. Copy some text and try again.");
        }
    }
}
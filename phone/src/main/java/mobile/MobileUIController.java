package mobile;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
// ...existing code...

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class MobileUIController {

    @FXML
    private TextArea responseTextArea;

    @FXML
    private TextArea requestTextArea;

    @FXML
    private Button sendRequestButton;

    @FXML
    private ProgressBar progressBar;

    private String sessionId = UUID.randomUUID().toString();
    private LinkedList<Map<String, String>> history = new LinkedList<>();
    private String ollamaEndpoint = "http://localhost:11434/api/chat";
    private String model = "llama3.1";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    @FXML
    public void initialize() {
        // Set initial message
        responseTextArea.setText("Welcome to Manorrock Assistant");
        
        // Stop the progress bar initially
        progressBar.setProgress(0);

        // Show help message on startup
        showHelp();

        // Setup key event handler for the requestTextArea
        requestTextArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                handleSendAction();
                event.consume(); // Prevents the newline from being added
            }
        });

        sendRequestButton.setOnAction(event -> handleSendAction());
    }

    private void handleSendAction() {
        String userMessage = requestTextArea.getText().trim();
        
        if (!userMessage.isEmpty()) {
            // Check if the message is a command
            if (userMessage.startsWith("/")) {
                handleCommand(userMessage);
                return;
            }

            // Get current timestamp
            LocalDateTime.now().format(formatter); // timestamp not used
            
            // Display the user's message in the response area
            responseTextArea.appendText("\n\nYou: " + userMessage);
            
            // Clear the request area
            requestTextArea.clear();
            
            // Process the message and display a response
            processMessage(userMessage);
        }
    }

    private void handleCommand(String command) {
        if (command.startsWith("/llmEndpoint ")) {
            changeEndpoint(command);
        } else if (command.startsWith("/model ")) {
            changeModel(command);
        } else if (command.equals("/help")) {
            showHelp();
        } else if (command.equals("/clear")) {
            clearResponseArea();
        } else {
            responseTextArea.appendText("\n\nSystem: Unknown command. Type /help for a list of commands.");
        }
        requestTextArea.clear();
    }

    private void changeEndpoint(String command) {
        Pattern pattern = Pattern.compile("/llmEndpoint\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String newEndpoint = matcher.group(1);
            ollamaEndpoint = "http://" + newEndpoint + "/api/chat";
            responseTextArea.appendText("\n\nSystem: Endpoint changed to " + ollamaEndpoint);
        } else {
            responseTextArea.appendText("\n\nSystem: Invalid endpoint format. Use /llmEndpoint myhostname:myport");
        }
    }

    private void changeModel(String command) {
        Pattern pattern = Pattern.compile("/model\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            model = matcher.group(1);
            responseTextArea.appendText("\n\nSystem: Model changed to " + model);
        } else {
            responseTextArea.appendText("\n\nSystem: Invalid model format. Use /model <n>");
        }
    }

    private void showHelp() {
        String helpMessage = "\n\nSystem: Available commands:\n" +
                             "/llmEndpoint myhostname:myport - Change the Ollama endpoint\n" +
                             "/model <n> - Change the model used\n" +
                             "/help - Show this help message\n" +
                             "/clear - Clear the response window";
        responseTextArea.appendText(helpMessage);
    }

    private void clearResponseArea() {
        responseTextArea.clear();
    }

    private void processMessage(String message) {
    LocalDateTime.now().format(formatter); // timestamp not used

        try {
            Map<String, String> messageObject = new HashMap<>();
            messageObject.put("role", "user");
            messageObject.put("content", message);

            // Add the new message to the history
            history.add(messageObject);
            if (history.size() > 50) {
                history.removeFirst();
            }

            // Create the request payload
            ObjectNode jsonInput = MAPPER.createObjectNode();
            jsonInput.put("model", model);
            
            // Add messages array
            ArrayNode messagesArray = jsonInput.putArray("messages");
            for (Map<String, String> msg : history) {
                ObjectNode msgObj = messagesArray.addObject();
                msgObj.put("role", msg.get("role"));
                msgObj.put("content", msg.get("content"));
            }
            
            jsonInput.put("stream", true);
            jsonInput.put("session_id", sessionId);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(jsonInput)))
                .build();

            sendRequestButton.setDisable(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

            client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenApply(HttpResponse::body)
                .thenAccept(lines -> {
                    StringBuilder responseBuilder = new StringBuilder();
                    final boolean[] isFirstLine = {true};
                    lines.forEach(line -> {
                        try {
                            JsonNode jsonObject = MAPPER.readTree(line);
                            
                            if (jsonObject.has("session_id")) {
                                sessionId = jsonObject.get("session_id").asText();
                            }
                            
                            if (jsonObject.has("messages")) {
                                JsonNode messages = jsonObject.get("messages");
                                if (messages.isArray()) {
                                    for (JsonNode msg : messages) {
                                        if ("assistant".equals(msg.get("role").asText())) {
                                            String content = msg.get("content").asText();
                                            responseBuilder.append(content);
                                            Platform.runLater(() -> {
                                                if (isFirstLine[0]) {
                                                    responseTextArea.appendText("\n\nAssistant: " + content);
                                                    isFirstLine[0] = false;
                                                } else {
                                                    responseTextArea.appendText(content);
                                                }
                                                responseTextArea.positionCaret(responseTextArea.getText().length());
                                            });
                                        }
                                    }
                                }
                            } else if (jsonObject.has("message")) {
                                String content = jsonObject.get("message").get("content").asText();
                                responseBuilder.append(content);
                                Platform.runLater(() -> {
                                    if (isFirstLine[0]) {
                                        responseTextArea.appendText("\n\nAssistant: " + content);
                                        isFirstLine[0] = false;
                                    } else {
                                        responseTextArea.appendText(content);
                                    }
                                    responseTextArea.positionCaret(responseTextArea.getText().length());
                                });
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing JSON response: " + e.getMessage());
                        }
                    });

                    String response = responseBuilder.toString().trim();
                    Platform.runLater(() -> {
                        sendRequestButton.setDisable(false);
                        progressBar.setProgress(0);

                        // Add the assistant's response to the history
                        Map<String, String> responseObject = new HashMap<>();
                        responseObject.put("role", "assistant");
                        responseObject.put("content", response);
                        history.add(responseObject);
                        if (history.size() > 50) {
                            history.removeFirst();
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        String errorMessage = "Ollama is unavailable.";
                        responseTextArea.appendText("\n\nAssistant: " + errorMessage);
                        responseTextArea.positionCaret(responseTextArea.getText().length());
                        sendRequestButton.setDisable(false);
                        progressBar.setProgress(0);
                    });
                    return null;
                });
        } catch (Exception e) {
            String errorMessage = "Ollama is unavailable.";
            responseTextArea.appendText("\n\nAssistant: " + errorMessage);
            responseTextArea.positionCaret(responseTextArea.getText().length());
            sendRequestButton.setDisable(false);
            progressBar.setProgress(0);
        }
    }
}

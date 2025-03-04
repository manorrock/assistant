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

import org.json.JSONObject;
import org.json.JSONArray;

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
    private LinkedList<JSONObject> history = new LinkedList<>();
    private String ollamaEndpoint = "http://localhost:11434/api/chat";
    private String model = "llama3";
    
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
            String timestamp = LocalDateTime.now().format(formatter);
            
            // Display the user's message in the response area
            responseTextArea.appendText("\n\nYou: " + userMessage);
            
            // Clear the request area
            requestTextArea.clear();
            
            // Process the message and display a response
            processMessage(userMessage);
        }
    }

    private void handleCommand(String command) {
        if (command.startsWith("/endpoint ")) {
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
        Pattern pattern = Pattern.compile("/endpoint\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String newEndpoint = matcher.group(1);
            ollamaEndpoint = "http://" + newEndpoint + "/api/chat";
            responseTextArea.appendText("\n\nSystem: Endpoint changed to " + ollamaEndpoint);
        } else {
            responseTextArea.appendText("\n\nSystem: Invalid endpoint format. Use /endpoint myhostname:myport");
        }
    }

    private void changeModel(String command) {
        Pattern pattern = Pattern.compile("/model\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            model = matcher.group(1);
            responseTextArea.appendText("\n\nSystem: Model changed to " + model);
        } else {
            responseTextArea.appendText("\n\nSystem: Invalid model format. Use /model <name>");
        }
    }

    private void showHelp() {
        String helpMessage = "\n\nSystem: Available commands:\n" +
                             "/endpoint myhostname:myport - Change the Ollama endpoint\n" +
                             "/model <name> - Change the model used\n" +
                             "/help - Show this help message\n" +
                             "/clear - Clear the response window";
        responseTextArea.appendText(helpMessage);
    }

    private void clearResponseArea() {
        responseTextArea.clear();
    }

    private void processMessage(String message) {
        String timestamp = LocalDateTime.now().format(formatter);

        try {
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", message);

            // Add the new message to the history
            history.add(messageObject);
            if (history.size() > 50) {
                history.removeFirst();
            }

            JSONObject jsonInput = new JSONObject();
            jsonInput.put("model", model);
            jsonInput.put("messages", new JSONArray(history));
            jsonInput.put("stream", true);
            jsonInput.put("session_id", sessionId);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput.toString()))
                .build();

            sendRequestButton.setDisable(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

            client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenApply(HttpResponse::body)
                .thenAccept(lines -> {
                    StringBuilder responseBuilder = new StringBuilder();
                    final boolean[] isFirstLine = {true};
                    lines.forEach(line -> {
                        JSONObject jsonObject = new JSONObject(line);
                        if (jsonObject.has("session_id")) {
                            sessionId = jsonObject.getString("session_id");
                        }
                        if (jsonObject.has("messages")) {
                            JSONArray messages = jsonObject.getJSONArray("messages");
                            for (int i = 0; i < messages.length(); i++) {
                                JSONObject msg = messages.getJSONObject(i);
                                if ("assistant".equals(msg.getString("role"))) {
                                    String content = msg.getString("content");
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
                        } else {
                            String content = jsonObject.getJSONObject("message").getString("content");
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
                    });

                    String response = responseBuilder.toString().trim();
                    Platform.runLater(() -> {
                        sendRequestButton.setDisable(false);
                        progressBar.setProgress(0);

                        // Add the assistant's response to the history
                        JSONObject responseObject = new JSONObject();
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

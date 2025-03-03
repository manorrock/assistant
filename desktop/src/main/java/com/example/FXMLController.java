package com.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.json.JSONArray;

public class FXMLController {

    @FXML
    private TextArea responseArea;
    
    @FXML
    private TextArea requestArea;
    
    @FXML
    private TextArea logArea;

    @FXML
    private StackPane dropZone;

    @FXML
    private Button sendButton;

    @FXML
    private Button startOverButton;

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
        responseArea.setText("Welcome to Manorrock Assistant");
        logArea.setText("Chat Log\n---------");
        
        // Stop the progress bar initially
        progressBar.setProgress(0);

        // Show help message on startup
        showHelp();

        // Setup key event handler for the requestArea
        requestArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                handleSendAction();
                event.consume(); // Prevents the newline from being added
            }
        });

        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleDragDropped);
    }
    
    @FXML
    private void handleSendAction() {
        String userMessage = requestArea.getText().trim();
        
        if (!userMessage.isEmpty()) {
            // Check if the message is a command
            if (userMessage.startsWith("/")) {
                handleCommand(userMessage);
                return;
            }

            // Get current timestamp
            String timestamp = LocalDateTime.now().format(formatter);
            
            // Display the user's message in the response area
            responseArea.appendText("\n\nYou: " + userMessage);
            
            // Add timestamped message to the log area
            logArea.appendText("\n\n[" + timestamp + " - You]\n" + userMessage);
            
            // Clear the request area
            requestArea.clear();
            
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
            responseArea.appendText("\n\nSystem: Unknown command. Type /help for a list of commands.");
        }
        requestArea.clear();
    }

    private void changeEndpoint(String command) {
        Pattern pattern = Pattern.compile("/endpoint\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String newEndpoint = matcher.group(1);
            ollamaEndpoint = "http://" + newEndpoint + "/api/chat";
            responseArea.appendText("\n\nSystem: Endpoint changed to " + ollamaEndpoint);
            logArea.appendText("\n\n[" + LocalDateTime.now().format(formatter) + " - System]\nEndpoint changed to " + ollamaEndpoint);
        } else {
            responseArea.appendText("\n\nSystem: Invalid endpoint format. Use /endpoint myhostname:myport");
        }
    }

    private void changeModel(String command) {
        Pattern pattern = Pattern.compile("/model\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            model = matcher.group(1);
            responseArea.appendText("\n\nSystem: Model changed to " + model);
            logArea.appendText("\n\n[" + LocalDateTime.now().format(formatter) + " - System]\nModel changed to " + model);
        } else {
            responseArea.appendText("\n\nSystem: Invalid model format. Use /model <name>");
        }
    }

    private void showHelp() {
        String helpMessage = "\n\nSystem: Available commands:\n" +
                             "/endpoint myhostname:myport - Change the Ollama endpoint\n" +
                             "/model <name> - Change the model used\n" +
                             "/help - Show this help message\n" +
                             "/clear - Clear the response window";
        responseArea.appendText(helpMessage);
    }

    private void clearResponseArea() {
        responseArea.clear();
    }

    @FXML
    private void handleStartOverAction() {
        // Clear the history and reset the session ID
        history.clear();
        sessionId = UUID.randomUUID().toString();
        
        // Clear the response and log areas
        responseArea.clear();
        logArea.clear();
        
        // Set initial messages
        responseArea.setText("Welcome to Manorrock Assistant");
        logArea.setText("Chat Log\n---------");
        
        // Show help message
        showHelp();
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

            sendButton.setDisable(true);
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
                                            responseArea.appendText("\n\nAssistant: " + content);
                                            isFirstLine[0] = false;
                                        } else {
                                            responseArea.appendText(content);
                                        }
                                        responseArea.positionCaret(responseArea.getText().length());
                                    });
                                }
                            }
                        } else {
                            String content = jsonObject.getJSONObject("message").getString("content");
                            responseBuilder.append(content);
                            Platform.runLater(() -> {
                                if (isFirstLine[0]) {
                                    responseArea.appendText("\n\nAssistant: " + content);
                                    isFirstLine[0] = false;
                                } else {
                                    responseArea.appendText(content);
                                }
                                responseArea.positionCaret(responseArea.getText().length());
                            });
                        }
                    });

                    String response = responseBuilder.toString().trim();
                    Platform.runLater(() -> {
                        logArea.appendText("\n\n[" + timestamp + " - Assistant]\n" + response);
                        sendButton.setDisable(false);
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
                        responseArea.appendText("\n\nAssistant: " + errorMessage);
                        logArea.appendText("\n\n[" + timestamp + " - Error]\n" + e.getMessage());
                        responseArea.positionCaret(responseArea.getText().length());
                        logArea.positionCaret(logArea.getText().length());
                        sendButton.setDisable(false);
                        progressBar.setProgress(0);
                    });
                    return null;
                });
        } catch (Exception e) {
            String errorMessage = "Ollama is unavailable.";
            logArea.appendText("\n\n[" + timestamp + " - Error]\n" + e.getMessage());
            responseArea.appendText("\n\nAssistant: " + errorMessage);
            responseArea.positionCaret(responseArea.getText().length());
            logArea.positionCaret(logArea.getText().length());
            sendButton.setDisable(false);
            progressBar.setProgress(0);
        }
    }

    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;
            List<File> files = db.getFiles();
            for (File file : files) {
                String fileName = file.getName();
                String timestamp = LocalDateTime.now().format(formatter);

                // Display the file drop message in the response area
                responseArea.appendText("\n\nYou dropped: " + fileName);
                responseArea.appendText("\n\nAssistant: Thank you for dropping " + fileName);

                // Add timestamped file drop message to the log area
                logArea.appendText("\n\n[" + timestamp + " - You]\nDropped file: " + fileName);
                logArea.appendText("\n\n[" + timestamp + " - Assistant]\nThank you for dropping " + fileName);
            }
        }
        event.setDropCompleted(success);
        event.consume();
    }
}
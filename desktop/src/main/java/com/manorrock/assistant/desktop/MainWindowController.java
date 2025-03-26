package com.manorrock.assistant.desktop;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Clipboard;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.manorrock.assistant.impl.AssistantImpl;

public class MainWindowController {

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

    private final AssistantImpl assistant;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    public MainWindowController() {
        this.assistant = new AssistantImpl();
    }

    @FXML
    public void initialize() {
        responseArea.setText("Welcome to Manorrock Assistant");
        progressBar.setProgress(0);
        
        if (!assistant.isCliAvailable()) {
            responseArea.setText("Manorrock Assistant CLI not found. Please visit " +
                "https://github.com/manorrock/assistant?tab=readme-ov-file#quick-install " +
                "for installation instructions.");
            sendButton.setDisable(true);
            return;
        }
        
        showHelp();

        requestArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                handleSendAction();
                event.consume();
            }
        });
    }

    @FXML
    private void handleSendAction() {
        String userMessage = requestArea.getText().trim();

        if (!userMessage.isEmpty()) {
            if (userMessage.startsWith("/")) {
                handleCommand(userMessage);
                return;
            }

            responseArea.appendText("\n\nYou: " + userMessage);
            requestArea.clear();
            processMessage(userMessage);
        }
    }

    private void handleCommand(String command) {
        if (command.equals("/new")) {
            startNewSession();
            return; // startNewSession will handle CLI dispatch
        } else if (command.startsWith("/explain")) {
            handleExplain(command);
            return; // handleExplain will handle CLI processing
        }
        
        responseArea.appendText("\n\nYou: " + command);
        processMessage(command);
        requestArea.clear();
    }

    private void processMessage(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        sendButton.setDisable(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        assistant.executeCommand(message)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    responseArea.appendText("\n\n[" + timestamp + " - Assistant]\n" + response);
                    responseArea.positionCaret(responseArea.getText().length());
                    sendButton.setDisable(false);
                    progressBar.setProgress(0);
                });
            })
            .exceptionally(error -> {
                Platform.runLater(() -> {
                    String errorMessage = "Error: " + error.getMessage();
                    responseArea.appendText("\n\n[" + timestamp + " - Error]\n" + errorMessage);
                    sendButton.setDisable(false);
                    progressBar.setProgress(0);
                });
                return null;
            });
    }

    private void showHelp() {
        responseArea.appendText("\n\nSystem: Available commands:\n" +
            "/clear - Clear the response window\n" +
            "/explain - Explain the selected text\n" +
            "/help - Show this help message\n" +
            "/llm* - Use /help llm for LLM-specific commands\n" +
            "/new - Start a new chat session");
    }

    private void clearResponseArea() {
        responseArea.clear();
    }

    private void startNewSession() {
        responseArea.clear();
        responseArea.setText("Welcome to Manorrock Assistant");
        showHelp();
        
        // Dispatch /new to CLI to reset its state
        processMessage("/new");
    }

    @FXML
    private void handleStartOverAction() {
        startNewSession();
    }

    private void explainSelection() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String clipboardContent = clipboard.getString();

        if (clipboardContent != null && !clipboardContent.isEmpty()) {
            String prompt = "Please explain the content below the line\n-----------------------------------------\n"
                + clipboardContent;
            responseArea.appendText("\n\nYou: " + prompt);
            processMessage(prompt);
        } else {
            responseArea.appendText("\n\nSystem: No text found in clipboard. Copy some text and try again.");
        }
    }

    private void handleExplain(String command) {
        // Check if it's a bare /explain command or has arguments
        String[] parts = command.trim().split("\\s+", 2);
        boolean hasFilePath = parts.length > 1 && !parts[1].trim().isEmpty();
        
        if (hasFilePath) {
            // If path is provided, pass directly to CLI
            responseArea.appendText("\n\nYou: " + command);
            processMessage(command);
            return;
        }
        
        // Try to get text from clipboard
        explainSelection();
    }
}
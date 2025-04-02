package com.manorrock.assistant.desktop;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Clipboard;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker.State;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.text.StringEscapeUtils;

import com.manorrock.assistant.core.CoreAssistant;
import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.core.CoreAssistantMessage;

public class MainWindowController {

    @FXML
    private WebView responseArea;

    @FXML
    private TextArea requestArea;

    @FXML
    private Button sendButton;

    @FXML
    private Button startOverButton;

    @FXML
    private ProgressBar progressBar;
    
    @FXML
    private ToggleButton themeToggle;

    @FXML
    private BorderPane root;

    private boolean isDarkMode = false;

    /**
     * Stores the assistant.
     */
    private final CoreAssistant assistant;

    /**
     * Stores the message history.
     */
    private final List<String> messageHistory;

    /**
     * HTML template.
     */
    private static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    :root {
                        --bg-color: #ffffff;
                        --text-color: #000000;
                        --separator-color: #ccc;
                        --user-header-color: #333;
                        --assistant-header-color: rgb(0, 0, 145);
                    }
                    
                    [data-theme='dark'] {
                        --bg-color: #1e1e1e;
                        --text-color: #ffffff;
                        --separator-color: #555;
                        --user-header-color: #ccc;
                        --assistant-header-color: rgb(100, 150, 255);
                    }
                    
                    body { 
                        font-family: system-ui; 
                        margin: 1em;
                        background-color: var(--bg-color);
                        color: var(--text-color);
                    }
                    .message { 
                        margin-bottom: 2em; 
                    }
                    .message-header {
                        font-weight: bold;
                        margin-bottom: 0.5em;
                    }
                    .message-separator {
                        border-bottom: 1px solid var(--separator-color);
                        margin-bottom: 0.5em;
                    }
                    .message pre { 
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        margin: 0;
                        font-family: monospace;
                        color: var(--text-color);
                    }
                    .user .message-header { color: var(--user-header-color); }
                    .assistant .message-header { color: var(--assistant-header-color); }
                </style>
            </head>
            <body data-theme="%s">%s</body>
            </html>
            """;

    /**
     * Constructor.
     */
    public MainWindowController() {
        this.assistant = new CoreAssistant();
        this.messageHistory = new ArrayList<>();
    }

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        progressBar.setProgress(0);
        requestArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                handleSendAction();
                event.consume();
            }
        });
        
        // Initialize WebView
        responseArea.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == State.SUCCEEDED) {
                responseArea.getEngine().executeScript(
                    "window.scrollTo(0, document.body.scrollHeight);"
                );
            }
        });
        
        // Set initial content
        updateWebViewContent("<div class='message assistant'>Welcome to Manorrock Assistant</div>");

        // Initialize theme toggle
        themeToggle.setSelected(isDarkMode);
        
        // Wait for scene to be available before applying theme
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applyTheme();
            }
        });

        // Show initial help message
        Platform.runLater(() -> {
            handleMessage("/help");
        });
    }

    private void applyTheme() {
        if (root != null && root.getScene() != null) {
            Platform.runLater(() -> {
                Scene scene = root.getScene();
                scene.getStylesheets().clear();
                
                String cssFile = isDarkMode ? "MainWindow_dark.css" : "MainWindow_light.css";
                String cssUrl = getClass().getResource(cssFile).toExternalForm();
                scene.getStylesheets().add(cssUrl);
            });
        }
    }

    private void renderContent() {
        Platform.runLater(() -> {
            responseArea.getEngine().loadContent(
                String.format(HTML_TEMPLATE, 
                    isDarkMode ? "dark" : "light",
                    String.join("\n", messageHistory))
            );
        });
    }

    private void updateWebViewContent(String content) {
        Platform.runLater(() -> {
            responseArea.getEngine().loadContent(
                String.format(HTML_TEMPLATE,
                    isDarkMode ? "dark" : "light",
                    content)
            );
        });
    }

    private void appendMessage(String content, String type) {
        String escapedContent = StringEscapeUtils.escapeHtml4(content);
        
        if (type.equals("user")) {
            // User messages appear immediately
            String messageHtml = String.format(
                "<div class='message %s'><div class='message-header'>%s</div><div class='message-separator'></div><pre>%s</pre></div>",
                type, "You", escapedContent);
            messageHistory.add(messageHtml);
            renderContent();
        } else {
            // For assistant messages, animate character by character
            StringBuilder currentContent = new StringBuilder();
            
            // Add initial empty message
            String initialHtml = String.format(
                "<div class='message %s'><div class='message-header'>%s</div><div class='message-separator'></div><pre></pre></div>",
                type, "Assistant");
            messageHistory.add(initialHtml);
            renderContent();
            
            Timeline timeline = new Timeline();
            timeline.setCycleCount(1);
            
            // Add a keyframe for each character
            char[] chars = escapedContent.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                final int index = i;
                KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(i * 15), // 15ms delay between characters for typewriter effect
                    event -> {
                        currentContent.append(chars[index]);
                        
                        // Update the last message's content
                        String updatedHtml = String.format(
                            "<div class='message %s'><div class='message-header'>%s</div><div class='message-separator'></div><pre>%s</pre></div>",
                            type, "Assistant", currentContent.toString());
                        messageHistory.set(messageHistory.size() - 1, updatedHtml);
                        renderContent();
                    }
                );
                timeline.getKeyFrames().add(keyFrame);
            }
            
            // Once animation is complete, enable controls
            timeline.setOnFinished(event -> {
                progressBar.setProgress(0);
                sendButton.setDisable(false);
                requestArea.requestFocus();
            });
            
            Platform.runLater(() -> timeline.play());
        }
    }

    /**
     * Handle a command message.
     * 
     * @param command the command message
     */
    private void handleCommand(String command) {
        appendMessage(command, "user");
        
        if (command.equals("/clear")) {
            handleClear();
            return;
        } else if (command.equals("/session new")) {
            handleSessionNew();
            return;
        } else if (command.startsWith("/explain")) {
            handleExplain(command);
            return;
        }
        
        handleMessage(command);
    }

    private void handleClear() {
        responseArea.getEngine().loadContent(String.format(HTML_TEMPLATE, ""));
        requestArea.clear();
    }

    private void handleMessage(String message) {
        AssistantMessage assistantMessage = new CoreAssistantMessage(message);
        sendButton.setDisable(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        assistant.sendMessage(assistantMessage)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    appendMessage(response.getContent(), "assistant");
                });
            })
            .exceptionally(error -> {
                Platform.runLater(() -> {
                    String errorMessage = error.getMessage();
                    if (errorMessage != null && errorMessage.contains("Unable to determine which LLM to use")) {
                        errorMessage = "No language model (LLM) is configured. Please configure an LLM using the /llm commands. Type /help llm for more information.";
                    }
                    appendMessage(errorMessage, "system");
                    sendButton.setDisable(false);
                    progressBar.setProgress(0);
                });
                return null;
            });        

        requestArea.clear();
    }

    /**
     * Handle the /explain command.
     * 
     * @param command the command.
     */
    private void handleExplain(String command) {
        String[] parts = command.trim().split("\\s+", 2);
        boolean hasFilePath = parts.length > 1 && !parts[1].trim().isEmpty();
        
        if (hasFilePath) {
            handleExplainFileContent(parts);
        }
        handleExplainClipboard();
    }

    /**
     * Handle the /explain command with clipboard content.
     */
    private void handleExplainClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String clipboardContent = clipboard.getString();

        if (clipboardContent != null && !clipboardContent.isEmpty()) {
            String prompt = "Please explain the content below the line\n-----------------------------------------\n"
                + clipboardContent;
            appendMessage(prompt, "user");
            handleMessage(prompt);
        } else {
            appendMessage("Assistant: No text found in clipboard. Copy some text and try again.\n", "assistant");
        }
    }

    /**
     * Handle the /explain command with file content.
     * 
     * @param parts the command parts.
     */
    private void handleExplainFileContent(String[] parts) {
        String filePath = parts[1].trim();
        Path file = Paths.get(filePath);
        String fileContent;
        try {
            fileContent = Files.readString(file);
            String prompt = "Please explain the content below the line\n-----------------------------------------\n"
            + fileContent;
            appendMessage(prompt, "user");
            handleMessage(prompt);
            return;
        } catch (IOException e) {
            appendMessage("Assistant: Error reading file: " + e.getMessage() + "\n", "assistant");
            return;
        }
    }

    /**
     * Handles the send action.
     */
    @FXML
    private void handleSendAction() {
        String userMessage = requestArea.getText().trim();
        if (!userMessage.isEmpty()) {
            if (userMessage.startsWith("/")) {
                handleCommand(userMessage);
                requestArea.clear();
                requestArea.requestFocus();
                return;
            }

            appendMessage(userMessage, "user");
            handleMessage(userMessage);
            requestArea.clear();
            requestArea.requestFocus();
        }
    }

    /**
     * Handle the /session new command.
     */
    private void handleSessionNew() {
        responseArea.getEngine().loadContent(String.format(HTML_TEMPLATE, ""));
        handleMessage("/session new");
    }

    /**
     * Handle the start over action.
     */
    @FXML
    private void handleStartOverAction() {
        handleSessionNew();
    }

    /**
     * Handle the theme toggle action.
     */
    @FXML
    private void handleThemeToggle() {
        isDarkMode = themeToggle.isSelected();
        applyTheme();
        renderContent();
    }
}

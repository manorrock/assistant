package com.manorrock.assistant.desktop;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
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

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;

import java.util.Arrays;

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
    private ProgressBar progressBar;
    
    @FXML
    private BorderPane root;

    /**
     * Stores the assistant.
     */
    private final DesktopAssistant assistant;

    /**
     * Stores the message history.
     */
    private final List<String> messageHistory;
    
    /**
     * Markdown parser.
     */
    private final Parser markdownParser;
    
    /**
     * HTML renderer for markdown.
     */
    private final HtmlRenderer htmlRenderer;

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
                        --separator-color: #000000;
                        --user-header-color: #000000;
                        --assistant-header-color: rgb(0, 0, 0);
                        --code-bg-color: #f5f5f5;
                        --blockquote-border-color: #c8c8c8;
                        --link-color: #0366d6;
                        --table-border-color: #ddd;
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
                    .message-content { 
                        white-space: pre-wrap; 
                    }
                    /* Markdown styling */
                    .markdown pre, .markdown code {
                        background-color: var(--code-bg-color);
                        border-radius: 3px;
                        padding: 0.2em 0.4em;
                        font-family: monospace;
                    }
                    .markdown pre {
                        padding: 1em;
                        overflow: auto;
                    }
                    .markdown pre code {
                        background-color: transparent;
                        padding: 0;
                    }
                    .markdown blockquote {
                        border-left: 4px solid var(--blockquote-border-color);
                        padding-left: 1em;
                        margin-left: 0;
                        color: #666;
                    }
                    .markdown a {
                        color: var(--link-color);
                        text-decoration: none;
                    }
                    .markdown a:hover {
                        text-decoration: underline;
                    }
                    .markdown table {
                        border-collapse: collapse;
                        margin: 1em 0;
                    }
                    .markdown table th, .markdown table td {
                        border: 1px solid var(--table-border-color);
                        padding: 6px 13px;
                    }
                    .markdown table th {
                        background-color: var(--code-bg-color);
                    }
                    .markdown ul, .markdown ol {
                        padding-left: 2em;
                    }
                    .markdown h1, .markdown h2, .markdown h3, 
                    .markdown h4, .markdown h5, .markdown h6 {
                        margin-top: 1em;
                        margin-bottom: 0.5em;
                    }
                    .markdown h1 { font-size: 1.6em; }
                    .markdown h2 { font-size: 1.4em; }
                    .markdown h3 { font-size: 1.2em; }
                    .markdown h4 { font-size: 1.1em; }
                    .markdown p { margin: 0.5em 0; }
                    .user .message-header { color: var(--user-header-color); }
                    .assistant .message-header { color: var(--assistant-header-color); }
                </style>
            </head>
            <body>%s</body>
            </html>
            """;

    /**
     * Current theme (light or dark).
     */
    private String currentTheme = "light";

    /**
     * Constructor.
     */
    public MainWindowController() {
        this.assistant = new DesktopAssistant();
        this.messageHistory = new ArrayList<>();
        
        // Set up bidirectional relationship between controller and assistant
        this.assistant.setController(this);
        
        // Initialize markdown parser with GitHub-like extensions
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            AutolinkExtension.create(),
            AnchorLinkExtension.create(),
            TaskListExtension.create()
        ));
        
        // GitHub-like settings
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        options.set(HtmlRenderer.RENDER_HEADER_ID, true);
        
        this.markdownParser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
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
        
        // Wait for scene to be available
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

    /**
     * Apply the current theme to the application.
     */
    private void applyTheme() {
        if (root != null && root.getScene() != null) {
            Platform.runLater(() -> {
                Scene scene = root.getScene();
                scene.getStylesheets().clear();
                
                String cssFile = "MainWindow_" + currentTheme + ".css";
                String cssUrl = getClass().getResource(cssFile).toExternalForm();
                scene.getStylesheets().add(cssUrl);
                
                // Update WebView with appropriate theme CSS variables
                updateWebViewTheme();
            });
        }
    }
    
    /**
     * Update the WebView theme variables.
     */
    private void updateWebViewTheme() {
        if (responseArea != null && responseArea.getEngine() != null) {
            String cssVars = "";
            
            if ("dark".equals(currentTheme)) {
                cssVars = """
                    document.documentElement.style.setProperty('--bg-color', '#2a2a2a');
                    document.documentElement.style.setProperty('--text-color', '#e0e0e0');
                    document.documentElement.style.setProperty('--separator-color', '#5a5a5a');
                    document.documentElement.style.setProperty('--user-header-color', '#81c995');
                    document.documentElement.style.setProperty('--assistant-header-color', '#7aa2f7');
                    document.documentElement.style.setProperty('--code-bg-color', '#383838');
                    document.documentElement.style.setProperty('--blockquote-border-color', '#555');
                    document.documentElement.style.setProperty('--link-color', '#58a6ff');
                    document.documentElement.style.setProperty('--table-border-color', '#444');
                """;
            } else {
                cssVars = """
                    document.documentElement.style.setProperty('--bg-color', '#ffffff');
                    document.documentElement.style.setProperty('--text-color', '#000000');
                    document.documentElement.style.setProperty('--separator-color', '#000000');
                    document.documentElement.style.setProperty('--user-header-color', '#000000');
                    document.documentElement.style.setProperty('--assistant-header-color', '#000000');
                    document.documentElement.style.setProperty('--code-bg-color', '#f5f5f5');
                    document.documentElement.style.setProperty('--blockquote-border-color', '#c8c8c8');
                    document.documentElement.style.setProperty('--link-color', '#0366d6');
                    document.documentElement.style.setProperty('--table-border-color', '#ddd');
                """;
            }
            
            responseArea.getEngine().executeScript(cssVars);
        }
    }

    private void renderContent() {
        Platform.runLater(() -> {
            responseArea.getEngine().loadContent(
                String.format(HTML_TEMPLATE, 
                    String.join("\n", messageHistory))
            );
            // Apply theme variables after content is loaded
            responseArea.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == State.SUCCEEDED) {
                    updateWebViewTheme();
                    responseArea.getEngine().executeScript(
                        "window.scrollTo(0, document.body.scrollHeight);"
                    );
                }
            });
        });
    }

    private void updateWebViewContent(String content) {
        Platform.runLater(() -> {
            responseArea.getEngine().loadContent(
                String.format(HTML_TEMPLATE, content)
            );
            // Apply theme variables after content is loaded
            responseArea.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == State.SUCCEEDED) {
                    updateWebViewTheme();
                    responseArea.getEngine().executeScript(
                        "window.scrollTo(0, document.body.scrollHeight);"
                    );
                }
            });
        });
    }

    private void appendMessage(String content, String type) {
        if (type.equals("user")) {
            // For user messages, keep as plain text with HTML escaping
            String escapedContent = StringEscapeUtils.escapeHtml4(content);
            String messageHtml = String.format(
                "<div class='message %s'><div class='message-header'>%s</div><div class='message-separator'></div><pre>%s</pre></div>",
                type, "You", escapedContent);
            messageHistory.add(messageHtml);
            renderContent();
        } else {
            // For assistant messages, render markdown
            String formattedContent = renderMarkdown(content);
            String messageHtml = String.format(
                "<div class='message %s'><div class='message-header'>%s</div><div class='message-separator'></div><div class='message-content markdown'>%s</div></div>",
                type, "Assistant", formattedContent);
            messageHistory.add(messageHtml);
            renderContent();
            
            Platform.runLater(() -> {
                progressBar.setProgress(0);
                sendButton.setDisable(false);
                requestArea.requestFocus();
            });
        }
    }
    
    /**
     * Renders markdown content to HTML.
     *
     * @param markdown The markdown content to render
     * @return The rendered HTML
     */
    private String renderMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        
        // Parse and render markdown to HTML
        Document document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
    }

    /**
     * Handle a command message.
     * 
     * @param command the command message
     */
    private void handleCommand(String command) {
        appendMessage(command, "user");
        
        // Handle UI-specific commands directly
        if (command.startsWith("/explain")) {
            handleExplain(command);
            return;
        } else if (command.startsWith("/clear")) {
            handleClear();
            return;
        } else if (command.startsWith("/session")) {
            handleSession(command);
            return;
        }
        
        // Delegate all other commands to CoreAssistant
        handleMessage(command);
    }

    private void handleMessage(String message) {
        AssistantMessage assistantMessage = new CoreAssistantMessage(message);
        sendButton.setDisable(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        assistant.sendMessage(assistantMessage)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    appendMessage(response.getContent(), "assistant");
                    sendButton.setDisable(false);
                    progressBar.setProgress(0);
                });
            })
            .exceptionally(error -> {
                Platform.runLater(() -> {
                    appendMessage(error.getMessage(), "assistant");
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
     * Handle the /session command.
     * 
     * @param command the session command
     */
    private void handleSession(String command) {
        String[] parts = command.trim().split("\\s+", 2);
        String subCommand = parts.length > 1 ? parts[1].trim() : "";
        
        // Only handle UI-specific aspects of session commands
        if (subCommand.equals("new")) {
            responseArea.getEngine().loadContent(String.format(HTML_TEMPLATE, ""));
        }
        
        handleMessage(command);
    }

    /**
     * Handle the send action.
     * This is called when the user presses Enter or clicks the send button.
     */
    @FXML
    private void handleSendAction() {
        String message = requestArea.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        if (message.startsWith("/")) {
            handleCommand(message);
        } else {
            handleMessage(message);
        }
    }

    /**
     * Handle the /clear command.
     */
    protected void handleClear() {
        messageHistory.clear();
        responseArea.getEngine().loadContent(String.format(HTML_TEMPLATE, ""));
        requestArea.clear();
    }

    /**
     * Set the application theme.
     * 
     * @param theme the theme to set ("light" or "dark")
     */
    public void setTheme(String theme) {
        if ("light".equals(theme) || "dark".equals(theme)) {
            this.currentTheme = theme;
            applyTheme();
        }
    }
    
    /**
     * Toggle between light and dark themes.
     * 
     * @return true if the new theme is dark, false if it's light
     */
    public boolean toggleTheme() {
        currentTheme = "light".equals(currentTheme) ? "dark" : "light";
        applyTheme();
        return "dark".equals(currentTheme);
    }
}

package com.example;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import org.json.JSONObject;
import org.json.JSONArray;

@TopComponent.Description(
        preferredID = "NetBeansControllerTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "com.example.NetBeansControllerTopComponent")
@ActionRegistration(displayName = "#CTL_NetBeansControllerAction")
@ActionReferences({
    @ActionReference(path = "Menu/Window", position = 0)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_NetBeansControllerAction",
        preferredID = "NetBeansControllerTopComponent"
)
@Messages({
    "CTL_NetBeansControllerAction=Manorrock Assistant",
    "CTL_NetBeansControllerTopComponent=Manorrock Assistant Window",
    "HINT_NetBeansControllerTopComponent=This is a Manorrock Assistant window"
})
public final class NetBeansControllerTopComponent extends TopComponent implements ActionListener {

    private JTextArea responseArea;
    private JTextArea requestArea;
    private JTextArea logArea;
    private JButton sendButton;
    private JButton startOverButton;
    private JProgressBar progressBar;
    private String sessionId = UUID.randomUUID().toString();
    private LinkedList<JSONObject> history = new LinkedList<>();
    private String ollamaEndpoint = "http://localhost:11434/api/chat";
    private String model = "llama3";
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    public NetBeansControllerTopComponent() {
        initComponents();
        setName(Bundle.CTL_NetBeansControllerTopComponent());
        setToolTipText(Bundle.HINT_NetBeansControllerTopComponent());
    }

    private void initComponents() {
        responseArea = new JTextArea();
        requestArea = new JTextArea();
        logArea = new JTextArea();
        sendButton = new JButton("Send");
        startOverButton = new JButton("Start Over");
        progressBar = new JProgressBar(0, 100);

        // Set initial message
        responseArea.setText("Welcome to Manorrock Assistant");
        logArea.setText("Chat Log\n---------");

        // Show help message on startup
        showHelp();

        // Setup key event handler for the requestArea
        requestArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER && !event.isShiftDown()) {
                    handleSendAction();
                    event.consume(); // Prevents the newline from being added
                }
            }
        });

        sendButton.addActionListener(this);
        startOverButton.addActionListener(this);

        // Layout setup (simplified)
        setLayout(new BorderLayout());
        add(new JScrollPane(responseArea), BorderLayout.CENTER);
        add(new JScrollPane(logArea), BorderLayout.EAST);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JScrollPane(requestArea), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendButton);
        buttonPanel.add(startOverButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton) {
            handleSendAction();
        } else if (e.getSource() == startOverButton) {
            handleStartOverAction();
        }
    }

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
            responseArea.append("\n\nYou: " + userMessage);

            // Add timestamped message to the log area
            logArea.append("\n\n[" + timestamp + " - You]\n" + userMessage);

            // Clear the request area
            requestArea.setText("");

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
            responseArea.append("\n\nSystem: Unknown command. Type /help for a list of commands.");
        }
        requestArea.setText("");
    }

    private void changeEndpoint(String command) {
        Pattern pattern = Pattern.compile("/endpoint\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String newEndpoint = matcher.group(1);
            ollamaEndpoint = "http://" + newEndpoint + "/api/chat";
            responseArea.append("\n\nSystem: Endpoint changed to " + ollamaEndpoint);
            logArea.append("\n\n[" + LocalDateTime.now().format(formatter) + " - System]\nEndpoint changed to " + ollamaEndpoint);
        } else {
            responseArea.append("\n\nSystem: Invalid endpoint format. Use /endpoint myhostname:myport");
        }
    }

    private void changeModel(String command) {
        Pattern pattern = Pattern.compile("/model\\s+(\\S+)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            model = matcher.group(1);
            responseArea.append("\n\nSystem: Model changed to " + model);
            logArea.append("\n\n[" + LocalDateTime.now().format(formatter) + " - System]\nModel changed to " + model);
        } else {
            responseArea.append("\n\nSystem: Invalid model format. Use /model <name>");
        }
    }

    private void showHelp() {
        String helpMessage = "\n\nSystem: Available commands:\n" +
                             "/endpoint myhostname:myport - Change the Ollama endpoint\n" +
                             "/model <name> - Change the model used\n" +
                             "/help - Show this help message\n" +
                             "/clear - Clear the response window";
        responseArea.append(helpMessage);
    }

    private void clearResponseArea() {
        responseArea.setText("");
    }

    private void handleStartOverAction() {
        // Clear the history and reset the session ID
        history.clear();
        sessionId = UUID.randomUUID().toString();

        // Clear the response and log areas
        responseArea.setText("");
        logArea.setText("");

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

            sendButton.setEnabled(false);
            progressBar.setIndeterminate(true);

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
                                    javax.swing.SwingUtilities.invokeLater(() -> {
                                        if (isFirstLine[0]) {
                                            responseArea.append("\n\nAssistant: " + content);
                                            isFirstLine[0] = false;
                                        } else {
                                            responseArea.append(content);
                                        }
                                        responseArea.setCaretPosition(responseArea.getDocument().getLength());
                                    });
                                }
                            }
                        } else {
                            String content = jsonObject.getJSONObject("message").getString("content");
                            responseBuilder.append(content);
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                if (isFirstLine[0]) {
                                    responseArea.append("\n\nAssistant: " + content);
                                    isFirstLine[0] = false;
                                } else {
                                    responseArea.append(content);
                                }
                                responseArea.setCaretPosition(responseArea.getDocument().getLength());
                            });
                        }
                    });

                    String response = responseBuilder.toString().trim();
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        logArea.append("\n\n[" + timestamp + " - Assistant]\n" + response);
                        sendButton.setEnabled(true);
                        progressBar.setIndeterminate(false);

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
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        String errorMessage = "Ollama is unavailable.";
                        responseArea.append("\n\nAssistant: " + errorMessage);
                        logArea.append("\n\n[" + timestamp + " - Error]\n" + e.getMessage());
                        responseArea.setCaretPosition(responseArea.getDocument().getLength());
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                        sendButton.setEnabled(true);
                        progressBar.setIndeterminate(false);
                    });
                    return null;
                });
        } catch (Exception e) {
            String errorMessage = "Ollama is unavailable.";
            logArea.append("\n\n[" + timestamp + " - Error]\n" + e.getMessage());
            responseArea.append("\n\nAssistant: " + errorMessage);
            responseArea.setCaretPosition(responseArea.getDocument().getLength());
            logArea.setCaretPosition(logArea.getDocument().getLength());
            sendButton.setEnabled(true);
            progressBar.setIndeterminate(false);
        }
    }
}

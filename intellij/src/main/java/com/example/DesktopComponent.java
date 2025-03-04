package com.example;

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class DesktopComponent implements ToolWindowFactory {

    private JTextArea responseArea;
    private JTextArea requestArea;
    private JTextArea logArea;
    private JButton sendButton;
    private JButton startOverButton;
    private JProgressBar progressBar;
    private String sessionId = UUID.randomUUID().toString();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        responseArea = new JTextArea("Welcome to Manorrock Assistant");
        requestArea = new JTextArea();
        logArea = new JTextArea("Chat Log\n---------");
        sendButton = new JButton("Send");
        startOverButton = new JButton("Start Over");
        progressBar = new JProgressBar();

        sendButton.addActionListener(e -> handleSendAction());
        startOverButton.addActionListener(e -> handleStartOverAction());

        panel.add(new JScrollPane(responseArea), BorderLayout.CENTER);
        panel.add(new JScrollPane(requestArea), BorderLayout.SOUTH);
        panel.add(new JScrollPane(logArea), BorderLayout.EAST);
        panel.add(sendButton, BorderLayout.WEST);
        panel.add(startOverButton, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.SOUTH);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private void handleSendAction() {
        String userMessage = requestArea.getText().trim();
        if (!userMessage.isEmpty()) {
            responseArea.append("\n\nYou: " + userMessage);
            logArea.append("\n\nYou: " + userMessage);
            requestArea.setText("");
            processMessage(userMessage);
        }
    }

    private void handleStartOverAction() {
        sessionId = UUID.randomUUID().toString();
        responseArea.setText("Welcome to Manorrock Assistant");
        logArea.setText("Chat Log\n---------");
    }

    private void processMessage(String message) {
        // Implement message processing logic here
        responseArea.append("\n\nAssistant: This is a placeholder response.");
        logArea.append("\n\nAssistant: This is a placeholder response.");
    }
}

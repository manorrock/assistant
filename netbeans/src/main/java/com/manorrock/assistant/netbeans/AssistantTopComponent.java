package com.manorrock.assistant.netbeans;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.TopComponent;

import com.manorrock.assistant.api.AssistantMessage;
import com.manorrock.assistant.core.CoreAssistant;
import com.manorrock.assistant.core.CoreAssistantMessage;

@TopComponent.Description(preferredID = "AssistantTopComponent", persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "com.manorrock.assistant.netbeans.AssistantTopComponent")
@ActionRegistration(displayName = "#CTL_AssistantAction")
@ActionReferences({@ActionReference(path = "Menu/Window", position = 0)})
@TopComponent.OpenActionRegistration(displayName = "#CTL_AssistantAction", preferredID = "AssistantTopComponent")
@Messages({"CTL_AssistantAction=Manorrock Assistant",
    "CTL_AssistantTopComponent=Manorrock Assistant Window",
    "HINT_AssistantTopComponent=This is a Manorrock Assistant window"})
public final class AssistantTopComponent extends TopComponent implements ActionListener, FocusListener {

    private final CoreAssistant assistant;
    private JTextPane responseArea;
    private JTextArea requestArea;
    private JButton sendButton;
    private JProgressBar progressBar;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    private InputOutput io;
    private TopComponent lastFocusedEditor;
    private Timer typewriterTimer;
    private int currentCharIndex;
    private String currentTypingText;
    private boolean isDarkMode = false;

    // Text style attributes
    private SimpleAttributeSet userMessageStyle;
    private SimpleAttributeSet assistantMessageStyle;
    private SimpleAttributeSet systemMessageStyle;
    private SimpleAttributeSet headerStyle;

    public AssistantTopComponent() {
        assistant = new CoreAssistant();
        initComponents();
        setName(Bundle.CTL_AssistantTopComponent());
        setToolTipText(Bundle.HINT_AssistantTopComponent());
        io = IOProvider.getDefault().getIO("Chat Log", false);
        
        appendSystemMessage("Welcome to Manorrock Assistant\n\nType /help for a list of commands.");
        
        TopComponent.getRegistry().addPropertyChangeListener(evt -> {
            if (TopComponent.Registry.PROP_ACTIVATED.equals(evt.getPropertyName())) {
                TopComponent activated = TopComponent.getRegistry().getActivated();
                if (activated != null && activated.getLookup().lookup(EditorCookie.class) != null) {
                    lastFocusedEditor = activated;
                }
            }
        });
        
        // Show initial help message
        handleCommand("/help");
    }

    private void initComponents() {
        responseArea = new JTextPane();
        responseArea.setEditable(false);
        requestArea = new JTextArea(3, 50);
        sendButton = new JButton("Send");
        progressBar = new JProgressBar(0, 100);
        
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

        // Layout setup
        setLayout(new BorderLayout());
        add(new JScrollPane(responseArea), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JScrollPane(requestArea), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton) {
            handleSendAction();
        }
    }

    // Make this method package-private so the test can access it directly
    void handleSendAction() {
        String userMessage = requestArea.getText().trim();

        if (!userMessage.isEmpty()) {
            // Clear the request area
            requestArea.setText("");

            // Handle commands (starting with /)
            if (userMessage.startsWith("/")) {
                handleCommand(userMessage);
                return;
            }

            // Display user message
            appendUserMessage(userMessage);
            
            // Send message to assistant - use a slight delay to ensure user message is displayed first
            SwingUtilities.invokeLater(() -> {
                processMessage(userMessage);
            });
        }
    }
    
    private void handleCommand(String command) {
        appendUserMessage(command);
        
        // Handle special commands
        if (command.equals("/new") || command.equals("/session new")) {
            responseArea.setText("");
            appendSystemMessage("Started a new chat session.");
            // Reset assistant state
            assistant.reset();
            return;
        } else if (command.equals("/clear")) {
            responseArea.setText("");
            return;
        } else if (command.startsWith("/explain")) {
            handleExplain(command);
            return;
        }
        
        // Process other commands through CoreAssistant
        processMessage(command);
    }
    
    /**
     * Handles explain command with appropriate text source (selection, file, or CLI).
     * 
     * @param command The explain command with potential arguments
     */
    private void handleExplain(String command) {
        // Check if it's a bare /explain command or has arguments
        String[] parts = command.trim().split("\\s+", 2);
        boolean hasFilePath = parts.length > 1 && !parts[1].trim().isEmpty();
        
        if (hasFilePath) {
            // If path is provided, pass directly to assistant
            processMessage(command);
            return;
        }
        
        EditorCookie editorCookie = getLastFocusedEditorCookie();
        if (editorCookie == null || editorCookie.getOpenedPanes() == null || editorCookie.getOpenedPanes().length == 0) {
            // No editor available, pass through to assistant
            processMessage(command);
            return;
        }
        
        try {
            String selectedText = editorCookie.getOpenedPanes()[0].getSelectedText();
            if (selectedText == null || selectedText.isEmpty()) {
                selectedText = editorCookie.getDocument().getText(0, editorCookie.getDocument().getLength());
            }
            
            if (selectedText.trim().length() == 0) {
                // No content available, pass through to assistant
                processMessage(command);
                return;
            }
            
            DataObject dataObj = lastFocusedEditor.getLookup().lookup(DataObject.class);
            String fileName = dataObj.getPrimaryFile().getNameExt();
            String fileInfo = selectedText.equals(editorCookie.getDocument().getText(0, editorCookie.getDocument().getLength())) ?
                "entire file: " + fileName :
                "selection from " + fileName;
            
            // Format the message with the required prefix and separator
            String messageToSend = "Explain the following in an easy to understand way\n\n--------\n\n" + selectedText;
            
            // Let user know what's being explained
            appendSystemMessage("Explaining " + fileInfo + "...");
            
            // Process through assistant
            processMessage(messageToSend);
        } catch (javax.swing.text.BadLocationException e) {
            // On error, pass through to assistant
            processMessage(command);
        }
    }

    private void processMessage(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        
        sendButton.setEnabled(false);
        requestArea.setEnabled(false); // Disable request area during processing
        progressBar.setIndeterminate(true);
        
        AssistantMessage assistantMessage = new CoreAssistantMessage(message);
        assistant.sendMessage(assistantMessage)
            .thenAccept(response -> {
                SwingUtilities.invokeLater(() -> {
                    appendAssistantMessage(response.getContent());
                    io.getOut().println("[" + timestamp + " - Assistant]\n" + response.getContent());
                    // Send button will be enabled when typewriter effect completes
                    progressBar.setIndeterminate(false);
                });
            })
            .exceptionally(e -> {
                SwingUtilities.invokeLater(() -> {
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("Unable to determine which LLM to use")) {
                        errorMessage = "No language model (LLM) is configured. Please configure an LLM using the /llm commands. Type /help llm for more information.";
                    }
                    appendSystemMessage("Error: " + errorMessage);
                    io.getOut().println("[" + timestamp + " - Error]\n" + errorMessage);
                    sendButton.setEnabled(true);
                    requestArea.setEnabled(true); // Re-enable request area on error
                    progressBar.setIndeterminate(false);
                });
                return null;
            });
    }
    
    private void appendUserMessage(String message) {
        // Make sure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendUserMessage(message));
            return;
        }
        
        StyledDocument doc = responseArea.getStyledDocument();
        try {
            // Add separator
            doc.insertString(doc.getLength(), "\n\n", null);
            
            // Add header
            doc.insertString(doc.getLength(), "You: ", headerStyle);
            
            // Add message
            doc.insertString(doc.getLength(), message, userMessageStyle);
            
            // Log to Output window
            String timestamp = LocalDateTime.now().format(formatter);
            io.getOut().println("[" + timestamp + " - You]\n" + message);
            
            // Scroll to bottom
            responseArea.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private void appendAssistantMessage(String message) {
        // Make sure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendAssistantMessage(message));
            return;
        }
        
        StyledDocument doc = responseArea.getStyledDocument();
        try {
            // Add separator
            doc.insertString(doc.getLength(), "\n\n", null);
            
            // Add header
            doc.insertString(doc.getLength(), "Assistant: ", headerStyle);
            
            // In test environment, skip the typewriter effect for consistent behavior
            if (Boolean.getBoolean("netbeans.running.environment")) {
                // Simply insert the message directly when running in test
                doc.insertString(doc.getLength(), message, assistantMessageStyle);
                sendButton.setEnabled(true);
                requestArea.setEnabled(true);
                return;
            }
            
            // Set up typewriter effect for normal operation
            currentTypingText = message;
            currentCharIndex = 0;
            
            if (typewriterTimer != null) {
                typewriterTimer.stop();
            }
            
            typewriterTimer = new Timer(15, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (currentCharIndex < currentTypingText.length()) {
                            char c = currentTypingText.charAt(currentCharIndex);
                            doc.insertString(doc.getLength(), String.valueOf(c), assistantMessageStyle);
                            currentCharIndex++;
                            responseArea.setCaretPosition(doc.getLength());
                        } else {
                            typewriterTimer.stop();
                            sendButton.setEnabled(true); // Re-enable send button when typing is complete
                            requestArea.setEnabled(true); // Re-enable request area when typing is complete
                        }
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                        typewriterTimer.stop();
                    }
                }
            });
            
            typewriterTimer.start();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private void appendSystemMessage(String message) {
        // Make sure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendSystemMessage(message));
            return;
        }
        
        StyledDocument doc = responseArea.getStyledDocument();
        try {
            if (doc.getLength() > 0) {
                doc.insertString(doc.getLength(), "\n\n", null);
            }
            doc.insertString(doc.getLength(), "System: ", headerStyle);
            doc.insertString(doc.getLength(), message, systemMessageStyle);
            responseArea.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private EditorCookie getLastFocusedEditorCookie() {
        if (lastFocusedEditor != null) {
            DataObject dataObject = lastFocusedEditor.getLookup().lookup(DataObject.class);
            if (dataObject != null) {
                return dataObject.getLookup().lookup(EditorCookie.class);
            }
        }
        return null;
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (e.getComponent() instanceof TopComponent) {
            TopComponent tc = (TopComponent) e.getComponent();
            if (tc.getLookup().lookup(EditorCookie.class) != null) {
                lastFocusedEditor = tc;
            }
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        // No action needed
    }
}

package com.manorrock.assistant.netbeans;

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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;

import com.manorrock.assistant.impl.AssistantImpl;

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

    private final AssistantImpl assistant;
    private JTextArea responseArea;
    private JTextArea requestArea;
    private JButton sendButton;
    private JProgressBar progressBar;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    private InputOutput io;
    private TopComponent lastFocusedEditor;

    public AssistantTopComponent() {
        assistant = new AssistantImpl();
        initComponents();
        setName(Bundle.CTL_AssistantTopComponent());
        setToolTipText(Bundle.HINT_AssistantTopComponent());
        io = IOProvider.getDefault().getIO("Chat Log", false);
        
        if (!assistant.isCliAvailable()) {
            responseArea.setText("Manorrock Assistant CLI not found. Please visit " +
                "https://github.com/manorrock/assistant?tab=readme-ov-file#quick-install " +
                "for installation instructions.");
            sendButton.setEnabled(false);
        } else {
            responseArea.setText("Welcome to Manorrock Assistant\n\nType /help for a list of commands.");
        }
        
        TopComponent.getRegistry().addPropertyChangeListener(evt -> {
            if (TopComponent.Registry.PROP_ACTIVATED.equals(evt.getPropertyName())) {
                TopComponent activated = TopComponent.getRegistry().getActivated();
                if (activated != null && activated.getLookup().lookup(EditorCookie.class) != null) {
                    lastFocusedEditor = activated;
                }
            }
        });
    }

    private void initComponents() {
        responseArea = new JTextArea();
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        requestArea = new JTextArea();
        sendButton = new JButton("Send");
        progressBar = new JProgressBar(0, 100);

        // Set initial message
        responseArea.setText("Welcome to Manorrock Assistant");

        // Show help message on startup
        responseArea.append("\n\nType /help for a list of commands.");

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

        // Layout setup (simplified)
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

    private void handleSendAction() {
        String userMessage = requestArea.getText().trim();

        if (!userMessage.isEmpty()) {
            String timestamp = LocalDateTime.now().format(formatter);

            // Display the user's message in the response area
            responseArea.append("\n\nYou: " + userMessage);

            // Add timestamped message to the Output Window
            io.getOut().println("[" + timestamp + " - You]\n" + userMessage);

            // Clear the request area
            requestArea.setText("");

            // Handle /new command specially to clear UI first
            if (userMessage.equals("/new")) {
                // Clear the UI
                responseArea.setText("");
                responseArea.append("Started a new chat session.\n");
                // Continue to send /new to CLI to reset its state
            }

            // Handle /explain command specially 
            if (userMessage.startsWith("/explain")) {
                handleExplain(userMessage);
                return; // handleExplain will handle CLI processing
            }

            // Process message through CLI
            processMessage(userMessage);
        }
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
            // If path is provided, pass directly to CLI
            processMessage(command);
            return;
        }
        
        EditorCookie editorCookie = getLastFocusedEditorCookie();
        if (editorCookie == null || editorCookie.getOpenedPanes() == null || editorCookie.getOpenedPanes().length == 0) {
            // No editor available, pass through to CLI
            processMessage(command);
            return;
        }
        
        try {
            String selectedText = editorCookie.getOpenedPanes()[0].getSelectedText();
            if (selectedText == null || selectedText.isEmpty()) {
                selectedText = editorCookie.getDocument().getText(0, editorCookie.getDocument().getLength());
            }
            
            if (selectedText.trim().length() == 0) {
                // No content available, pass through to CLI
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
            responseArea.append("\n\nExplaining " + fileInfo + "...\n");
            
            // Process through CLI
            processMessage(messageToSend);
        } catch (javax.swing.text.BadLocationException e) {
            // On error, pass through to CLI
            processMessage(command);
        }
    }

    private void processMessage(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        
        sendButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        
        assistant.executeCommand(message)
            .thenAccept(response -> {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    responseArea.append("\n\nAssistant: " + response);
                    responseArea.setCaretPosition(responseArea.getDocument().getLength());
                    io.getOut().println("[" + timestamp + " - Assistant]\n" + response);
                    sendButton.setEnabled(true);
                    progressBar.setIndeterminate(false);
                });
            })
            .exceptionally(e -> {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    String errorMessage = "Error: " + e.getMessage();
                    responseArea.append("\n\nSystem: " + errorMessage);
                    io.getOut().println("[" + timestamp + " - Error]\n" + errorMessage);
                    sendButton.setEnabled(true);
                    progressBar.setIndeterminate(false);
                });
                return null;
            });
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
